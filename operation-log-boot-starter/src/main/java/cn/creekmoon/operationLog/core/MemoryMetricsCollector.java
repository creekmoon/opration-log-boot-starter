package cn.creekmoon.operationLog.core;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * 内存版统一指标收集器
 * 单实例高性能实现，使用 ConcurrentHashMap 保证线程安全
 */
@Component
public class MemoryMetricsCollector implements UnifiedMetricsCollector {

    // 端点级指标存储
    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetrics = new ConcurrentHashMap<>();
    
    // 全局计数器
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder totalErrors = new LongAdder();
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    
    // 并发请求计数
    private final AtomicInteger currentConcurrent = new AtomicInteger(0);
    private final AtomicInteger peakConcurrent = new AtomicInteger(0);
    
    // QPS 计算
    private final AtomicLong qpsWindowStart = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger qpsWindowRequests = new AtomicInteger(0);
    private volatile double currentQps = 0.0;
    private volatile double avgQps = 0.0;

    @Override
    public void requestStarted() {
        int current = currentConcurrent.incrementAndGet();
        // 更新峰值
        int peak;
        do {
            peak = peakConcurrent.get();
        } while (current > peak && !peakConcurrent.compareAndSet(peak, current));
    }

    @Override
    public void requestEnded() {
        currentConcurrent.decrementAndGet();
    }

    @Override
    public void record(String endpoint, long responseTime) {
        EndpointMetrics metrics = endpointMetrics.computeIfAbsent(endpoint, k -> new EndpointMetrics(endpoint));
        metrics.record(responseTime);
        
        totalRequests.increment();
        totalResponseTime.addAndGet(responseTime);
        
        // QPS 计算
        updateQps();
    }

    @Override
    public void recordError(String endpoint) {
        EndpointMetrics metrics = endpointMetrics.computeIfAbsent(endpoint, k -> new EndpointMetrics(endpoint));
        metrics.recordError();
        totalErrors.increment();
    }

    @Override
    public EndpointMetricsSnapshot getEndpointMetrics(String endpoint) {
        EndpointMetrics metrics = endpointMetrics.get(endpoint);
        if (metrics == null) {
            return EndpointMetricsSnapshot.empty(endpoint);
        }
        return metrics.toSnapshot();
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getAllEndpointMetrics() {
        return endpointMetrics.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toSnapshot()
                ));
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getSlowestEndpoints(int limit) {
        return endpointMetrics.values().stream()
                .sorted(Comparator.comparingLong(EndpointMetrics::getAvgResponseTime).reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        EndpointMetrics::getEndpoint,
                        EndpointMetrics::toSnapshot,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getErrorEndpoints(int limit) {
        return endpointMetrics.values().stream()
                .sorted(Comparator.comparingDouble(EndpointMetrics::getErrorRate).reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        EndpointMetrics::getEndpoint,
                        EndpointMetrics::toSnapshot,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new
                ));
    }

    @Override
    public double getGlobalErrorRate() {
        long total = totalRequests.sum();
        return total > 0 ? (double) totalErrors.sum() / total : 0.0;
    }

    @Override
    public long getTotalRequests() {
        return totalRequests.sum();
    }

    @Override
    public double getCurrentQps() {
        updateQps();
        return currentQps;
    }

    @Override
    public double getAvgQps() {
        return avgQps;
    }

    @Override
    public int getCurrentConcurrentRequests() {
        return currentConcurrent.get();
    }

    @Override
    public int getPeakConcurrentRequests() {
        return peakConcurrent.get();
    }

    @Override
    public void reset() {
        endpointMetrics.clear();
        totalRequests.reset();
        totalErrors.reset();
        totalResponseTime.set(0);
        currentConcurrent.set(0);
        peakConcurrent.set(0);
        qpsWindowStart.set(System.currentTimeMillis());
        qpsWindowRequests.set(0);
        currentQps = 0.0;
        avgQps = 0.0;
    }

    /**
     * 更新 QPS 计算
     */
    private void updateQps() {
        long now = System.currentTimeMillis();
        long windowStart = qpsWindowStart.get();
        
        if (now - windowStart >= 1000) {
            // 每秒更新一次
            int requests = qpsWindowRequests.getAndSet(1);
            if (now - windowStart < 2000) {
                // 正常情况：使用滑动窗口
                currentQps = requests;
                avgQps = avgQps * 0.8 + currentQps * 0.2; // 指数移动平均
            }
            qpsWindowStart.set(now);
        } else {
            qpsWindowRequests.incrementAndGet();
        }
    }

    /**
     * 端点级指标（内部类）
     */
    private static class EndpointMetrics {
        private final String endpoint;
        private final LongAdder totalCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

        EndpointMetrics(String endpoint) {
            this.endpoint = endpoint;
        }

        void record(long responseTime) {
            totalCount.increment();
            totalResponseTime.addAndGet(responseTime);
            
            // 更新最大响应时间
            long currentMax;
            do {
                currentMax = maxResponseTime.get();
            } while (responseTime > currentMax && !maxResponseTime.compareAndSet(currentMax, responseTime));
            
            // 更新最小响应时间
            long currentMin;
            do {
                currentMin = minResponseTime.get();
            } while (responseTime < currentMin && !minResponseTime.compareAndSet(currentMin, responseTime));
        }

        void recordError() {
            errorCount.increment();
        }

        String getEndpoint() {
            return endpoint;
        }

        long getAvgResponseTime() {
            long count = totalCount.sum();
            return count > 0 ? totalResponseTime.get() / count : 0;
        }

        double getErrorRate() {
            long count = totalCount.sum();
            return count > 0 ? (double) errorCount.sum() / count : 0.0;
        }

        EndpointMetricsSnapshot toSnapshot() {
            long count = totalCount.sum();
            long errors = errorCount.sum();
            long totalTime = totalResponseTime.get();
            
            return new EndpointMetricsSnapshot(
                    endpoint,
                    count,
                    errors,
                    totalTime,
                    count > 0 ? totalTime / count : 0,
                    maxResponseTime.get(),
                    minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get(),
                    count > 0 ? (double) errors / count : 0.0
            );
        }
    }
}
