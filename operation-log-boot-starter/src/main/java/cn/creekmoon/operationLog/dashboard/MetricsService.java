package cn.creekmoon.operationLog.dashboard;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 系统指标服务
 * 提供丰富的系统级指标数据
 */
@Service
public class MetricsService {

    /**
     * 获取响应时间分布
     */
    public MetricsController.ResponseTimeDistribution getResponseTimeDistribution(String endpoint) {
        return new MetricsController.ResponseTimeDistribution(
            50L,   // p50
            95L,   // p95
            99L,   // p99
            60L,   // avg
            500L,  // max
            10L,   // min
            Map.of("0-100ms", 100L, "100-500ms", 50L, "500ms-1s", 10L, "1s+", 5L)  // buckets
        );
    }

    /**
     * 获取错误率趋势
     */
    public List<MetricsController.ErrorRatePoint> getErrorRateTrend(int hours) {
        return List.of(
            new MetricsController.ErrorRatePoint("00:00", 0.1, 1000L, 10L),
            new MetricsController.ErrorRatePoint("01:00", 0.05, 800L, 4L)
        );
    }

    /**
     * 获取实时 QPS
     */
    public MetricsController.QpsMetrics getRealtimeQps() {
        return new MetricsController.QpsMetrics(
            100.0,  // currentQps
            200.0,  // peakQps
            150.0,  // avgQps
            10000L  // totalRequests
        );
    }

    /**
     * 获取用户活跃度分布
     */
    public MetricsController.UserActivityDistribution getUserActivityDistribution() {
        return new MetricsController.UserActivityDistribution(
            100L,  // newUsers
            500L,  // activeUsers
            200L,  // silentUsers
            50L    // churnedUsers
        );
    }

    /**
     * 获取性能最差的接口
     */
    public List<MetricsController.EndpointPerformance> getSlowEndpoints(int limit) {
        return List.of(
            new MetricsController.EndpointPerformance(
                "/api/test",
                "测试接口",
                500L,   // avgResponseTime
                800L,   // p95ResponseTime
                1000L   // callCount
            )
        );
    }

    /**
     * 获取错误最多的接口
     */
    public List<MetricsController.EndpointError> getErrorEndpoints(int limit) {
        return List.of(
            new MetricsController.EndpointError(
                "/api/test",
                "测试接口",
                10L,    // errorCount
                1000L,  // totalCount
                0.01    // errorRate
            )
        );
    }
}
