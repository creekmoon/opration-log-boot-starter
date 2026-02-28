package cn.creekmoon.operationLog.heatmap;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 热力图服务实现类
 * 使用Redis HyperLogLog统计PV/UV
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatmapServiceImpl implements HeatmapService {

    private final StringRedisTemplate redisTemplate;
    private final HeatmapProperties properties;

    // 降级模式下的本地缓存
    private final Map<String, AtomicLong> fallbackPvCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> fallbackUvCache = new ConcurrentHashMap<>();
    private final AtomicBoolean fallbackActive = new AtomicBoolean(false);
    private final AtomicLong redisErrorCount = new AtomicLong(0);
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    @PostConstruct
    public void init() {
        log.info("[operation-log] HeatmapService initialized, enabled={}, sampleRate={}", 
                properties.isEnabled(), properties.getSampleRate());
    }

    @PreDestroy
    public void destroy() {
        log.info("[operation-log] HeatmapService destroyed");
    }

    @Override
    public void recordVisit(String className, String methodName, String userId, LocalDateTime timestamp) {
        if (!properties.isEnabled()) {
            return;
        }

        // 采样检查
        if (properties.getSampleRate() < 1.0 && Math.random() > properties.getSampleRate()) {
            return;
        }

        try {
            String baseKey = buildBaseKey(className, methodName);
            
            // 记录PV (String类型, incr)
            recordPv(baseKey, timestamp);
            
            // 记录UV (HyperLogLog类型, pfadd)
            if (userId != null && !userId.isEmpty()) {
                recordUv(baseKey, userId, timestamp);
            }
        } catch (Exception e) {
            handleRedisError("recordVisit", e);
            // 降级到本地缓存
            if (properties.isFallbackEnabled()) {
                recordVisitFallback(className, methodName, userId);
            }
        }
    }

    @Override
    public void recordVisit(String className, String methodName, String userId) {
        recordVisit(className, methodName, userId, LocalDateTime.now());
    }

    /**
     * 记录PV
     */
    private void recordPv(String baseKey, LocalDateTime timestamp) {
        // 实时PV
        String realtimeKey = properties.getRedisKeyPrefix() + ":pv:realtime:" + baseKey;
        redisTemplate.opsForValue().increment(realtimeKey);
        redisTemplate.expire(realtimeKey, 
                java.time.Duration.ofHours(properties.getRealtimeRetentionHours()));

        // 小时级PV
        String hourKey = properties.getRedisKeyPrefix() + ":pv:hourly:" + 
                timestamp.format(HOUR_FORMATTER) + ":" + baseKey;
        redisTemplate.opsForValue().increment(hourKey);
        redisTemplate.expire(hourKey, 
                java.time.Duration.ofDays(properties.getHourlyRetentionDays()));

        // 天级PV
        String dayKey = properties.getRedisKeyPrefix() + ":pv:daily:" + 
                timestamp.format(DATE_FORMATTER) + ":" + baseKey;
        redisTemplate.opsForValue().increment(dayKey);
        redisTemplate.expire(dayKey, 
                java.time.Duration.ofDays(properties.getDailyRetentionDays()));
    }

    /**
     * 记录UV
     */
    private void recordUv(String baseKey, String userId, LocalDateTime timestamp) {
        // 实时UV
        String realtimeKey = properties.getRedisKeyPrefix() + ":uv:realtime:" + baseKey;
        redisTemplate.opsForHyperLogLog().add(realtimeKey, userId);
        redisTemplate.expire(realtimeKey, 
                java.time.Duration.ofHours(properties.getRealtimeRetentionHours()));

        // 小时级UV
        String hourKey = properties.getRedisKeyPrefix() + ":uv:hourly:" + 
                timestamp.format(HOUR_FORMATTER) + ":" + baseKey;
        redisTemplate.opsForHyperLogLog().add(hourKey, userId);
        redisTemplate.expire(hourKey, 
                java.time.Duration.ofDays(properties.getHourlyRetentionDays()));

        // 天级UV
        String dayKey = properties.getRedisKeyPrefix() + ":uv:daily:" + 
                timestamp.format(DATE_FORMATTER) + ":" + baseKey;
        redisTemplate.opsForHyperLogLog().add(dayKey, userId);
        redisTemplate.expire(dayKey, 
                java.time.Duration.ofDays(properties.getDailyRetentionDays()));
    }

    /**
     * 降级模式下的访问记录
     */
    private void recordVisitFallback(String className, String methodName, String userId) {
        String key = className + "." + methodName;
        
        // 限制缓存大小
        if (fallbackPvCache.size() >= properties.getFallbackMaxSize()) {
            return;
        }

        fallbackPvCache.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        if (userId != null && !userId.isEmpty()) {
            fallbackUvCache.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }
    }

    @Override
    public HeatmapStats getRealtimeStats(String className, String methodName) {
        if (!properties.isEnabled()) {
            return new HeatmapStats(className, methodName, 0, 0, LocalDateTime.now());
        }

        String baseKey = buildBaseKey(className, methodName);
        String pvKey = properties.getRedisKeyPrefix() + ":pv:realtime:" + baseKey;
        String uvKey = properties.getRedisKeyPrefix() + ":uv:realtime:" + baseKey;

        try {
            String pvValue = redisTemplate.opsForValue().get(pvKey);
            Long uvValue = redisTemplate.opsForHyperLogLog().size(uvKey);

            long pv = pvValue != null ? Long.parseLong(pvValue) : 0;
            long uv = uvValue != null ? uvValue : 0;

            return new HeatmapStats(className, methodName, pv, uv, LocalDateTime.now());
        } catch (Exception e) {
            handleRedisError("getRealtimeStats", e);
            // 返回降级缓存数据
            return getFallbackStats(className, methodName);
        }
    }

    @Override
    public Map<String, HeatmapStats> getAllRealtimeStats() {
        if (!properties.isEnabled()) {
            return Collections.emptyMap();
        }

        Map<String, HeatmapStats> result = new HashMap<>();
        
        try {
            String pattern = properties.getRedisKeyPrefix() + ":pv:realtime:*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String pvKey : keys) {
                    try {
                        // 解析key获取className和methodName
                        String[] parts = pvKey.substring((properties.getRedisKeyPrefix() + ":pv:realtime:").length()).split(":");
                        if (parts.length >= 2) {
                            String className = parts[0];
                            String methodName = parts[1];
                            HeatmapStats stats = getRealtimeStats(className, methodName);
                            result.put(stats.fullName(), stats);
                        }
                    } catch (Exception ex) {
                        log.warn("[operation-log] Failed to parse key: {}", pvKey);
                    }
                }
            }
        } catch (Exception e) {
            handleRedisError("getAllRealtimeStats", e);
        }

        // 合并降级缓存数据
        if (fallbackActive.get()) {
            for (Map.Entry<String, AtomicLong> entry : fallbackPvCache.entrySet()) {
                String key = entry.getKey();
                long pv = entry.getValue().get();
                Set<String> uvSet = fallbackUvCache.getOrDefault(key, Collections.emptySet());
                
                String[] parts = key.split("\\.");
                if (parts.length >= 2) {
                    String className = parts[0];
                    String methodName = parts[1];
                    result.merge(key, 
                        new HeatmapStats(className, methodName, pv, uvSet.size(), LocalDateTime.now()),
                        (old, neu) -> new HeatmapStats(
                            old.className(), old.methodName(),
                            old.pv() + neu.pv(), old.uv() + neu.uv(),
                            LocalDateTime.now()
                        ));
                }
            }
        }

        return result;
    }

    /**
     * 获取降级缓存的统计数据
     */
    private HeatmapStats getFallbackStats(String className, String methodName) {
        String key = className + "." + methodName;
        long pv = fallbackPvCache.getOrDefault(key, new AtomicLong(0)).get();
        long uv = fallbackUvCache.getOrDefault(key, Collections.emptySet()).size();
        return new HeatmapStats(className, methodName, pv, uv, LocalDateTime.now());
    }

    @Override
    public List<HeatmapTopItem> getTopN(TimeWindow timeWindow, MetricType metricType, int topN) {
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        int limit = Math.min(topN, properties.getTopNMaxSize());
        String pattern = buildPatternForTimeWindow(timeWindow, metricType);
        
        Map<String, Long> allStats = new HashMap<>();

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    try {
                        Long value = getValueFromKey(key, metricType);
                        if (value != null && value > 0) {
                            String fullName = extractFullNameFromKey(key, timeWindow, metricType);
                            allStats.merge(fullName, value, Long::sum);
                        }
                    } catch (Exception ex) {
                        log.warn("[operation-log] Failed to get value for key: {}", key);
                    }
                }
            }
        } catch (Exception e) {
            handleRedisError("getTopN", e);
        }

        // 排序并取TopN
        return allStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\.", 2);
                    String className = parts.length > 0 ? parts[0] : "unknown";
                    String methodName = parts.length > 1 ? parts[1] : "unknown";
                    return new HeatmapTopItem(0, className, methodName, entry.getValue(), metricType);
                })
                .collect(Collectors.toList())
                .stream()
                .map(item -> new HeatmapTopItem(
                        allStats.entrySet().stream()
                                .filter(e -> e.getValue() > item.value())
                                .count() + 1,
                        item.className(), item.methodName(), item.value(), metricType))
                .collect(Collectors.toList());
    }

    @Override
    public List<HeatmapTopItem> getTopN(TimeWindow timeWindow, MetricType metricType) {
        return getTopN(timeWindow, metricType, properties.getTopNDefaultSize());
    }

    /**
     * 根据时间窗口构建key pattern
     */
    private String buildPatternForTimeWindow(TimeWindow timeWindow, MetricType metricType) {
        String prefix = properties.getRedisKeyPrefix() + ":" + metricType.name().toLowerCase() + ":";
        switch (timeWindow) {
            case REALTIME:
                return prefix + "realtime:*";
            case HOURLY:
                return prefix + "hourly:*";
            case DAILY:
                return prefix + "daily:*";
            default:
                return prefix + "*";
        }
    }

    /**
     * 从key获取值
     */
    private Long getValueFromKey(String key, MetricType metricType) {
        if (metricType == MetricType.PV) {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0;
        } else {
            return redisTemplate.opsForHyperLogLog().size(key);
        }
    }

    /**
     * 从key中提取完整方法名
     */
    private String extractFullNameFromKey(String key, TimeWindow timeWindow, MetricType metricType) {
        String prefix = properties.getRedisKeyPrefix() + ":" + metricType.name().toLowerCase() + ":";
        String remaining = key.substring(prefix.length());
        
        // 根据时间窗口类型移除时间部分
        switch (timeWindow) {
            case HOURLY:
                // hourly:yyyyMMddHH:class:method -> class:method
                int firstColon = remaining.indexOf(':');
                if (firstColon > 0) {
                    remaining = remaining.substring(firstColon + 1);
                }
                break;
            case DAILY:
                // daily:yyyyMMdd:class:method -> class:method
                firstColon = remaining.indexOf(':');
                if (firstColon > 0) {
                    remaining = remaining.substring(firstColon + 1);
                }
                break;
            default:
                // realtime:class:method -> class:method
                break;
        }
        
        return remaining.replace(":", ".");
    }

    @Override
    public List<HeatmapTrendPoint> getTrend(String className, String methodName, TimeWindow timeWindow, int pointCount) {
        if (!properties.isEnabled()) {
            return Collections.emptyList();
        }

        List<HeatmapTrendPoint> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        try {
            for (int i = pointCount - 1; i >= 0; i--) {
                LocalDateTime pointTime = now.minusHours(i);
                String baseKey = buildBaseKey(className, methodName);
                
                long pv = 0;
                long uv = 0;

                if (timeWindow == TimeWindow.HOURLY || timeWindow == TimeWindow.REALTIME) {
                    String pvKey = properties.getRedisKeyPrefix() + ":pv:hourly:" + 
                            pointTime.format(HOUR_FORMATTER) + ":" + baseKey;
                    String uvKey = properties.getRedisKeyPrefix() + ":uv:hourly:" + 
                            pointTime.format(HOUR_FORMATTER) + ":" + baseKey;
                    
                    String pvValue = redisTemplate.opsForValue().get(pvKey);
                    Long uvValue = redisTemplate.opsForHyperLogLog().size(uvKey);
                    
                    pv = pvValue != null ? Long.parseLong(pvValue) : 0;
                    uv = uvValue != null ? uvValue : 0;
                }

                result.add(new HeatmapTrendPoint(pointTime, pv, uv));
            }
        } catch (Exception e) {
            handleRedisError("getTrend", e);
        }

        return result;
    }

    @Override
    public void cleanupExpiredData() {
        if (!properties.isEnabled()) {
            return;
        }

        log.info("[operation-log] Starting heatmap data cleanup");
        
        try {
            // 清理过期的实时数据(理论上Redis TTL会自动处理)
            // 这里可以添加额外的清理逻辑
            
            // 清理降级缓存
            if (!fallbackPvCache.isEmpty()) {
                fallbackPvCache.clear();
                fallbackUvCache.clear();
                log.info("[operation-log] Fallback cache cleared");
            }
        } catch (Exception e) {
            log.error("[operation-log] Error during cleanup", e);
        }
    }

    @Override
    public HeatmapStatus getStatus() {
        boolean redisConnected = checkRedisConnection();
        long totalKeys = 0;
        
        try {
            Set<String> keys = redisTemplate.keys(properties.getRedisKeyPrefix() + ":*");
            totalKeys = keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.warn("[operation-log] Failed to count keys", e);
        }

        return new HeatmapStatus(
                properties.isEnabled(),
                redisConnected,
                fallbackActive.get(),
                totalKeys,
                totalKeys * 12 * 1024 // HyperLogLog约12KB每个key
        );
    }

    /**
     * 检查Redis连接状态
     */
    private boolean checkRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            if (fallbackActive.get()) {
                fallbackActive.set(false);
                log.info("[operation-log] Redis connection restored");
            }
            return true;
        } catch (Exception e) {
            fallbackActive.set(true);
            return false;
        }
    }

    /**
     * 处理Redis错误
     */
    private void handleRedisError(String operation, Exception e) {
        long errors = redisErrorCount.incrementAndGet();
        if (errors <= 5 || errors % 100 == 0) {
            log.warn("[operation-log] Redis error in {}: {} (total errors: {})", 
                    operation, e.getMessage(), errors);
        }
        fallbackActive.set(true);
    }

    /**
     * 构建基础key
     */
    private String buildBaseKey(String className, String methodName) {
        return className + ":" + methodName;
    }

    // ==================== CSV导出方法实现 ====================

    @Override
    public List<List<String>> exportRealtimeStatsToCsv() {
        List<List<String>> rows = new ArrayList<>();
        
        // 表头
        rows.add(Arrays.asList("接口类", "接口方法", "PV", "UV", "统计时间"));
        
        // 数据
        Map<String, HeatmapStats> stats = getAllRealtimeStats();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (HeatmapStats stat : stats.values()) {
            rows.add(Arrays.asList(
                    stat.className(),
                    stat.methodName(),
                    String.valueOf(stat.pv()),
                    String.valueOf(stat.uv()),
                    stat.timestamp() != null ? stat.timestamp().format(formatter) : ""
            ));
        }
        
        return rows;
    }

    @Override
    public List<List<String>> exportTopNToCsv(TimeWindow timeWindow, MetricType metricType, int topN) {
        List<List<String>> rows = new ArrayList<>();
        
        // 表头
        rows.add(Arrays.asList("排名", "接口类", "接口方法", "指标类型", "数值"));
        
        // 数据
        List<HeatmapTopItem> topItems = getTopN(timeWindow, metricType, topN);
        
        for (HeatmapTopItem item : topItems) {
            rows.add(Arrays.asList(
                    String.valueOf(item.rank()),
                    item.className(),
                    item.methodName(),
                    item.metricType().name(),
                    String.valueOf(item.value())
            ));
        }
        
        return rows;
    }

    @Override
    public List<List<String>> exportTrendToCsv(String className, String methodName, TimeWindow timeWindow, int pointCount) {
        List<List<String>> rows = new ArrayList<>();
        
        // 表头
        rows.add(Arrays.asList("时间", "PV", "UV"));
        
        // 数据
        List<HeatmapTrendPoint> points = getTrend(className, methodName, timeWindow, pointCount);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (HeatmapTrendPoint point : points) {
            rows.add(Arrays.asList(
                    point.timestamp() != null ? point.timestamp().format(formatter) : "",
                    String.valueOf(point.pv()),
                    String.valueOf(point.uv())
            ));
        }
        
        return rows;
    }
}
