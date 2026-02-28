package cn.creekmoon.operationLog.heatmap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 扩展热力图服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtendedHeatmapServiceImpl implements ExtendedHeatmapService {

    private final StringRedisTemplate redisTemplate;
    private final HeatmapProperties properties;
    private final HeatmapService heatmapService;

    @Override
    public void recordVisit(String className, String methodName, String userId, LocalDateTime timestamp) {
        heatmapService.recordVisit(className, methodName, userId, timestamp);
    }

    @Override
    public void recordVisit(String className, String methodName, String userId) {
        heatmapService.recordVisit(className, methodName, userId);
    }

    @Override
    public HeatmapStats getRealtimeStats(String className, String methodName) {
        return heatmapService.getRealtimeStats(className, methodName);
    }

    @Override
    public Map<String, HeatmapStats> getAllRealtimeStats() {
        return heatmapService.getAllRealtimeStats();
    }

    @Override
    public java.util.List<HeatmapTopItem> getTopN(TimeWindow timeWindow, MetricType metricType, int topN) {
        return heatmapService.getTopN(timeWindow, metricType, topN);
    }

    @Override
    public java.util.List<HeatmapTopItem> getTopN(TimeWindow timeWindow, MetricType metricType) {
        return heatmapService.getTopN(timeWindow, metricType);
    }

    @Override
    public java.util.List<HeatmapTrendPoint> getTrend(String className, String methodName, TimeWindow timeWindow, int pointCount) {
        return heatmapService.getTrend(className, methodName, timeWindow, pointCount);
    }

    @Override
    public void cleanupExpiredData() {
        heatmapService.cleanupExpiredData();
    }

    @Override
    public HeatmapStatus getStatus() {
        return heatmapService.getStatus();
    }

    @Override
    public java.util.List<java.util.List<String>> exportRealtimeStatsToCsv() {
        return heatmapService.exportRealtimeStatsToCsv();
    }

    @Override
    public java.util.List<java.util.List<String>> exportTopNToCsv(TimeWindow timeWindow, MetricType metricType, int topN) {
        return heatmapService.exportTopNToCsv(timeWindow, metricType, topN);
    }

    @Override
    public java.util.List<java.util.List<String>> exportTrendToCsv(String className, String methodName, TimeWindow timeWindow, int pointCount) {
        return heatmapService.exportTrendToCsv(className, methodName, timeWindow, pointCount);
    }

    @Override
    public ResponseTimeStats getResponseTimeStats(String className, String methodName, TimeWindow timeWindow) {
        String baseKey = buildMetricsKey(className, methodName, timeWindow);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(baseKey + ":responseTime");
        
        if (entries.isEmpty()) {
            return new ResponseTimeStats(className, methodName, 0, 0, 0, 0, 0, 0);
        }

        return new ResponseTimeStats(
            className, methodName,
            getLongValue(entries, "p50"),
            getLongValue(entries, "p95"),
            getLongValue(entries, "p99"),
            getLongValue(entries, "avg"),
            getLongValue(entries, "max"),
            getLongValue(entries, "min")
        );
    }

    @Override
    public Map<LocalDateTime, Double> getErrorRateTrend(String className, String methodName, TimeWindow timeWindow, int pointCount) {
        Map<LocalDateTime, Double> result = new HashMap<>();
        String baseKey = buildMetricsKey(className, methodName, timeWindow);
        
        for (int i = 0; i < pointCount; i++) {
            LocalDateTime timestamp = calculateTimestamp(timeWindow, i, pointCount);
            String timeKey = baseKey + ":" + formatTimestamp(timestamp, timeWindow);
            
            Long total = getCount(timeKey + ":total");
            Long errors = getCount(timeKey + ":error");
            
            double errorRate = total > 0 ? (double) errors / total : 0.0;
            result.put(timestamp, errorRate);
        }
        
        return result;
    }

    @Override
    public Map<String, Long> getGeoDistribution(TimeWindow timeWindow) {
        Map<String, Long> result = new HashMap<>();
        
        if (!properties.isEnabled()) {
            return result;
        }
        
        try {
            String pattern = properties.getRedisKeyPrefix() + ":geo:" + timeWindow.name().toLowerCase() + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    String region = extractRegionFromKey(key);
                    Long count = getCount(key);
                    result.merge(region, count, Long::sum);
                }
            }
        } catch (Exception e) {
            log.warn("[operation-log] 获取地域分布失败: {}", e.getMessage());
        }
        
        return result;
    }

    @Override
    public Map<String, Long> getTerminalDistribution(TimeWindow timeWindow) {
        Map<String, Long> result = new HashMap<>();
        
        if (!properties.isEnabled()) {
            return result;
        }
        
        try {
            String pattern = properties.getRedisKeyPrefix() + ":terminal:" + timeWindow.name().toLowerCase() + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null) {
                for (String key : keys) {
                    String terminal = extractTerminalFromKey(key);
                    Long count = getCount(key);
                    result.merge(terminal, count, Long::sum);
                }
            }
        } catch (Exception e) {
            log.warn("[operation-log] 获取终端分布失败: {}", e.getMessage());
        }
        
        return result;
    }

    private String buildMetricsKey(String className, String methodName, TimeWindow timeWindow) {
        return properties.getRedisKeyPrefix() + ":metrics:" + timeWindow.name().toLowerCase() + ":" + className + ":" + methodName;
    }

    private long getLongValue(Map<Object, Object> entries, String key) {
        Object value = entries.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    private Long getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    private LocalDateTime calculateTimestamp(TimeWindow timeWindow, int offset, int total) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeWindow) {
            case REALTIME -> now.minusMinutes(total - offset);
            case HOURLY -> now.minusHours(total - offset);
            case DAILY -> now.minusDays(total - offset);
        };
    }

    private String formatTimestamp(LocalDateTime timestamp, TimeWindow timeWindow) {
        return switch (timeWindow) {
            case REALTIME -> String.format("%02d%02d", timestamp.getHour(), timestamp.getMinute());
            case HOURLY -> String.format("%02d", timestamp.getHour());
            case DAILY -> String.format("%02d", timestamp.getDayOfMonth());
        };
    }

    private String extractRegionFromKey(String key) {
        int lastColon = key.lastIndexOf(':');
        return lastColon > 0 ? key.substring(lastColon + 1) : "unknown";
    }

    private String extractTerminalFromKey(String key) {
        int lastColon = key.lastIndexOf(':');
        return lastColon > 0 ? key.substring(lastColon + 1) : "unknown";
    }
}
