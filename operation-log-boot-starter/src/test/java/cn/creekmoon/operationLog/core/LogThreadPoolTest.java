package cn.creekmoon.operationLog.core;

import cn.creekmoon.operationLog.config.OperationLogProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogThreadPool 单元测试
 */
class LogThreadPoolTest {

    @BeforeEach
    void setUp() {
        // 重置初始化状态
        resetLogThreadPool();
    }

    @AfterEach
    void tearDown() {
        // 确保线程池关闭
        LogThreadPool.shutdown();
        // 重置初始化状态
        resetLogThreadPool();
    }

    /**
     * 通过反射重置 LogThreadPool 的静态状态
     */
    private void resetLogThreadPool() {
        ReflectionTestUtils.setField(LogThreadPool.class, "service", null);
        ReflectionTestUtils.setField(LogThreadPool.class, "initialized", false);
    }

    @Test
    void testInitializeWithDefaultConfig() {
        // 使用默认配置初始化
        LogThreadPool.initialize();

        // 验证已初始化
        assertTrue(LogThreadPool.isInitialized());
        assertNotNull(LogThreadPool.getService());
    }

    @Test
    void testInitializeWithCustomConfig() {
        // 创建自定义配置
        OperationLogProperties properties = new OperationLogProperties();
        OperationLogProperties.ThreadPoolProperties threadPool = properties.getThreadPool();
        threadPool.setCoreSize(2);
        threadPool.setMaxSize(8);
        threadPool.setQueueCapacity(256);
        threadPool.setKeepAliveSeconds(120);

        // 使用自定义配置初始化
        LogThreadPool.initialize(properties);

        // 验证已初始化
        assertTrue(LogThreadPool.isInitialized());
        assertNotNull(LogThreadPool.getService());

        // 验证线程池参数
        ThreadPoolExecutor executor = (ThreadPoolExecutor) LogThreadPool.getService();
        assertEquals(2, executor.getCorePoolSize());
        assertEquals(8, executor.getMaximumPoolSize());
        assertEquals(120, executor.getKeepAliveTime(TimeUnit.SECONDS));
    }

    @Test
    void testDefaultThreadPoolProperties() {
        // 验证默认配置值
        OperationLogProperties properties = new OperationLogProperties();
        OperationLogProperties.ThreadPoolProperties threadPool = properties.getThreadPool();

        assertEquals(0, threadPool.getCoreSize());
        assertEquals(4, threadPool.getMaxSize());
        assertEquals(512, threadPool.getQueueCapacity());
        assertEquals(60, threadPool.getKeepAliveSeconds());
    }

    @Test
    void testRunTask() throws InterruptedException {
        // 初始化线程池
        LogThreadPool.initialize();

        // 提交任务
        AtomicInteger counter = new AtomicInteger(0);
        LogThreadPool.runTask(counter::incrementAndGet);
        LogThreadPool.runTask(counter::incrementAndGet);
        LogThreadPool.runTask(counter::incrementAndGet);

        // 等待任务完成
        ExecutorService service = LogThreadPool.getService();
        service.shutdown();
        boolean terminated = service.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(terminated, "线程池应在规定时间内完成所有任务");
        assertEquals(3, counter.get(), "所有任务应被执行");
    }

    @Test
    void testDoubleInitialization() {
        // 第一次初始化
        OperationLogProperties properties1 = new OperationLogProperties();
        properties1.getThreadPool().setCoreSize(2);
        LogThreadPool.initialize(properties1);

        // 第二次初始化（应被忽略）
        OperationLogProperties properties2 = new OperationLogProperties();
        properties2.getThreadPool().setCoreSize(10);
        LogThreadPool.initialize(properties2);

        // 验证第一次配置仍然有效
        ThreadPoolExecutor executor = (ThreadPoolExecutor) LogThreadPool.getService();
        assertEquals(2, executor.getCorePoolSize());
    }

    @Test
    void testShutdown() {
        // 初始化线程池
        LogThreadPool.initialize();
        ExecutorService service = LogThreadPool.getService();

        assertNotNull(service);
        assertFalse(service.isShutdown());

        // 关闭线程池
        LogThreadPool.shutdown();

        assertTrue(service.isShutdown());
    }

    @Test
    void testLazyInitialization() {
        // 验证延迟初始化
        assertFalse(LogThreadPool.isInitialized());

        // 提交任务触发初始化
        LogThreadPool.runTask(() -> {});

        // 验证已初始化
        assertTrue(LogThreadPool.isInitialized());
    }
}
