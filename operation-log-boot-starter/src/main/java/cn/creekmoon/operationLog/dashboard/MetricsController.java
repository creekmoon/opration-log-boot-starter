package cn.creekmoon.operationLog.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 系统指标控制器
 * 提供丰富的系统级指标 API
 */
@RestController
@RequestMapping("/operation-log/dashboard/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private final MetricsService metricsService;

    /**
     * 响应时间分布
     */
    @GetMapping("/response-time")
    public ResponseTimeDistribution getResponseTimeDistribution(
            @RequestParam(required = false) String endpoint) {
        return metricsService.getResponseTimeDistribution(endpoint);
    }

    /**
     * 错误率趋势
     */
    @GetMapping("/error-rate")
    public List<ErrorRatePoint> getErrorRateTrend(
            @RequestParam(defaultValue = "24") int hours) {
        return metricsService.getErrorRateTrend(hours);
    }

    /**
     * 实时 QPS
     */
    @GetMapping("/qps")
    public QpsMetrics getRealtimeQps() {
        return metricsService.getRealtimeQps();
    }

    /**
     * 用户活跃度分布
     */
    @GetMapping("/user-activity")
    public UserActivityDistribution getUserActivityDistribution() {
        return metricsService.getUserActivityDistribution();
    }

    /**
     * 性能最差的接口
     */
    @GetMapping("/slow-endpoints")
    public List<EndpointPerformance> getSlowEndpoints(
            @RequestParam(defaultValue = "10") int limit) {
        return metricsService.getSlowEndpoints(limit);
    }

    /**
     * 错误最多的接口
     */
    @GetMapping("/error-endpoints")
    public List<EndpointError> getErrorEndpoints(
            @RequestParam(defaultValue = "10") int limit) {
        return metricsService.getErrorEndpoints(limit);
    }

    // ==================== DTOs ====================

    public record ResponseTimeDistribution(
            long p50,
            long p95,
            long p99,
            long avg,
            long max,
            long min,
            Map<String, Long> buckets  // 0-100ms, 100-500ms, 500ms-1s, 1s+
    ) {}

    public record ErrorRatePoint(
            String time,
            double errorRate,
            long totalCount,
            long errorCount
    ) {}

    public record QpsMetrics(
            double currentQps,
            double peakQps,
            double avgQps,
            long totalRequests
    ) {}

    public record UserActivityDistribution(
            long newUsers,        // 7天内新用户
            long activeUsers,     // 7天内有活跃
            long silentUsers,     // 7-30天无活跃
            long churnedUsers     // 30天+无活跃
    ) {}

    public record EndpointPerformance(
            String endpoint,
            String operationName,
            long avgResponseTime,
            long p95ResponseTime,
            long callCount
    ) {}

    public record EndpointError(
            String endpoint,
            String operationName,
            long errorCount,
            long totalCount,
            double errorRate
    ) {}
}
