package cn.creekmoon.operationLog.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 系统指标采集器
 * 提供真实响应时间、QPS、并发数、错误率等指标采集
 */
@Slf4j
public class MetricsCollector {

    /**
     * 接口性能数据
     */
    @Getter
    public static class EndpointMetrics {
        private final String endpoint;
        private final LongAdder totalCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder totalResponseTime = new LongAdder();
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        
        // 响应时间滑动窗口 (最近1分钟的样本)
        private final long[] responseTimeWindow = new long[10000];
        private final AtomicLong windowIndex = new AtomicLong(0);
        private final ReentrantReadWriteLock windowLock = new ReentrantReadWriteLock();

        public EndpointMetrics(String endpoint) {
            this.endpoint = endpoint;
        }

        /**
         * 记录响应时间
         */
        public void recordResponseTime(long millis) {
            totalCount.increment();
            totalResponseTime.add(millis);
            
            // 更新最大/最小值
            maxResponseTime.updateAndGet(current -> Math.max(current, millis));
            minResponseTime.updateAndGet(current -> Math.min(current, millis));
            
            // 存入滑动窗口
            windowLock.writeLock().lock();
            try {
                int index = (int) (windowIndex.getAndIncrement() % responseTimeWindow.length);
                responseTimeWindow[index] = millis;
            } finally {
                windowLock.writeLock().unlock();
            }
        }

        /**
         * 记录错误
         */
        public void recordError() {
            errorCount.increment();
        }

        /**
         * 获取响应时间分位数 (P50, P95, P99)
         */
        public PercentileResult getPercentiles() {
            windowLock.readLock().lock();
            try {
                long count = Math.min(windowIndex.get(), responseTimeWindow.length);
                if (count == 0) {
                    return new PercentileResult(0, 0, 0, 0, 0, Long.MAX_VALUE);
                }

                // 复制有效数据
                long[] samples = new long[(int) count];
                long startIndex = Math.max(0, windowIndex.get() - responseTimeWindow.length);
                for (int i = 0; i < count; i++) {
                    long idx = (startIndex + i) % responseTimeWindow.length;
                    samples[i] = responseTimeWindow[(int) idx];
                }

                // 排序计算分位数
                Arrays.sort(samples);
                
                long p50 = samples[(int) (count * 0.5)];
                long p95 = samples[(int) (count * 0.95)];
                long p99 = samples[(int) Math.min(count * 0.99, count - 1)];
                long avg = totalCount.sum() > 0 ? totalResponseTime.sum() / totalCount.sum() : 0;
                long max = maxResponseTime.get();
                long min = minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get();

                return new PercentileResult(p50, p95, p99, avg, max, min);
            } finally {
                windowLock.readLock().unlock();
            }
        }

        /**
         * 获取响应时间分布桶
         */
        public Map<String, Long> getDistributionBuckets() {
            windowLock.readLock().lock();
            try {
                long count = Math.min(windowIndex.get(), responseTimeWindow.length);
                Map<String, Long> buckets = new LinkedHashMap<>();
                buckets.put("0-100ms", 0L);
                buckets.put("100-500ms", 0L);
                buckets.put("500ms-1s", 0L);
                buckets.put("1s+", 0L);

                if (count == 0) {
                    return buckets;
                }

                long startIndex = Math.max(0, windowIndex.get() - responseTimeWindow.length);
                for (int i = 0; i < count; i++) {
                    long idx = (startIndex + i) % responseTimeWindow.length;
                    long time = responseTimeWindow[(int) idx];
                    
                    if (time < 100) {
                        buckets.put("0-100ms", buckets.get("0-100ms") + 1);
                    } else if (time < 500) {
                        buckets.put("100-500ms", buckets.get("100-500ms") + 1);
                    } else if (time < 1000) {
                        buckets.put("500ms-1s", buckets.get("500ms-1s") + 1);
                    } else {
                        buckets.put("1s+", buckets.get("1s+") + 1);
                    }
                }
                return buckets;
            } finally {
                windowLock.readLock().unlock();
            }
        }

        /**
         * 获取错误率
         */
        public double getErrorRate() {
            long total = totalCount.sum();
            long errors = errorCount.sum();
            return total > 0 ? (double) errors / total : 0.0;
        }

        /**
         * 获取平均响应时间
         */
        public long getAvgResponseTime() {
            long total = totalCount.sum();
            return total > 0 ? totalResponseTime.sum() / total : 0;
        }
    }

    /**
     * 分位数结果
     */
    public record PercentileResult(
            long p50,
            long p95,
            long p99,
            long avg,
            long max,
            long min
    ) {}

    // ==================== 全局指标存储 ====================
    
    private static final ConcurrentHashMap<String, EndpointMetrics> endpointMetricsMap = new ConcurrentHashMap<>();
    private static final AtomicLong concurrentRequests = new AtomicLong(0);
    private static final AtomicLong peakConcurrentRequests = new AtomicLong(0);
    private static final LongAdder totalRequests = new LongAdder();
    
    // QPS 计算 (1秒滑动窗口)
    private static final long[] qpsWindow = new long[60]; // 60秒窗口
    private static final AtomicLong qpsWindowIndex = new AtomicLong(0);

    // ==================== 核心 API ====================

    /**
     * 记录请求开始 (增加并发计数)
     * 注意: totalRequests 的计数在 record() 方法中统一处理, 避免重复计数
     */
    public static void requestStarted() {
        long current = concurrentRequests.incrementAndGet();
        peakConcurrentRequests.updateAndGet(peak -> Math.max(peak, current));
        // 注意: 不在此处增加 totalRequests, 由 record() 方法统一计数
        
        // 记录到当前秒的 QPS 窗口
        long currentSecond = System.currentTimeMillis() / 1000;
        int index = (int) (currentSecond % qpsWindow.length);
        int currentIndex = (int) (qpsWindowIndex.get() % qpsWindow.length);
        
        // 如果是新的秒，更新窗口索引
        if (index != currentIndex) {
            qpsWindowIndex.set(currentSecond);
            qpsWindow[index] = 0;
        }
        qpsWindow[index]++;
    }

    /**
     * 记录请求结束 (减少并发计数)
     */
    public static void requestEnded() {
        concurrentRequests.decrementAndGet();
    }

    /**
     * 记录接口响应时间
     */
    public static void record(String endpoint, long responseTimeMillis) {
        EndpointMetrics metrics = endpointMetricsMap.computeIfAbsent(endpoint, EndpointMetrics::new);
        metrics.recordResponseTime(responseTimeMillis);
        totalRequests.increment();
    }

    /**
     * 记录接口错误
     */
    public static void recordError(String endpoint) {
        EndpointMetrics metrics = endpointMetricsMap.computeIfAbsent(endpoint, EndpointMetrics::new);
        metrics.recordError();
    }

    /**
     * 获取当前并发数
     */
    public static long getCurrentConcurrentRequests() {
        return concurrentRequests.get();
    }

    /**
     * 获取峰值并发数
     */
    public static long getPeakConcurrentRequests() {
        return peakConcurrentRequests.get();
    }

    /**
     * 获取总请求数
     */
    public static long getTotalRequests() {
        return totalRequests.sum();
    }

    /**
     * 获取实时 QPS (最近1秒)
     */
    public static double getCurrentQps() {
        long currentSecond = System.currentTimeMillis() / 1000;
        int index = (int) (currentSecond % qpsWindow.length);
        return qpsWindow[index];
    }

    /**
     * 获取平均 QPS (最近60秒)
     */
    public static double getAvgQps() {
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

    /**
     * 获取所有接口的指标
     */
    public static Map<String, EndpointMetrics> getAllEndpointMetrics() {
        return new HashMap<>(endpointMetricsMap);
    }

    /**
     * 获取指定接口的指标
     */
    public static EndpointMetrics getEndpointMetrics(String endpoint) {
        return endpointMetricsMap.get(endpoint);
    }

    /**
     * 获取性能最差的接口 (按平均响应时间排序)
     */
    public static List<Map.Entry<String, EndpointMetrics>> getSlowestEndpoints(int limit) {
        return endpointMetricsMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue().getAvgResponseTime(), e1.getValue().getAvgResponseTime()))
                .limit(limit)
                .toList();
    }

    /**
     * 获取错误最多的接口 (按错误率排序)
     */
    public static List<Map.Entry<String, EndpointMetrics>> getErrorEndpoints(int limit) {
        return endpointMetricsMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().getErrorRate(), e1.getValue().getErrorRate()))
                .limit(limit)
                .toList();
    }

    /**
     * 获取全局错误率
     */
    public static double getGlobalErrorRate() {
        long totalErrors = endpointMetricsMap.values().stream()
                .mapToLong(m -> m.errorCount.sum())
                .sum();
        long total = totalRequests.sum();
        return total > 0 ? (double) totalErrors / total : 0;
    }

    /**
     * 重置所有指标 (主要用于测试)
     */
    public static void reset() {
        endpointMetricsMap.clear();
        concurrentRequests.set(0);
        peakConcurrentRequests.set(0);
        totalRequests.reset();
        Arrays.fill(qpsWindow, 0);
        qpsWindowIndex.set(0);
    }
}
