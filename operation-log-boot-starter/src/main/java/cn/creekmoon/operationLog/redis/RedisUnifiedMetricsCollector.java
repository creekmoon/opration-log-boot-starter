package cn.creekmoon.operationLog.redis;

import cn.creekmoon.operationLog.core.UnifiedMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Redis 版统一指标收集器
 * 
 * 特点：
 * - 多副本数据共享
 * - 数据持久化
 * - 支持跨副本用户行为追踪
 * - 自动降级处理
 * 
 * @author CodeSmith
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RedisUnifiedMetricsCollector implements UnifiedMetricsCollector {

    private final StringRedisTemplate redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final RedisFailoverManager failoverManager;
    
    // 本地缓存（用于降级时）
    private final MemoryFallbackStore fallbackStore = new MemoryFallbackStore();
    
    // 批量写入缓冲区
    private final List<MetricRecord> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 100;
    private static final long FLUSH_INTERVAL_MS = 5000;
    private volatile long lastFlushTime = System.currentTimeMillis();

    /**
     * 指标记录
     */
    public record MetricRecord(
            String endpoint,
            long responseTime,
            boolean success,
            long timestamp
    ) {}

    @Override
    public void requestStarted() {
        if (isFallbackMode()) {
            fallbackStore.requestStarted();
            return;
        }
        // Redis 模式：不追踪全局并发（各副本独立）
        // 如需全局并发，可通过 Redis 计数器实现
        fallbackStore.requestStarted(); // 本地仍追踪用于当前实例监控
    }

    @Override
    public void requestEnded() {
        fallbackStore.requestEnded();
    }

    @Override
    @Async("logThreadPool")
    public void record(String endpoint, long responseTimeMillis) {
        if (isFallbackMode()) {
            fallbackStore.record(endpoint, responseTimeMillis);
            return;
        }
        
        try {
            MetricRecord record = new MetricRecord(
                    endpoint, responseTimeMillis, true, System.currentTimeMillis()
            );
            
            synchronized (buffer) {
                buffer.add(record);
                long now = System.currentTimeMillis();
                if (buffer.size() >= BATCH_SIZE || (now - lastFlushTime) >= FLUSH_INTERVAL_MS) {
                    flushBuffer();
                    lastFlushTime = now;
                }
            }
        } catch (Exception e) {
            log.warn("记录Redis指标失败: {}", e.getMessage());
            fallbackStore.record(endpoint, responseTimeMillis);
        }
    }

    @Override
    @Async("logThreadPool")
    public void recordError(String endpoint) {
        if (isFallbackMode()) {
            fallbackStore.recordError(endpoint);
            return;
        }
        
        try {
            String statKey = RedisKeyConstants.statKey(endpoint);
            String errorRankKey = RedisKeyConstants.errorRankKey();
            
            redisTemplate.executePipelined((connection) -> {
                connection.hashCommands().hIncrBy(statKey.getBytes(), "errorCount".getBytes(), 1);
                connection.hashCommands().hIncrBy(statKey.getBytes(), "totalCount".getBytes(), 1);
                return null;
            });
            
            redisTemplate.opsForZSet().incrementScore(errorRankKey, endpoint, 1);
            redisTemplate.expire(errorRankKey, Duration.ofSeconds(RedisKeyConstants.TTL_ERROR_RANK));
            redisTemplate.expire(statKey, Duration.ofSeconds(RedisKeyConstants.TTL_STAT));
            
        } catch (Exception e) {
            log.warn("记录Redis错误指标失败: {}", e.getMessage());
            fallbackStore.recordError(endpoint);
        }
    }

    @Override
    public long getCurrentConcurrentRequests() {
        return fallbackStore.getCurrentConcurrentRequests();
    }

    @Override
    public long getPeakConcurrentRequests() {
        return fallbackStore.getPeakConcurrentRequests();
    }

    @Override
    public long getTotalRequests() {
        if (isFallbackMode()) {
            return fallbackStore.getTotalRequests();
        }
        
        try {
            // 从 Redis 获取全局总请求数
            String pattern = RedisKeyConstants.KEY_PREFIX + "stat:" + 
                    LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            
            long total = 0;
            for (String key : keys) {
                Object count = redisTemplate.opsForHash().get(key, "totalCount");
                if (count != null) {
                    total += Long.parseLong(count.toString());
                }
            }
            return total;
            
        } catch (Exception e) {
            log.warn("获取Redis全局请求数失败: {}", e.getMessage());
            return fallbackStore.getTotalRequests();
        }
    }

    @Override
    public double getCurrentQps() {
        // Redis 模式：计算全局 QPS
        long totalRequests = getTotalRequests();
        // 简化估算：假设数据是最近1分钟内积累的
        return totalRequests / 60.0;
    }

    @Override
    public double getAvgQps() {
        // 与当前 QPS 相同（Redis 数据是全局聚合的）
        return getCurrentQps();
    }

    @Override
    public double getGlobalErrorRate() {
        if (isFallbackMode()) {
            return fallbackStore.getGlobalErrorRate();
        }
        
        try {
            String pattern = RedisKeyConstants.KEY_PREFIX + "stat:" + 
                    LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return 0.0;
            }
            
            long totalCount = 0;
            long errorCount = 0;
            
            for (String key : keys) {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                totalCount += parseLong(entries.get("totalCount"));
                errorCount += parseLong(entries.get("errorCount"));
            }
            
            return totalCount > 0 ? (double) errorCount / totalCount : 0.0;
            
        } catch (Exception e) {
            log.warn("获取Redis全局错误率失败: {}", e.getMessage());
            return fallbackStore.getGlobalErrorRate();
        }
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getAllEndpointMetrics() {
        if (isFallbackMode()) {
            return fallbackStore.getAllEndpointMetrics();
        }
        
        Map<String, EndpointMetricsSnapshot> result = new HashMap<>();
        
        try {
            String pattern = RedisKeyConstants.KEY_PREFIX + "stat:" + 
                    LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null) {
                return result;
            }
            
            for (String key : keys) {
                String endpoint = key.substring(key.lastIndexOf(':') + 1).replace("_", ".");
                EndpointMetricsSnapshot snapshot = fetchEndpointMetricsFromRedis(endpoint);
                if (snapshot != null) {
                    result.put(endpoint, snapshot);
                }
            }
            
        } catch (Exception e) {
            log.warn("获取Redis全部指标失败: {}", e.getMessage());
        }
        
        return result;
    }

    @Override
    public EndpointMetricsSnapshot getEndpointMetrics(String endpoint) {
        if (isFallbackMode()) {
            return fallbackStore.getEndpointMetrics(endpoint);
        }
        
        EndpointMetricsSnapshot snapshot = fetchEndpointMetricsFromRedis(endpoint);
        return snapshot != null ? snapshot : EndpointMetricsSnapshot.empty(endpoint);
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getSlowestEndpoints(int limit) {
        Map<String, EndpointMetricsSnapshot> all = getAllEndpointMetrics();
        
        return all.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(
                        e2.getValue().avgResponseTime(), 
                        e1.getValue().avgResponseTime()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getErrorEndpoints(int limit) {
        Map<String, EndpointMetricsSnapshot> all = getAllEndpointMetrics();
        
        return all.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(
                        e2.getValue().errorRate(), 
                        e1.getValue().errorRate()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private EndpointMetricsSnapshot fetchEndpointMetricsFromRedis(String endpoint) {
        try {
            String statKey = RedisKeyConstants.statKey(endpoint);
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(statKey);
            
            if (entries.isEmpty()) {
                return null;
            }
            
            long totalCount = parseLong(entries.get("totalCount"));
            long errorCount = parseLong(entries.get("errorCount"));
            long totalResponseTime = parseLong(entries.get("totalResponseTime"));
            long maxResponseTime = parseLong(entries.get("maxResponseTime"));
            long minResponseTime = parseLong(entries.get("minResponseTime"));
            
            long avgResponseTime = totalCount > 0 ? totalResponseTime / totalCount : 0;
            double errorRate = totalCount > 0 ? (double) errorCount / totalCount : 0.0;
            
            return new EndpointMetricsSnapshot(
                    endpoint,
                    totalCount,
                    errorCount,
                    totalResponseTime,
                    avgResponseTime,
                    maxResponseTime,
                    minResponseTime == 0 ? 0 : minResponseTime,
                    errorRate
            );
            
        } catch (Exception e) {
            log.warn("获取接口指标失败: endpoint={}, error={}", endpoint, e.getMessage());
            return null;
        }
    }

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
            Map<String, EndpointStats> statsMap = new HashMap<>();
            
            for (MetricRecord record : toFlush) {
                EndpointStats stats = statsMap.computeIfAbsent(record.endpoint(), k -> new EndpointStats());
                stats.totalCount++;
                stats.totalResponseTime += record.responseTime();
                stats.maxResponseTime = Math.max(stats.maxResponseTime, record.responseTime());
                stats.minResponseTime = Math.min(stats.minResponseTime, record.responseTime());
            }
            
            redisTemplate.executePipelined((connection) -> {
                for (Map.Entry<String, EndpointStats> entry : statsMap.entrySet()) {
                    String endpoint = entry.getKey();
                    EndpointStats stats = entry.getValue();
                    String statKey = RedisKeyConstants.statKey(endpoint);
                    
                    connection.hashCommands().hIncrBy(statKey.getBytes(), "totalCount".getBytes(), stats.totalCount);
                    connection.hashCommands().hIncrBy(statKey.getBytes(), "totalResponseTime".getBytes(), stats.totalResponseTime);
                    
                    byte[] maxKey = "maxResponseTime".getBytes();
                    byte[] maxValue = connection.hashCommands().hGet(statKey.getBytes(), maxKey);
                    long currentMax = maxValue == null ? 0 : Long.parseLong(new String(maxValue));
                    if (stats.maxResponseTime > currentMax) {
                        connection.hashCommands().hSet(statKey.getBytes(), maxKey, String.valueOf(stats.maxResponseTime).getBytes());
                    }
                    
                    byte[] minKey = "minResponseTime".getBytes();
                    byte[] minValue = connection.hashCommands().hGet(statKey.getBytes(), minKey);
                    long currentMin = minValue == null ? Long.MAX_VALUE : Long.parseLong(new String(minValue));
                    if (stats.minResponseTime < currentMin) {
                        connection.hashCommands().hSet(statKey.getBytes(), minKey, String.valueOf(stats.minResponseTime).getBytes());
                    }
                }
                return null;
            });
            
            for (String endpoint : statsMap.keySet()) {
                String statKey = RedisKeyConstants.statKey(endpoint);
                redisTemplate.expire(statKey, Duration.ofSeconds(RedisKeyConstants.TTL_STAT));
            }
            
        } catch (Exception e) {
            log.warn("批量刷新Redis指标失败: {}", e.getMessage());
            // 回退到本地存储
            toFlush.forEach(r -> fallbackStore.record(r.endpoint(), r.responseTime()));
        }
    }

    private boolean isFallbackMode() {
        return failoverManager != null && failoverManager.getFallbackMode().get();
    }

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
     * 降级时使用的内存存储
     */
    private static class MemoryFallbackStore {
        private final ConcurrentHashMap<String, FallbackMetrics> metricsMap = new ConcurrentHashMap<>();
        private final AtomicLong concurrentRequests = new AtomicLong(0);
        private final AtomicLong peakConcurrentRequests = new AtomicLong(0);
        private final LongAdder totalRequests = new LongAdder();

        void requestStarted() {
            long current = concurrentRequests.incrementAndGet();
            peakConcurrentRequests.updateAndGet(peak -> Math.max(peak, current));
            totalRequests.increment();
        }

        void requestEnded() {
            concurrentRequests.decrementAndGet();
        }

        void record(String endpoint, long responseTime) {
            FallbackMetrics metrics = metricsMap.computeIfAbsent(endpoint, k -> new FallbackMetrics());
            metrics.record(responseTime);
            totalRequests.increment();
        }

        void recordError(String endpoint) {
            FallbackMetrics metrics = metricsMap.computeIfAbsent(endpoint, k -> new FallbackMetrics());
            metrics.recordError();
        }

        long getCurrentConcurrentRequests() {
            return concurrentRequests.get();
        }

        long getPeakConcurrentRequests() {
            return peakConcurrentRequests.get();
        }

        long getTotalRequests() {
            return totalRequests.sum();
        }

        double getGlobalErrorRate() {
            long totalErrors = metricsMap.values().stream()
                    .mapToLong(m -> m.errorCount)
                    .sum();
            long total = totalRequests.sum();
            return total > 0 ? (double) totalErrors / total : 0;
        }

        Map<String, EndpointMetricsSnapshot> getAllEndpointMetrics() {
            Map<String, EndpointMetricsSnapshot> result = new HashMap<>();
            metricsMap.forEach((key, m) -> {
                result.put(key, new EndpointMetricsSnapshot(
                        key, m.totalCount, m.errorCount, m.totalResponseTime,
                        m.getAvgResponseTime(), m.maxResponseTime, m.minResponseTime, m.getErrorRate()
                ));
            });
            return result;
        }

        EndpointMetricsSnapshot getEndpointMetrics(String endpoint) {
            FallbackMetrics m = metricsMap.get(endpoint);
            if (m == null) {
                return EndpointMetricsSnapshot.empty(endpoint);
            }
            return new EndpointMetricsSnapshot(
                    endpoint, m.totalCount, m.errorCount, m.totalResponseTime,
                    m.getAvgResponseTime(), m.maxResponseTime, m.minResponseTime, m.getErrorRate()
            );
        }
    }

    private static class FallbackMetrics {
        long totalCount = 0;
        long errorCount = 0;
        long totalResponseTime = 0;
        long maxResponseTime = 0;
        long minResponseTime = Long.MAX_VALUE;

        synchronized void record(long responseTime) {
            totalCount++;
            totalResponseTime += responseTime;
            maxResponseTime = Math.max(maxResponseTime, responseTime);
            minResponseTime = Math.min(minResponseTime, responseTime);
        }

        synchronized void recordError() {
            errorCount++;
        }

        synchronized long getAvgResponseTime() {
            return totalCount > 0 ? totalResponseTime / totalCount : 0;
        }

        synchronized double getErrorRate() {
            return totalCount > 0 ? (double) errorCount / totalCount : 0;
        }
    }

    private static class EndpointStats {
        long totalCount = 0;
        long totalResponseTime = 0;
        long maxResponseTime = 0;
        long minResponseTime = Long.MAX_VALUE;
    }
}
