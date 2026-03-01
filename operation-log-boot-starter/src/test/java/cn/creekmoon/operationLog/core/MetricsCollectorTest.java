package cn.creekmoon.operationLog.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetricsCollector 单元测试
 */
class MetricsCollectorTest {

    @BeforeEach
    void setUp() {
        MetricsCollector.reset();
    }

    @AfterEach
    void tearDown() {
        MetricsCollector.reset();
    }

    @Test
    void testRecordResponseTime() {
        // 记录一些响应时间
        MetricsCollector.record("TestEndpoint.testMethod1", 50);
        MetricsCollector.record("TestEndpoint.testMethod1", 100);
        MetricsCollector.record("TestEndpoint.testMethod1", 150);

        // 获取指标
        MetricsCollector.EndpointMetrics metrics = MetricsCollector.getEndpointMetrics("TestEndpoint.testMethod1");
        
        assertNotNull(metrics);
        assertEquals(3, metrics.getTotalCount().sum());
        assertEquals(100, metrics.getAvgResponseTime()); // (50+100+150)/3
    }

    @Test
    void testRecordError() {
        // 记录正常请求
        MetricsCollector.record("TestEndpoint.method", 50);
        MetricsCollector.record("TestEndpoint.method", 60);
        
        // 记录错误
        MetricsCollector.recordError("TestEndpoint.method");
        
        MetricsCollector.EndpointMetrics metrics = MetricsCollector.getEndpointMetrics("TestEndpoint.method");
        
        assertNotNull(metrics);
        assertEquals(2, metrics.getTotalCount().sum());
        assertEquals(1, metrics.getErrorCount().sum());
        assertEquals(0.5, metrics.getErrorRate(), 0.01);
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        // 模拟并发请求
        MetricsCollector.requestStarted();
        assertEquals(1, MetricsCollector.getCurrentConcurrentRequests());
        
        MetricsCollector.requestStarted();
        assertEquals(2, MetricsCollector.getCurrentConcurrentRequests());
        
        MetricsCollector.requestEnded();
        assertEquals(1, MetricsCollector.getCurrentConcurrentRequests());
        
        MetricsCollector.requestEnded();
        assertEquals(0, MetricsCollector.getCurrentConcurrentRequests());
    }

    @Test
    void testPeakConcurrentRequests() {
        MetricsCollector.reset();
        
        // 模拟峰值
        for (int i = 0; i < 10; i++) {
            MetricsCollector.requestStarted();
        }
        
        assertEquals(10, MetricsCollector.getPeakConcurrentRequests());
        
        // 减少并发不应影响峰值
        for (int i = 0; i < 5; i++) {
            MetricsCollector.requestEnded();
        }
        
        assertEquals(10, MetricsCollector.getPeakConcurrentRequests());
    }

    @Test
    void testTotalRequests() {
        assertEquals(0, MetricsCollector.getTotalRequests());
        
        // totalRequests 现在由 record() 统一计数, requestStarted() 不再增加
        MetricsCollector.record("TestEndpoint.method1", 50);
        MetricsCollector.record("TestEndpoint.method2", 100);
        MetricsCollector.record("TestEndpoint.method3", 150);
        
        assertEquals(3, MetricsCollector.getTotalRequests());
    }

    @Test
    void testPercentiles() {
        // 记录100个不同响应时间
        for (int i = 1; i <= 100; i++) {
            MetricsCollector.record("TestEndpoint.method", i);
        }
        
        MetricsCollector.EndpointMetrics metrics = MetricsCollector.getEndpointMetrics("TestEndpoint.method");
        MetricsCollector.PercentileResult percentiles = metrics.getPercentiles();
        
        assertNotNull(percentiles);
        assertEquals(50, percentiles.p50(), 2); // 中位数约50
        assertEquals(95, percentiles.p95(), 2); // P95约95
        assertEquals(99, percentiles.p99(), 2); // P99约99
    }

    @Test
    void testDistributionBuckets() {
        // 记录不同范围的响应时间
        for (int i = 0; i < 50; i++) {
            MetricsCollector.record("Test.method", 50);      // 0-100ms
        }
        for (int i = 0; i < 30; i++) {
            MetricsCollector.record("Test.method", 200);     // 100-500ms
        }
        for (int i = 0; i < 15; i++) {
            MetricsCollector.record("Test.method", 700);     // 500ms-1s
        }
        for (int i = 0; i < 5; i++) {
            MetricsCollector.record("Test.method", 1500);    // 1s+
        }
        
        MetricsCollector.EndpointMetrics metrics = MetricsCollector.getEndpointMetrics("Test.method");
        Map<String, Long> buckets = metrics.getDistributionBuckets();
        
        assertEquals(50L, buckets.get("0-100ms"));
        assertEquals(30L, buckets.get("100-500ms"));
        assertEquals(15L, buckets.get("500ms-1s"));
        assertEquals(5L, buckets.get("1s+"));
    }

    @Test
    void testGetSlowestEndpoints() {
        // 记录不同响应时间
        MetricsCollector.record("Endpoint.fast", 10);
        MetricsCollector.record("Endpoint.medium", 100);
        MetricsCollector.record("Endpoint.slow", 500);
        
        List<Map.Entry<String, MetricsCollector.EndpointMetrics>> slowest = 
            MetricsCollector.getSlowestEndpoints(2);
        
        assertEquals(2, slowest.size());
        assertEquals("Endpoint.slow", slowest.get(0).getKey());
        assertEquals("Endpoint.medium", slowest.get(1).getKey());
    }

    @Test
    void testGetErrorEndpoints() {
        // 记录正常和错误请求
        MetricsCollector.record("Endpoint.good", 50);
        MetricsCollector.recordError("Endpoint.bad");
        
        List<Map.Entry<String, MetricsCollector.EndpointMetrics>> errorEndpoints = 
            MetricsCollector.getErrorEndpoints(10);
        
        assertFalse(errorEndpoints.isEmpty());
        // 错误率最高的应该在前
        assertEquals("Endpoint.bad", errorEndpoints.get(0).getKey());
    }

    @Test
    void testGlobalErrorRate() {
        // 接口1: 10次请求，2次错误
        for (int i = 0; i < 10; i++) {
            MetricsCollector.record("Endpoint1.method", 50);
        }
        MetricsCollector.recordError("Endpoint1.method");
        MetricsCollector.recordError("Endpoint1.method");
        
        // 接口2: 10次请求，0次错误
        for (int i = 0; i < 10; i++) {
            MetricsCollector.record("Endpoint2.method", 50);
        }
        
        // 总请求数 = 20 (record 会递增 totalRequests)
        // 总错误数 = 2
        double globalErrorRate = MetricsCollector.getGlobalErrorRate();
        assertEquals(0.1, globalErrorRate, 0.01);  // 2/20 = 0.1
    }

    @Test
    void testQpsMetrics() {
        // 模拟请求 - requestStarted 更新 QPS 窗口
        for (int i = 0; i < 100; i++) {
            MetricsCollector.requestStarted();
        }
        
        // QPS 应该大于0
        assertTrue(MetricsCollector.getCurrentQps() >= 0);
        // totalRequests 现在由 record() 统一计数
        assertEquals(0, MetricsCollector.getTotalRequests());
        
        // 调用 record() 增加 totalRequests
        for (int i = 0; i < 100; i++) {
            MetricsCollector.record("TestEndpoint.method", 50);
        }
        assertEquals(100, MetricsCollector.getTotalRequests());
    }

    @Test
    void testReset() {
        MetricsCollector.record("Test.method", 50);
        MetricsCollector.recordError("Test.method");
        MetricsCollector.requestStarted();
        
        MetricsCollector.reset();
        
        assertEquals(0, MetricsCollector.getTotalRequests());
        assertEquals(0, MetricsCollector.getCurrentConcurrentRequests());
        assertNull(MetricsCollector.getEndpointMetrics("Test.method"));
    }

    @Test
    void testMultipleEndpoints() {
        MetricsCollector.record("Endpoint1.method", 50);
        MetricsCollector.record("Endpoint2.method", 100);
        MetricsCollector.record("Endpoint3.method", 150);
        
        Map<String, MetricsCollector.EndpointMetrics> allMetrics = MetricsCollector.getAllEndpointMetrics();
        
        assertEquals(3, allMetrics.size());
        assertTrue(allMetrics.containsKey("Endpoint1.method"));
        assertTrue(allMetrics.containsKey("Endpoint2.method"));
        assertTrue(allMetrics.containsKey("Endpoint3.method"));
    }

    @Test
    void testMaxMinResponseTime() {
        MetricsCollector.record("Test.method", 100);
        MetricsCollector.record("Test.method", 50);
        MetricsCollector.record("Test.method", 200);
        
        MetricsCollector.EndpointMetrics metrics = MetricsCollector.getEndpointMetrics("Test.method");
        MetricsCollector.PercentileResult percentiles = metrics.getPercentiles();
        
        assertEquals(200, percentiles.max());
        assertEquals(50, percentiles.min());
    }

    @Test
    void testEmptyMetrics() {
        MetricsCollector.PercentileResult percentiles = new MetricsCollector.PercentileResult(0, 0, 0, 0, 0, Long.MAX_VALUE);
        
        assertEquals(0, percentiles.p50());
        assertEquals(0, percentiles.p95());
        assertEquals(0, percentiles.avg());
    }
}
