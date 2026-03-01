package cn.creekmoon.operationLog.core;

import java.util.Map;

/**
 * 统一指标收集器接口
 * 
 * 抽象层，定义指标收集的核心操作。
 * 实现类可以是内存版或 Redis 版。
 * 
 * @author CodeSmith
 * @since 1.0.0
 */
public interface UnifiedMetricsCollector {
    
    /**
     * 记录请求开始（增加并发计数）
     */
    void requestStarted();
    
    /**
     * 记录请求结束（减少并发计数）
     */
    void requestEnded();
    
    /**
     * 记录接口响应时间
     * 
     * @param endpoint 接口标识（类名.方法名）
     * @param responseTimeMillis 响应时间（毫秒）
     */
    void record(String endpoint, long responseTimeMillis);
    
    /**
     * 记录接口错误
     * 
     * @param endpoint 接口标识
     */
    void recordError(String endpoint);
    
    /**
     * 获取当前并发请求数
     */
    long getCurrentConcurrentRequests();
    
    /**
     * 获取峰值并发请求数
     */
    long getPeakConcurrentRequests();
    
    /**
     * 获取总请求数
     */
    long getTotalRequests();
    
    /**
     * 获取实时 QPS
     */
    double getCurrentQps();
    
    /**
     * 获取平均 QPS（最近60秒）
     */
    double getAvgQps();
    
    /**
     * 获取全局错误率
     */
    double getGlobalErrorRate();
    
    /**
     * 获取所有接口的指标
     */
    Map<String, EndpointMetricsSnapshot> getAllEndpointMetrics();
    
    /**
     * 获取指定接口的指标
     */
    EndpointMetricsSnapshot getEndpointMetrics(String endpoint);
    
    /**
     * 获取性能最差的接口
     * 
     * @param limit 返回数量限制
     */
    Map<String, EndpointMetricsSnapshot> getSlowestEndpoints(int limit);
    
    /**
     * 获取错误最多的接口
     * 
     * @param limit 返回数量限制
     */
    Map<String, EndpointMetricsSnapshot> getErrorEndpoints(int limit);
    
    /**
     * 端点指标快照
     * 用于跨层传输的数据结构，避免暴露内部实现
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
        public static EndpointMetricsSnapshot empty(String endpoint) {
            return new EndpointMetricsSnapshot(
                    endpoint, 0, 0, 0, 0, 0, 0, 0.0
            );
        }
    }
}
