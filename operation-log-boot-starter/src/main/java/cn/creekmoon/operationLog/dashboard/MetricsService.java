package cn.creekmoon.operationLog.dashboard;

import cn.creekmoon.operationLog.core.MetricsCollector;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统指标服务
 * 提供真实系统级指标数据采集
 */
@Service
public class MetricsService {

    /**
     * 获取响应时间分布
     */
    public MetricsController.ResponseTimeDistribution getResponseTimeDistribution(String endpoint) {
        MetricsCollector.EndpointMetrics metrics;
        
        if (endpoint != null && !endpoint.isEmpty()) {
            metrics = MetricsCollector.getEndpointMetrics(endpoint);
        } else {
            // 获取全局聚合数据
            metrics = getGlobalAggregatedMetrics();
        }
        
        if (metrics == null) {
            // 无数据时返回空值
            return new MetricsController.ResponseTimeDistribution(
                0L, 0L, 0L, 0L, 0L, 0L,
                Map.of("0-100ms", 0L, "100-500ms", 0L, "500ms-1s", 0L, "1s+", 0L)
            );
        }
        
        MetricsCollector.PercentileResult percentiles = metrics.getPercentiles();
        Map<String, Long> buckets = metrics.getDistributionBuckets();
        
        return new MetricsController.ResponseTimeDistribution(
            percentiles.p50(),
            percentiles.p95(),
            percentiles.p99(),
            percentiles.avg(),
            percentiles.max(),
            percentiles.min(),
            buckets
        );
    }

    /**
     * 获取错误率趋势 (最近N小时)
     */
    public List<MetricsController.ErrorRatePoint> getErrorRateTrend(int hours) {
        List<MetricsController.ErrorRatePoint> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        // 获取所有接口的聚合数据
        Map<String, MetricsCollector.EndpointMetrics> allMetrics = MetricsCollector.getAllEndpointMetrics();
        
        // 如果没有任何数据，返回空趋势
        if (allMetrics.isEmpty()) {
            return trend;
        }
        
        // 计算全局错误率
        long totalRequests = allMetrics.values().stream()
                .mapToLong(m -> m.getTotalCount().sum())
                .sum();
        long totalErrors = allMetrics.values().stream()
                .mapToLong(m -> m.getErrorCount().sum())
                .sum();
        double errorRate = totalRequests > 0 ? (double) totalErrors / totalRequests : 0.0;
        
        // 生成当前时间点的数据点
        // 注意：由于是内存实时计算，只返回当前数据点
        // 后续可通过 Redis 实现历史趋势
        String currentTime = LocalDateTime.now().format(formatter);
        trend.add(new MetricsController.ErrorRatePoint(
            currentTime,
            errorRate,
            totalRequests,
            totalErrors
        ));
        
        return trend;
    }

    /**
     * 获取实时 QPS
     */
    public MetricsController.QpsMetrics getRealtimeQps() {
        double currentQps = MetricsCollector.getCurrentQps();
        double avgQps = MetricsCollector.getAvgQps();
        long totalRequests = MetricsCollector.getTotalRequests();
        
        // 峰值 QPS 通过并发数估算
        double peakQps = currentQps * 1.5; // 简化估算
        
        return new MetricsController.QpsMetrics(
            currentQps,
            peakQps,
            avgQps,
            totalRequests
        );
    }

    /**
     * 获取用户活跃度分布
     * 注：此数据需要从用户画像模块获取，暂时返回空值
     */
    public MetricsController.UserActivityDistribution getUserActivityDistribution() {
        // TODO: 从 ProfileService 获取真实用户数据
        // 目前返回空值，待后续集成用户画像模块
        return new MetricsController.UserActivityDistribution(0L, 0L, 0L, 0L);
    }

    /**
     * 获取性能最差的接口
     */
    public List<MetricsController.EndpointPerformance> getSlowEndpoints(int limit) {
        List<Map.Entry<String, MetricsCollector.EndpointMetrics>> slowest = 
            MetricsCollector.getSlowestEndpoints(limit);
        
        return slowest.stream()
            .map(entry -> {
                String endpoint = entry.getKey();
                MetricsCollector.EndpointMetrics metrics = entry.getValue();
                MetricsCollector.PercentileResult percentiles = metrics.getPercentiles();
                
                return new MetricsController.EndpointPerformance(
                    endpoint,
                    extractOperationName(endpoint),
                    metrics.getAvgResponseTime(),
                    percentiles.p95(),
                    metrics.getTotalCount().sum()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取错误最多的接口
     */
    public List<MetricsController.EndpointError> getErrorEndpoints(int limit) {
        List<Map.Entry<String, MetricsCollector.EndpointMetrics>> errorEndpoints = 
            MetricsCollector.getErrorEndpoints(limit);
        
        return errorEndpoints.stream()
            .map(entry -> {
                String endpoint = entry.getKey();
                MetricsCollector.EndpointMetrics metrics = entry.getValue();
                long totalCount = metrics.getTotalCount().sum();
                long errorCount = metrics.getErrorCount().sum();
                double errorRate = totalCount > 0 ? (double) errorCount / totalCount : 0.0;
                
                return new MetricsController.EndpointError(
                    endpoint,
                    extractOperationName(endpoint),
                    errorCount,
                    totalCount,
                    errorRate
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取全局聚合指标
     */
    private MetricsCollector.EndpointMetrics getGlobalAggregatedMetrics() {
        Map<String, MetricsCollector.EndpointMetrics> allMetrics = MetricsCollector.getAllEndpointMetrics();
        
        if (allMetrics.isEmpty()) {
            return null;
        }
        
        // 创建虚拟的全局聚合对象
        MetricsCollector.EndpointMetrics global = new MetricsCollector.EndpointMetrics("GLOBAL");
        
        for (MetricsCollector.EndpointMetrics metrics : allMetrics.values()) {
            // 复制统计数据到全局对象
            long count = metrics.getTotalCount().sum();
            long errors = metrics.getErrorCount().sum();
            long totalTime = metrics.getTotalResponseTime().sum();
            
            for (int i = 0; i < count; i++) {
                global.recordResponseTime(count > 0 ? totalTime / count : 0);
            }
            for (int i = 0; i < errors; i++) {
                global.recordError();
            }
        }
        
        return global;
    }

    /**
     * 从 endpoint 提取操作名称
     */
    private String extractOperationName(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return "Unknown";
        }
        
        // 提取方法名 (最后一部分)
        int lastDot = endpoint.lastIndexOf('.');
        if (lastDot > 0 && lastDot < endpoint.length() - 1) {
            return endpoint.substring(lastDot + 1);
        }
        
        return endpoint;
    }
}
