package cn.creekmoon.operationLog.heatmap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    public ResponseTimeStats getResponseTimeStats(String className, String methodName, TimeWindow timeWindow) {
        String baseKey = buildMetricsKey(className, methodName, timeWindow);
        
        /* 从Redis获取响应时间统计数据 */
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(baseKey + ":responseTime");
        
        if (entries.isEmpty()) {
            return new ResponseTimeStats(className, methodName, 0, 0, 0, 0, 0, 0);
        }

        long p50 = getLongValue(entries, "p50");
        long p95 = getLongValue(entries, "p95");
        long p99 = getLongValue(entries, "p99");
        long avg = getLongValue(entries, "avg");
        long max = getLongValue(entries, "max");
        long min = getLongValue(entries, "min");

        return new ResponseTimeStats(className, methodName, p50, p95, p99, avg, max, min);
    }

    @Override
    public Map<LocalDateTime, Double> getErrorRateTrend(String className, String methodName, 
                                                         TimeWindow timeWindow, int pointCount) {
        Map<LocalDateTime, Double> result = new HashMap<>();
        String baseKey = buildMetricsKey(className, methodName, timeWindow);
        
        /* 获取错误率和总请求数，计算错误率趋势 */
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
        String pattern = properties.getRedisKeyPrefix() + ":geo:" + timeWindow.name().toLowerCase() + ":*";
        
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null) {
            for (String key : keys) {
                String region = extractRegionFromKey(key);
                Long count = getCount(key);
                result.merge(region, count, Long::sum);
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Long> getTerminalDistribution(TimeWindow timeWindow) {
        Map<String, Long> result = new HashMap<>();
        String pattern = properties.getRedisKeyPrefix() + ":terminal:" + timeWindow.name().toLowerCase() + ":*";
        
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null) {
            for (String key : keys) {
                String terminal = extractTerminalFromKey(key);
                Long count = getCount(key);
                result.merge(terminal, count, Long::sum);
            }
        }
        
        return result;
    }

    /* 辅助方法 */
    private String buildMetricsKey(String className, String methodName, TimeWindow timeWindow) {
        return properties.getRedisKeyPrefix() + ":metrics:" + timeWindow.name().toLowerCase() + 
               ":" + className + ":" + methodName;
    }

    private long getLongValue(Map<Object, Object> entries, String key) {
        Object value = entries.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0;
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
