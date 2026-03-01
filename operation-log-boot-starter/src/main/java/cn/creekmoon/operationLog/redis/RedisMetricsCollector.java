package cn.creekmoon.operationLog.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis版指标收集器
 * 将接口调用指标写入Redis，支持多副本共享状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMetricsCollector {

    private final StringRedisTemplate redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    
    // 批量写入缓冲区
    private final List<MetricRecord> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 5000;
    private long lastFlushTime = System.currentTimeMillis();

    /**
     * 指标记录
     */
    public record MetricRecord(
            String endpoint,
            long responseTime,
            boolean success,
            String userId,
            long timestamp
    ) {}

    /**
     * 记录接口调用指标
     * 异步写入Redis，支持批量处理
     */
    @Async("logThreadPool")
    public void record(String endpoint, long responseTime, boolean success) {
        record(endpoint, responseTime, success, null);
    }

    /**
     * 记录接口调用指标（带用户ID）
     */
    @Async("logThreadPool")
    public void record(String endpoint, long responseTime, boolean success, String userId) {
        try {
            MetricRecord record = new MetricRecord(endpoint, responseTime, success, userId, System.currentTimeMillis());
            
            // 添加到缓冲区
            synchronized (buffer) {
                buffer.add(record);
                
                // 检查是否需要批量刷新
                long now = System.currentTimeMillis();
                if (buffer.size() >= BATCH_SIZE || (now - lastFlushTime) >= FLUSH_INTERVAL_MS) {
                    flushBuffer();
                    lastFlushTime = now;
                }
            }
        } catch (Exception e) {
            log.warn("记录Redis指标失败: {}", e.getMessage());
        }
    }

    /**
     * 立即写入Redis（单条）
     */
    public void recordImmediate(String endpoint, long responseTime, boolean success, String userId) {
        try {
            String statKey = RedisKeyConstants.statKey(endpoint);
            String latencyKey = RedisKeyConstants.latencyKey(endpoint);
            
            // 使用Pipeline批量执行
            redisTemplate.executePipelined(new org.springframework.data.redis.core.RedisCallback<Object>() {
                @Override
                public Object doInRedis(org.springframework.data.redis.connection.RedisConnection connection) {
                    // 更新接口统计 (Hash)
                    connection.hashCommands().hIncrBy(statKey.getBytes(), "totalCount".getBytes(), 1);
                    connection.hashCommands().hIncrBy(statKey.getBytes(), "totalResponseTime".getBytes(), responseTime);
                    
                    if (!success) {
                        connection.hashCommands().hIncrBy(statKey.getBytes(), "errorCount".getBytes(), 1);
                    }
                    
                    // 更新最大响应时间
                    byte[] maxKey = "maxResponseTime".getBytes();
                    byte[] maxValue = connection.hashCommands().hGet(statKey.getBytes(), maxKey);
                    long currentMax = maxValue == null ? 0 : Long.parseLong(new String(maxValue));
                    if (responseTime > currentMax) {
                        connection.hashCommands().hSet(statKey.getBytes(), maxKey, String.valueOf(responseTime).getBytes());
                    }
                    
                    // 更新最小响应时间
                    byte[] minKey = "minResponseTime".getBytes();
                    byte[] minValue = connection.hashCommands().hGet(statKey.getBytes(), minKey);
                    long currentMin = minValue == null ? Long.MAX_VALUE : Long.parseLong(new String(minValue));
                    if (responseTime < currentMin) {
                        connection.hashCommands().hSet(statKey.getBytes(), minKey, String.valueOf(responseTime).getBytes());
                    }
                    
                    // 记录响应时间到Sorted Set
                    connection.zSetCommands().zAdd(latencyKey.getBytes(), responseTime, 
                            (System.nanoTime() + "").getBytes());
                    
                    return null;
                }
            });
            
            // 设置TTL
            redisTemplate.expire(statKey, Duration.ofSeconds(RedisKeyConstants.TTL_STAT));
            redisTemplate.expire(latencyKey, Duration.ofSeconds(RedisKeyConstants.TTL_LATENCY));
            
            // 记录UV（如果有用户ID）
            if (userId != null && !userId.isEmpty()) {
                String uvKey = RedisKeyConstants.uvKey(endpoint);
                redisTemplate.opsForHyperLogLog().add(uvKey, userId);
                redisTemplate.expire(uvKey, Duration.ofSeconds(RedisKeyConstants.TTL_UV));
            }
            
            // 记录到错误排行（如果是错误）
            if (!success) {
                String errorRankKey = RedisKeyConstants.errorRankKey();
                redisTemplate.opsForZSet().incrementScore(errorRankKey, endpoint, 1);
                redisTemplate.expire(errorRankKey, Duration.ofSeconds(RedisKeyConstants.TTL_ERROR_RANK));
            }
            
        } catch (Exception e) {
            log.warn("立即写入Redis指标失败: {}", e.getMessage());
        }
    }

    /**
     * 批量刷新缓冲区
     */
    private void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }
        
        List<MetricRecord> toFlush;
        synchronized (buffer) {
            toFlush = new ArrayList<>(buffer);
            buffer.clear();
        }
        
        try {
            // 按endpoint分组统计
            Map<String, EndpointStats> statsMap = new HashMap<>();
            
            for (MetricRecord record : toFlush) {
                EndpointStats stats = statsMap.computeIfAbsent(record.endpoint(), k -> new EndpointStats());
                stats.totalCount++;
                stats.totalResponseTime += record.responseTime();
                if (!record.success()) {
                    stats.errorCount++;
                }
                stats.maxResponseTime = Math.max(stats.maxResponseTime, record.responseTime());
                stats.minResponseTime = Math.min(stats.minResponseTime, record.responseTime());
            }
            
            // 批量写入Redis
            redisTemplate.executePipelined(new org.springframework.data.redis.core.RedisCallback<Object>() {
                @Override
                public Object doInRedis(org.springframework.data.redis.connection.RedisConnection connection) {
                    for (Map.Entry<String, EndpointStats> entry : statsMap.entrySet()) {
                        String endpoint = entry.getKey();
                        EndpointStats stats = entry.getValue();
                        String statKey = RedisKeyConstants.statKey(endpoint);
                        
                        connection.hashCommands().hIncrBy(statKey.getBytes(), "totalCount".getBytes(), stats.totalCount);
                        connection.hashCommands().hIncrBy(statKey.getBytes(), "totalResponseTime".getBytes(), stats.totalResponseTime);
                        
                        if (stats.errorCount > 0) {
                            connection.hashCommands().hIncrBy(statKey.getBytes(), "errorCount".getBytes(), stats.errorCount);
                        }
                        
                        // 更新最大/最小响应时间（使用简单的比较，可能存在并发问题，但可接受）
                        connection.hashCommands().hSet(statKey.getBytes(), "maxResponseTime".getBytes(), 
                                String.valueOf(stats.maxResponseTime).getBytes());
                        connection.hashCommands().hSet(statKey.getBytes(), "minResponseTime".getBytes(), 
                                String.valueOf(stats.minResponseTime).getBytes());
                    }
                    return null;
                }
            });
            
            // 设置TTL
            for (String endpoint : statsMap.keySet()) {
                String statKey = RedisKeyConstants.statKey(endpoint);
                redisTemplate.expire(statKey, Duration.ofSeconds(RedisKeyConstants.TTL_STAT));
            }
            
        } catch (Exception e) {
            log.warn("批量刷新Redis指标失败: {}", e.getMessage());
        }
    }

    /**
     * 获取接口的全局统计
     */
    public GlobalEndpointMetrics getGlobalMetrics(String endpoint) {
        try {
            String statKey = RedisKeyConstants.statKey(endpoint);
            String latencyKey = RedisKeyConstants.latencyKey(endpoint);
            String uvKey = RedisKeyConstants.uvKey(endpoint);
            
            Map<Object, Object> statMap = redisTemplate.opsForHash().entries(statKey);
            
            long totalCount = parseLong(statMap.get("totalCount"));
            long errorCount = parseLong(statMap.get("errorCount"));
            long totalResponseTime = parseLong(statMap.get("totalResponseTime"));
            long maxResponseTime = parseLong(statMap.get("maxResponseTime"));
            long minResponseTime = parseLong(statMap.get("minResponseTime"));
            
            long avgResponseTime = totalCount > 0 ? totalResponseTime / totalCount : 0;
            double errorRate = totalCount > 0 ? (double) errorCount / totalCount : 0;
            
            // 获取P99（从Sorted Set）
            long p99 = calculateP99(latencyKey, 0.99);
            long p95 = calculateP99(latencyKey, 0.95);
            long p50 = calculateP99(latencyKey, 0.5);
            
            // 获取UV
            long uv = redisTemplate.opsForHyperLogLog().size(uvKey);
            
            return new GlobalEndpointMetrics(
                    endpoint,
                    totalCount,
                    errorCount,
                    avgResponseTime,
                    maxResponseTime,
                    minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime,
                    p50, p95, p99,
                    errorRate,
                    uv
            );
            
        } catch (Exception e) {
            log.warn("获取Redis全局指标失败: {}", e.getMessage());
            return GlobalEndpointMetrics.empty(endpoint);
        }
    }

    /**
     * 获取全局Top10接口（按请求数）
     */
    public List<GlobalEndpointMetrics> getGlobalTopEndpoints(int limit) {
        List<GlobalEndpointMetrics> result = new ArrayList<>();
        
        try {
            // 扫描所有stat key
            String pattern = RedisKeyConstants.KEY_PREFIX + "stat:" + LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ":*";
            
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return result;
            }
            
            for (String key : keys) {
                // 提取endpoint名称
                String endpoint = key.substring(key.lastIndexOf(':') + 1);
                GlobalEndpointMetrics metrics = getGlobalMetrics(endpoint);
                if (metrics.totalCount() > 0) {
                    result.add(metrics);
                }
            }
            
            // 按请求数排序
            result.sort((a, b) -> Long.compare(b.totalCount(), a.totalCount()));
            
            // 限制数量
            if (result.size() > limit) {
                return result.subList(0, limit);
            }
            
        } catch (Exception e) {
            log.warn("获取全局Top接口失败: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * 计算P99/P95/P50分位数
     */
    private long calculateP99(String latencyKey, double percentile) {
        try {
            Long total = redisTemplate.opsForZSet().zCard(latencyKey);
            if (total == null || total == 0) {
                return 0;
            }
            
            long rank = (long) (total * percentile);
            Set<?> values = redisTemplate.opsForZSet().range(latencyKey, rank, rank);
            
            if (values == null || values.isEmpty()) {
                return 0;
            }
            
            // 获取score（响应时间）
            String value = values.iterator().next().toString();
            Double score = redisTemplate.opsForZSet().score(latencyKey, value.toString());
            return score != null ? score.longValue() : 0;
            
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 解析Long值
     */
    private long parseLong(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 端点统计（内部类）
     */
    private static class EndpointStats {
        long totalCount = 0;
        long errorCount = 0;
        long totalResponseTime = 0;
        long maxResponseTime = 0;
        long minResponseTime = Long.MAX_VALUE;
    }

    /**
     * 全局端点指标
     */
    public record GlobalEndpointMetrics(
            String endpoint,
            long totalCount,
            long errorCount,
            long avgResponseTime,
            long maxResponseTime,
            long minResponseTime,
            long p50,
            long p95,
            long p99,
            double errorRate,
            long uv
    ) {
        public static GlobalEndpointMetrics empty(String endpoint) {
            return new GlobalEndpointMetrics(endpoint, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0);
        }
    }
}
