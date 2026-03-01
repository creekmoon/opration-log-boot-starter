package cn.creekmoon.operationLog.core;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 内存版统一指标收集器实现
 * 
 * 特点：
 * - 零外部依赖
 * - 高性能本地内存操作
 * - 适用于单实例部署或降级模式
 * 
 * @author CodeSmith
 * @since 1.0.0
 */
@Slf4j
public class MemoryMetricsCollector implements UnifiedMetricsCollector {

    /**
     * 端点指标数据（内部实现）
     */
    public static class EndpointMetrics {
        private final String endpoint;
        private final LongAdder totalCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder totalResponseTime = new LongAdder();
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        
        // 响应时间滑动窗口
        private final long[] responseTimeWindow = new long[10000];
        private final AtomicLong windowIndex = new AtomicLong(0);
        private final ReentrantReadWriteLock windowLock = new ReentrantReadWriteLock();

        public EndpointMetrics(String endpoint) {
            this.endpoint = endpoint;
        }

        public void recordResponseTime(long millis) {
            totalCount.increment();
            totalResponseTime.add(millis);
            maxResponseTime.updateAndGet(current -> Math.max(current, millis));
            minResponseTime.updateAndGet(current -> Math.min(current, millis));
            
            windowLock.writeLock().lock();
            try {
                int index = (int) (windowIndex.getAndIncrement() % responseTimeWindow.length);
                responseTimeWindow[index] = millis;
            } finally {
                windowLock.writeLock().unlock();
            }
        }

        public void recordError() {
            errorCount.increment();
        }

        public long getTotalCount() {
            return totalCount.sum();
        }

        public long getErrorCount() {
            return errorCount.sum();
        }

        public long getTotalResponseTime() {
            return totalResponseTime.sum();
        }

        public long getAvgResponseTime() {
            long total = totalCount.sum();
            return total > 0 ? totalResponseTime.sum() / total : 0;
        }

        public long getMaxResponseTime() {
            return maxResponseTime.get();
        }

        public long getMinResponseTime() {
            return minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get();
        }

        public double getErrorRate() {
            long total = totalCount.sum();
            long errors = errorCount.sum();
            return total > 0 ? (double) errors / total : 0.0;
        }
    }

    // ==================== 全局指标存储 ====================
    private final ConcurrentHashMap<String, EndpointMetrics> endpointMetricsMap = new ConcurrentHashMap<>();
    private final AtomicLong concurrentRequests = new AtomicLong(0);
    private final AtomicLong peakConcurrentRequests = new AtomicLong(0);
    private final LongAdder totalRequests = new LongAdder();
    
    // QPS 计算 (1秒滑动窗口)
    private final long[] qpsWindow = new long[60];
    private final AtomicLong qpsWindowIndex = new AtomicLong(0);

    @Override
    public void requestStarted() {
        long current = concurrentRequests.incrementAndGet();
        peakConcurrentRequests.updateAndGet(peak -> Math.max(peak, current));
        totalRequests.increment();
        
        long currentSecond = System.currentTimeMillis() / 1000;
        int index = (int) (currentSecond % qpsWindow.length);
        int currentIndex = (int) (qpsWindowIndex.get() % qpsWindow.length);
        
        if (index != currentIndex) {
            qpsWindowIndex.set(currentSecond);
            qpsWindow[index] = 0;
        }
        qpsWindow[index]++;
    }

    @Override
    public void requestEnded() {
        concurrentRequests.decrementAndGet();
    }

    @Override
    public void record(String endpoint, long responseTimeMillis) {
        EndpointMetrics metrics = endpointMetricsMap.computeIfAbsent(endpoint, EndpointMetrics::new);
        metrics.recordResponseTime(responseTimeMillis);
        totalRequests.increment();
    }

    @Override
    public void recordError(String endpoint) {
        EndpointMetrics metrics = endpointMetricsMap.computeIfAbsent(endpoint, EndpointMetrics::new);
        metrics.recordError();
    }

    @Override
    public long getCurrentConcurrentRequests() {
        return concurrentRequests.get();
    }

    @Override
    public long getPeakConcurrentRequests() {
        return peakConcurrentRequests.get();
    }

    @Override
    public long getTotalRequests() {
        return totalRequests.sum();
    }

    @Override
    public double getCurrentQps() {
        long currentSecond = System.currentTimeMillis() / 1000;
        int index = (int) (currentSecond % qpsWindow.length);
        return qpsWindow[index];
    }

    @Override
    public double getAvgQps() {
        long sum = 0;
        int count = 0;
        for (long qps : qpsWindow) {
            if (qps > 0) {
                sum += qps;
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0;
    }

    @Override
    public double getGlobalErrorRate() {
        long totalErrors = endpointMetricsMap.values().stream()
                .mapToLong(EndpointMetrics::getErrorCount)
                .sum();
        long total = totalRequests.sum();
        return total > 0 ? (double) totalErrors / total : 0;
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getAllEndpointMetrics() {
        Map<String, EndpointMetricsSnapshot> result = new HashMap<>();
        endpointMetricsMap.forEach((key, metrics) -> {
            result.put(key, toSnapshot(key, metrics));
        });
        return result;
    }

    @Override
    public EndpointMetricsSnapshot getEndpointMetrics(String endpoint) {
        EndpointMetrics metrics = endpointMetricsMap.get(endpoint);
        if (metrics == null) {
            return EndpointMetricsSnapshot.empty(endpoint);
        }
        return toSnapshot(endpoint, metrics);
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getSlowestEndpoints(int limit) {
        return endpointMetricsMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(
                        e2.getValue().getAvgResponseTime(), 
                        e1.getValue().getAvgResponseTime()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> toSnapshot(e.getKey(), e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Map<String, EndpointMetricsSnapshot> getErrorEndpoints(int limit) {
        return endpointMetricsMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(
                        e2.getValue().getErrorRate(), 
                        e1.getValue().getErrorRate()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> toSnapshot(e.getKey(), e.getValue()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private EndpointMetricsSnapshot toSnapshot(String endpoint, EndpointMetrics metrics) {
        return new EndpointMetricsSnapshot(
                endpoint,
                metrics.getTotalCount(),
                metrics.getErrorCount(),
                metrics.getTotalResponseTime(),
                metrics.getAvgResponseTime(),
                metrics.getMaxResponseTime(),
                metrics.getMinResponseTime(),
                metrics.getErrorRate()
        );
    }

    /**
     * 重置所有指标（测试用）
     */
    public void reset() {
        endpointMetricsMap.clear();
        concurrentRequests.set(0);
        peakConcurrentRequests.set(0);
        totalRequests.reset();
        Arrays.fill(qpsWindow, 0);
        qpsWindowIndex.set(0);
    }
}
