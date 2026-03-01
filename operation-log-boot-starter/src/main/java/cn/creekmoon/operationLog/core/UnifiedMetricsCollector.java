package cn.creekmoon.operationLog.core;

import java.util.Map;

/**
 * 统一指标收集器接口
 * 定义指标收集的核心能力，支持内存和 Redis 两种实现
 */
public interface UnifiedMetricsCollector {

    /**
     * 请求开始（并发计数）
     */
    void requestStarted();

    /**
     * 请求结束（并发计数）
     */
    void requestEnded();

    /**
     * 记录接口调用
     *
     * @param endpoint     接口标识
     * @param responseTime 响应时间（毫秒）
     */
    void record(String endpoint, long responseTime);

    /**
     * 记录接口错误
     *
     * @param endpoint 接口标识
     */
    void recordError(String endpoint);

    /**
     * 获取指定接口的指标快照
     *
     * @param endpoint 接口标识
     * @return 指标快照
     */
    EndpointMetricsSnapshot getEndpointMetrics(String endpoint);

    /**
     * 获取所有接口的指标快照
     *
     * @return 接口标识到指标快照的映射
     */
    Map<String, EndpointMetricsSnapshot> getAllEndpointMetrics();

    /**
     * 获取最慢的接口
     *
     * @param limit 返回数量限制
     * @return 接口标识到指标快照的映射（按平均响应时间降序）
     */
    Map<String, EndpointMetricsSnapshot> getSlowestEndpoints(int limit);

    /**
     * 获取错误最多的接口
     *
     * @param limit 返回数量限制
     * @return 接口标识到指标快照的映射（按错误率降序）
     */
    Map<String, EndpointMetricsSnapshot> getErrorEndpoints(int limit);

    /**
     * 获取全局错误率
     *
     * @return 错误率（0.0 ~ 1.0）
     */
    double getGlobalErrorRate();

    /**
     * 获取总请求数
     *
     * @return 总请求数
     */
    long getTotalRequests();

    /**
     * 获取当前 QPS
     *
     * @return 当前每秒请求数
     */
    double getCurrentQps();

    /**
     * 获取平均 QPS
     *
     * @return 平均每秒请求数
     */
    double getAvgQps();

    /**
     * 获取当前并发请求数
     *
     * @return 当前并发数
     */
    int getCurrentConcurrentRequests();

    /**
     * 获取峰值并发请求数
     *
     * @return 峰值并发数
     */
    int getPeakConcurrentRequests();

    /**
     * 重置所有指标
     */
    void reset();

    /**
     * 端点指标快照
     * 不可变记录，保存某一时刻的指标数据
     */
    record EndpointMetricsSnapshot(
            String endpoint,
            long totalCount,
            long errorCount,
            long totalResponseTime,
            long avgResponseTime,
            long maxResponseTime,
            long minResponseTime,
            double errorRate
    ) {
        public EndpointMetricsSnapshot {
            if (endpoint == null) endpoint = "";
        }

        public static EndpointMetricsSnapshot empty(String endpoint) {
            return new EndpointMetricsSnapshot(endpoint, 0, 0, 0, 0, 0, 0, 0.0);
        }
    }
}
