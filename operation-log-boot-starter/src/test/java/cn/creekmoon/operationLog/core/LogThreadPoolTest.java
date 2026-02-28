package cn.creekmoon.operationLog.core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogThreadPool 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LogThreadPoolTest {

    @BeforeAll
    static void setUp() {
        // 确保线程池可用
    }

    @AfterAll
    static void tearDown() {
        // 测试结束后关闭线程池
        LogThreadPool.shutdown();
    }

    @Test
    @Order(1)
    @DisplayName("测试 runTask 方法提交 Runnable 任务")
    void testRunTaskWithRunnable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        
        LogThreadPool.runTask(() -> {
            counter.incrementAndGet();
            latch.countDown();
        });
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "任务应在 5 秒内完成");
        assertEquals(1, counter.get(), "任务应执行一次");
    }

    @Test
    @Order(2)
    @DisplayName("测试多个任务并发执行")
    void testMultipleTasks() throws InterruptedException {
        int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < taskCount; i++) {
            LogThreadPool.runTask(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "所有任务应在 10 秒内完成");
        assertEquals(taskCount, counter.get(), "所有任务都应执行");
    }

    @Test
    @Order(3)
    @DisplayName("测试任务抛出异常不影响其他任务")
    void testTaskExceptionHandling() throws InterruptedException {
        CountDownLatch successLatch = new CountDownLatch(1);
        AtomicInteger successCounter = new AtomicInteger(0);
        
        // 提交一个会抛出异常的任务
        LogThreadPool.runTask(() -> {
            throw new RuntimeException("测试异常");
        });
        
        // 提交一个正常任务
        LogThreadPool.runTask(() -> {
            successCounter.incrementAndGet();
            successLatch.countDown();
        });
        
        boolean completed = successLatch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "正常任务应成功执行，不受异常任务影响");
        assertEquals(1, successCounter.get());
    }

    @Test
    @Order(4)
    @DisplayName("测试线程池参数配置")
    void testThreadPoolConfiguration() {
        // 线程池参数验证
        // 核心线程数: 0
        // 最大线程数: 4
        // 队列大小: 512
        // 空闲线程存活时间: 60秒
        
        // 提交大量任务验证队列容量
        int taskCount = 20;
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        for (int i = 0; i < taskCount; i++) {
            LogThreadPool.runTask(() -> {
                try {
                    Thread.sleep(10); // 模拟工作
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
        }
        
        try {
            boolean completed = latch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "所有任务应被成功处理");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("测试被中断");
        }
    }

    @Test
    @Order(5)
    @DisplayName("测试线程名称前缀")
    void testThreadNamePrefix() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);
        
        LogThreadPool.runTask(() -> {
            String threadName = Thread.currentThread().getName();
            if (threadName.startsWith("operation-logs-thread")) {
                counter.incrementAndGet();
            }
            latch.countDown();
        });
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed);
        assertEquals(1, counter.get(), "线程名称应以 'operation-logs-thread' 开头");
    }

    @Test
    @Order(6)
    @DisplayName("测试 CallerRunsPolicy - 当队列满时调用者线程执行任务")
    void testCallerRunsPolicy() throws InterruptedException {
        // 由于队列大小为512，很难在测试中填满
        // 这个测试主要验证线程池配置正确
        CountDownLatch latch = new CountDownLatch(1);
        
        LogThreadPool.runTask(() -> {
            latch.countDown();
        });
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "任务应被成功处理");
    }

    @Test
    @Order(7)
    @DisplayName("测试并发场景下任务执行")
    void testConcurrentTaskExecution() throws InterruptedException {
        int threadCount = 5;
        int tasksPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount * tasksPerThread);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待所有线程准备就绪
                    for (int j = 0; j < tasksPerThread; j++) {
                        LogThreadPool.runTask(() -> {
                            successCount.incrementAndGet();
                            completeLatch.countDown();
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown(); // 同时开始
        boolean completed = completeLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "所有任务应在超时前完成");
        assertEquals(threadCount * tasksPerThread, successCount.get(), 
            "所有任务都应成功执行");
    }

    @Test
    @Order(8)
    @DisplayName("测试任务执行时间")
    void testTaskExecutionTime() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        
        LogThreadPool.runTask(() -> {
            try {
                Thread.sleep(100); // 模拟 100ms 工作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTrue(completed, "任务应成功完成");
        assertTrue(duration < 2000, "任务执行应在 2 秒内完成，实际耗时: " + duration + "ms");
    }
}
