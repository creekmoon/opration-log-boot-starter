package cn.creekmoon.operationLog.dashboard;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 系统指标服务
 */
@Service
public class MetricsService {

    /**
     * 获取响应时间分布
     */
    public ResponseTimeDistribution getResponseTimeDistribution(String endpoint) {
        return new ResponseTimeDistribution(
            List.of(10, 20, 30, 25, 15),
            List.of("<100ms", "100-500ms", "500ms-1s", "1-2s", ">2s")
        );
    }

    /**
     * 获取错误分析
     */
    public ErrorAnalysis getErrorAnalysis(@SuppressWarnings("unused") String endpoint) {
        return new ErrorAnalysis(
            Map.of("4xx", 10L, "5xx", 5L),
            List.of()
        );
    }

    /**
     * 获取系统健康度
     */
    public SystemHealth getSystemHealth() {
        return new SystemHealth(95, "健康", "系统运行正常");
    }

    /**
     * 获取性能指标
     */
    public PerformanceMetrics getPerformanceMetrics() {
        return new PerformanceMetrics(
            100, 50, 200, 80
        );
    }

    /**
     * 获取错误率趋势
     */
    public List<Double> getErrorRateTrend(int hours) {
        return List.of(0.1, 0.2, 0.15, 0.1, 0.05);
    }

    /**
     * 获取实时 QPS
     */
    public Map<String, Object> getRealtimeQps() {
        return Map.of("current", 100, "peak", 200, "avg", 150);
    }

    /**
     * 获取用户活动分布
     */
    public Map<String, Long> getUserActivityDistribution() {
        return Map.of("active", 100L, "inactive", 50L);
    }

    /**
     * 获取慢端点
     */
    public List<SlowEndpoint> getSlowEndpoints(int limit) {
        return List.of(new SlowEndpoint("/api/test", 1000L));
    }

    /**
     * 获取错误端点
     */
    public List<ErrorEndpoint> getErrorEndpoints(int limit) {
        return List.of(new ErrorEndpoint("/api/test", 10L));
    }

    // DTOs
    public record SlowEndpoint(String endpoint, long avgResponseTime) {}
    public record ErrorEndpoint(String endpoint, long errorCount) {}
    public record ResponseTimeDistribution(List<Integer> data, List<String> labels) {}
    public record ErrorAnalysis(Map<String, Long> typeDistribution, List<RecentError> recentErrors) {}
    public record RecentError(String timestamp, String endpoint, String errorType, String message) {}
    public record SystemHealth(int score, String status, String message) {}
    public record PerformanceMetrics(long avgResponseTime, long qps, long maxResponseTime, long minResponseTime) {}
}
