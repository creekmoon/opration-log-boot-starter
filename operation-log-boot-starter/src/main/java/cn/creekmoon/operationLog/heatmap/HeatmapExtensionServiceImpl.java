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
 * 热力图扩展服务实现
 * 
 * 提供热力图的高级统计功能，包括：
 * - 响应时间分位数统计 (P50/P95/P99)
 * - 错误率趋势分析
 * - 地域分布统计
 * - 终端分布统计
 * 
 * 设计说明：
 * 采用组合模式，依赖原HeatmapService获取基础数据，
 * 本服务只实现扩展功能，避免接口继承带来的复杂性。
 * 
 * @author creekmoon
 * @since 2.2.0
 * @see HeatmapService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeatmapExtensionServiceImpl implements HeatmapExtensionService {

    /** Redis操作模板 */
    private final StringRedisTemplate redisTemplate;
    
    /** 热力图配置属性 */
    private final HeatmapProperties properties;
    
    /** 基础热力图服务（组合关系） */
    private final HeatmapService heatmapService;

    /**
     * 获取响应时间统计
     * 
     * @param className 类名
     * @param methodName 方法名
     * @param timeWindow 时间窗口
     * @return 响应时间统计，包含P50/P95/P99/平均值/最大值/最小值
     */
    @Override
    public ResponseTimeStats getResponseTimeStats(String className, String methodName, 
                                                   HeatmapService.TimeWindow timeWindow) {
        String metricsKey = buildMetricsKey(className, methodName, timeWindow);
        Map<Object, Object> entries = redisTemplate.opsForHash()
                .entries(metricsKey + ":responseTime");
        
        if (entries.isEmpty()) {
            return ResponseTimeStats.empty(className, methodName);
        }

        return new ResponseTimeStats(
            className, 
            methodName,
            getLongValue(entries, "p50"),
            getLongValue(entries, "p95"),
            getLongValue(entries, "p99"),
            getLongValue(entries, "avg"),
            getLongValue(entries, "max"),
            getLongValue(entries, "min")
        );
    }

    /**
     * 获取错误率趋势
     * 
     * @param className 类名
     * @param methodName 方法名
     * @param timeWindow 时间窗口
     * @param pointCount 数据点数量
     * @return 时间戳到错误率的映射
     */
    @Override
    public Map<LocalDateTime, Double> getErrorRateTrend(String className, String methodName, 
                                                         HeatmapService.TimeWindow timeWindow, 
                                                         int pointCount) {
        Map<LocalDateTime, Double> result = new HashMap<>();
        String baseKey = buildMetricsKey(className, methodName, timeWindow);
        
        for (int i = 0; i < pointCount; i++) {
            LocalDateTime timestamp = calculateTimestamp(timeWindow, i, pointCount);
            String timeKey = buildTimeKey(baseKey, timestamp, timeWindow);
            
            Long total = getCount(timeKey + ":total");
            Long errors = getCount(timeKey + ":error");
            
            double errorRate = total > 0 ? (double) errors / total : 0.0;
            result.put(timestamp, errorRate);
        }
        
        return result;
    }

    /**
     * 获取地域分布统计
     * 
     * @param timeWindow 时间窗口
     * @return 地域到访问量的映射
     */
    @Override
    public Map<String, Long> getGeoDistribution(HeatmapService.TimeWindow timeWindow) {
        Map<String, Long> result = new HashMap<>();
        
        if (!properties.isEnabled()) {
            log.debug("[operation-log] 热力图功能未启用，返回空地域分布");
            return result;
        }
        
        try {
            String pattern = buildGeoPattern(timeWindow);
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    String region = extractLastSegment(key);
                    Long count = getCount(key);
                    result.merge(region, count, Long::sum);
                }
            }
        } catch (Exception e) {
            log.warn("[operation-log] 获取地域分布失败: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取终端分布统计
     * 
     * @param timeWindow 时间窗口
     * @return 终端类型到访问量的映射
     */
    @Override
    public Map<String, Long> getTerminalDistribution(HeatmapService.TimeWindow timeWindow) {
        Map<String, Long> result = new HashMap<>();
        
        if (!properties.isEnabled()) {
            log.debug("[operation-log] 热力图功能未启用，返回空终端分布");
            return result;
        }
        
        try {
            String pattern = buildTerminalPattern(timeWindow);
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                for (String key : keys) {
                    String terminal = extractLastSegment(key);
                    Long count = getCount(key);
                    result.merge(terminal, count, Long::sum);
                }
            }
        } catch (Exception e) {
            log.warn("[operation-log] 获取终端分布失败: {}", e.getMessage());
        }
        
        return result;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建指标数据Key
     */
    private String buildMetricsKey(String className, String methodName, 
                                    HeatmapService.TimeWindow timeWindow) {
        return String.format("%s:metrics:%s:%s:%s",
                properties.getRedisKeyPrefix(),
                timeWindow.name().toLowerCase(),
                className,
                methodName);
    }

    /**
     * 构建时间维度Key
     */
    private String buildTimeKey(String baseKey, LocalDateTime timestamp, 
                                 HeatmapService.TimeWindow timeWindow) {
        String timeSuffix = formatTimestamp(timestamp, timeWindow);
        return baseKey + ":" + timeSuffix;
    }

    /**
     * 构建地域查询模式
     */
    private String buildGeoPattern(HeatmapService.TimeWindow timeWindow) {
        return String.format("%s:geo:%s:*",
                properties.getRedisKeyPrefix(),
                timeWindow.name().toLowerCase());
    }

    /**
     * 构建终端查询模式
     */
    private String buildTerminalPattern(HeatmapService.TimeWindow timeWindow) {
        return String.format("%s:terminal:%s:*",
                properties.getRedisKeyPrefix(),
                timeWindow.name().toLowerCase());
    }

    /**
     * 从Redis获取Long值
     */
    private long getLongValue(Map<Object, Object> entries, String key) {
        Object value = entries.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    /**
     * 从Redis获取计数
     */
    private Long getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 计算时间戳
     */
    private LocalDateTime calculateTimestamp(HeatmapService.TimeWindow timeWindow, 
                                              int offset, int total) {
        LocalDateTime now = LocalDateTime.now();
        return switch (timeWindow) {
            case REALTIME -> now.minusMinutes(total - offset);
            case HOURLY -> now.minusHours(total - offset);
            case DAILY -> now.minusDays(total - offset);
        };
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(LocalDateTime timestamp, 
                                    HeatmapService.TimeWindow timeWindow) {
        return switch (timeWindow) {
            case REALTIME -> String.format("%02d%02d", 
                    timestamp.getHour(), timestamp.getMinute());
            case HOURLY -> String.format("%02d", timestamp.getHour());
            case DAILY -> String.format("%02d", timestamp.getDayOfMonth());
        };
    }

    /**
     * 提取Key的最后一段
     */
    private String extractLastSegment(String key) {
        int lastColon = key.lastIndexOf(':');
        return lastColon > 0 ? key.substring(lastColon + 1) : "unknown";
    }
}
