package cn.creekmoon.operationLog.redis;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based 全局自适应采样器
 * 
 * 核心功能：
 * 1. 基于全局QPS动态调整采样率
 * 2. 全局慢查询检测（Redis共享P99）
 * 3. 定时上报本地P99到Redis（聚合多副本）
 * 4. 多副本间采样策略一致性
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBasedSampler {

    private final StringRedisTemplate redisTemplate;
    
    // 本地缓存配置
    private static final long CACHE_TTL_MS = 1000; // 1秒缓存
    private static final long P99_CACHE_TTL_MS = 10000; // 10秒P99缓存
    
    // 采样决策缓存
    private final Map<String, SampleCache> sampleCache = new ConcurrentHashMap<>();
    
    // P99阈值缓存
    private final Map<String, P99Cache> p99Cache = new ConcurrentHashMap<>();
    
    // 本地P99计算（滑动窗口）
    private final Map<String, LocalP99Calculator> localP99Calculators = new ConcurrentHashMap<>();
    
    // 采样率配置
    private volatile double baseSampleRate = 1.0;
    private volatile boolean globalSlowMode = false;
    
    // 慢查询阈值（毫秒）
    private static final long SLOW_QUERY_THRESHOLD = 1000;
    
    // QPS阈值配置
    private static final long QPS_LOW = 1000;
    private static final long QPS_MEDIUM = 10000;
    private static final long QPS_HIGH = 100000;
    
    // 采样率调整因子
    private static final double RATE_FULL = 1.0;
    private static final double RATE_HALF = 0.5;
    private static final double RATE_TENTH = 0.1;
    private static final double RATE_HUNDREDTH = 0.01;

    @PostConstruct
    public void init() {
        log.info("RedisBasedSampler 已初始化，全局自适应采样已启用");
    }

    /**
     * 判断是否应该采样
     * 
     * 决策逻辑：
     * 1. 如果是慢查询接口 → 100%采样
     * 2. 根据全局QPS计算采样率
     * 3. 本地随机决策
     */
    public boolean shouldSample(String endpoint) {
        long now = System.currentTimeMillis();
        
        // 检查缓存
        SampleCache cache = sampleCache.get(endpoint);
        if (cache != null && (now - cache.timestamp) < CACHE_TTL_MS) {
            return cache.shouldSample;
        }
        
        // 计算采样决策
        boolean shouldSample = calculateSampleDecision(endpoint);
        
        // 更新缓存
        sampleCache.put(endpoint, new SampleCache(shouldSample, now));
        
        return shouldSample;
    }

    /**
     * 计算采样决策
     */
    private boolean calculateSampleDecision(String endpoint) {
        try {
            // 1. 检查是否是全局慢查询接口
            if (isGlobalSlowEndpoint(endpoint)) {
                return true; // 慢查询100%采样
            }
            
            // 2. 获取全局QPS
            long globalQps = getGlobalQps(endpoint);
            
            // 3. 计算采样率
            double sampleRate = calculateSampleRate(globalQps);
            
            // 4. 本地随机决策
            return ThreadLocalRandom.current().nextDouble() < sampleRate;
            
        } catch (Exception e) {
            log.debug("采样决策计算失败，使用默认采样率: {}", e.getMessage());
            // 降级：使用基础采样率
            return ThreadLocalRandom.current().nextDouble() < baseSampleRate;
        }
    }

    /**
     * 基于全局QPS计算采样率
     */
    private double calculateSampleRate(long globalQps) {
        if (globalQps < QPS_LOW) {
            return RATE_FULL; // 100%
        } else if (globalQps < QPS_MEDIUM) {
            return RATE_HALF; // 50%
        } else if (globalQps < QPS_HIGH) {
            return RATE_TENTH; // 10%
        } else {
            return RATE_HUNDREDTH; // 1%
        }
    }

    /**
     * 判断是否是全局慢查询接口
     * 基于Redis共享的P99阈值
     */
    public boolean isGlobalSlowEndpoint(String endpoint) {
        long now = System.currentTimeMillis();
        
        // 检查缓存
        P99Cache cache = p99Cache.get(endpoint);
        if (cache != null && (now - cache.timestamp) < P99_CACHE_TTL_MS) {
            return cache.p99 > SLOW_QUERY_THRESHOLD;
        }
        
        try {
            // 从Redis获取全局P99
            String p99Key = RedisKeyConstants.p99GlobalKey();
            Double p99Score = redisTemplate.opsForZSet().score(p99Key, endpoint);
            
            long p99 = p99Score != null ? p99Score.longValue() : 0;
            
            // 更新缓存
            p99Cache.put(endpoint, new P99Cache(p99, now));
            
            return p99 > SLOW_QUERY_THRESHOLD;
            
        } catch (Exception e) {
            log.debug("获取全局P99失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取全局QPS
     */
    private long getGlobalQps(String endpoint) {
        try {
            // 使用QPS Key（分钟级）
            String qpsKey = RedisKeyConstants.qpsKey(endpoint);
            String value = redisTemplate.opsForValue().get(qpsKey);
            
            if (value != null) {
                return Long.parseLong(value);
            }
            
            // 备选：从统计Hash计算QPS
            String statKey = RedisKeyConstants.statKey(endpoint);
            Object totalCount = redisTemplate.opsForHash().get(statKey, "totalCount");
            
            if (totalCount != null) {
                // 估算：假设数据是最近1分钟内积累的
                return Long.parseLong(totalCount.toString()) / 60;
            }
            
            return 0;
            
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 记录本地响应时间（用于上报P99）
     */
    public void recordLocalResponseTime(String endpoint, long responseTime) {
        LocalP99Calculator calculator = localP99Calculators.computeIfAbsent(
                endpoint, k -> new LocalP99Calculator());
        calculator.add(responseTime);
    }

    /**
     * 定时上报本地P99到Redis（每分钟）
     */
    @Scheduled(fixedRate = 60000)
    public void reportLocalP99() {
        try {
            String p99Key = RedisKeyConstants.p99GlobalKey();
            
            for (Map.Entry<String, LocalP99Calculator> entry : localP99Calculators.entrySet()) {
                String endpoint = entry.getKey();
                LocalP99Calculator calculator = entry.getValue();
                
                long p99 = calculator.getP99();
                if (p99 > 0) {
                    // 上报到Redis Sorted Set
                    redisTemplate.opsForZSet().add(p99Key, endpoint, p99);
                }
                
                // 重置本地计算器
                calculator.reset();
            }
            
            // 设置TTL
            redisTemplate.expire(p99Key, Duration.ofSeconds(RedisKeyConstants.TTL_P99));
            
            log.debug("本地P99已上报到Redis，接口数: {}", localP99Calculators.size());
            
        } catch (Exception e) {
            log.warn("上报本地P99失败: {}", e.getMessage());
        }
    }

    /**
     * 获取当前采样率（供监控使用）
     */
    public double getCurrentSampleRate(String endpoint) {
        try {
            long globalQps = getGlobalQps(endpoint);
            return calculateSampleRate(globalQps);
        } catch (Exception e) {
            return baseSampleRate;
        }
    }

    /**
     * 获取全局慢查询接口列表
     */
    public Set<String> getGlobalSlowEndpoints() {
        try {
            String p99Key = RedisKeyConstants.p99GlobalKey();
            
            // 获取P99 > 阈值的接口
            Set<String> slowEndpoints = redisTemplate.opsForZSet().rangeByScore(
                    p99Key, SLOW_QUERY_THRESHOLD, Double.MAX_VALUE);
            
            if (slowEndpoints == null) {
                return Set.of();
            }
            
            return slowEndpoints;
                    
        } catch (Exception e) {
            log.warn("获取全局慢查询接口失败: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * 更新QPS计数（每请求调用）
     */
    public void incrementQps(String endpoint) {
        try {
            String qpsKey = RedisKeyConstants.qpsKey(endpoint);
            redisTemplate.opsForValue().increment(qpsKey);
            redisTemplate.expire(qpsKey, Duration.ofSeconds(RedisKeyConstants.TTL_QPS));
        } catch (Exception e) {
            // 静默失败，不影响业务
        }
    }

    // ==================== 内部类 ====================

    /**
     * 采样决策缓存
     */
    private record SampleCache(boolean shouldSample, long timestamp) {}

    /**
     * P99缓存
     */
    private record P99Cache(long p99, long timestamp) {}

    /**
     * 本地P99计算器（滑动窗口）
     */
    private static class LocalP99Calculator {
        private static final int WINDOW_SIZE = 1000;
        private final long[] window = new long[WINDOW_SIZE];
        private int index = 0;
        private int count = 0;

        synchronized void add(long value) {
            window[index] = value;
            index = (index + 1) % WINDOW_SIZE;
            if (count < WINDOW_SIZE) {
                count++;
            }
        }

        synchronized long getP99() {
            if (count == 0) {
                return 0;
            }
            
            long[] sorted = new long[count];
            int start = count < WINDOW_SIZE ? 0 : index;
            for (int i = 0; i < count; i++) {
                sorted[i] = window[(start + i) % WINDOW_SIZE];
            }
            
            java.util.Arrays.sort(sorted);
            return sorted[(int) (count * 0.99)];
        }

        synchronized void reset() {
            count = 0;
            index = 0;
        }
    }
}
