package cn.creekmoon.operationLog.core;

import cn.creekmoon.operationLog.config.OperationLogProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.*;

/**
 * 专用于后置处理的线程池
 */
@Slf4j
public class LogThreadPool {

    private static ThreadFactory namedThreadFactory;
    private static volatile ExecutorService service;
    private static volatile boolean initialized = false;
    private static final Object lock = new Object();

    static {
        namedThreadFactory = new CustomizableThreadFactory("operation-logs-thread");
    }

    /**
     * 初始化线程池（使用默认配置）
     */
    public static void initialize() {
        initialize(new OperationLogProperties());
    }

    /**
     * 初始化线程池（使用指定配置）
     *
     * @param properties 操作日志配置属性
     */
    public static void initialize(OperationLogProperties properties) {
        if (initialized) {
            log.debug("LogThreadPool 已经初始化，跳过重复初始化");
            return;
        }
        synchronized (lock) {
            if (initialized) {
                return;
            }
            OperationLogProperties.ThreadPoolProperties poolProps = properties.getThreadPool();
            
            log.info("初始化 LogThreadPool: coreSize={}, maxSize={}, queueCapacity={}, keepAliveSeconds={}",
                    poolProps.getCoreSize(), poolProps.getMaxSize(), 
                    poolProps.getQueueCapacity(), poolProps.getKeepAliveSeconds());
            
            service = new ThreadPoolExecutor(
                    poolProps.getCoreSize(),
                    poolProps.getMaxSize(),
                    poolProps.getKeepAliveSeconds(),
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(poolProps.getQueueCapacity()),
                    namedThreadFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy());
            
            initialized = true;
            
            // 添加 JVM 关闭钩子，确保优雅关闭
            addShutdownHook();
        }
    }

    /**
     * 添加 JVM 关闭钩子
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM 关闭钩子触发，开始关闭 LogThreadPool...");
            shutdown();
        }));
    }

    public static void runTask(Runnable runnable) {
        // 延迟初始化（使用默认配置）
        if (!initialized) {
            initialize();
        }
        service.submit(runnable);
    }

    /**
     * 优雅关闭线程池
     */
    public static void shutdown() {
        synchronized (lock) {
            if (service != null && !service.isShutdown()) {
                log.info("正在关闭 LogThreadPool 线程池...");
                service.shutdown();
                try {
                    // 等待最多 60 秒让现有任务完成
                    if (!service.awaitTermination(60, TimeUnit.SECONDS)) {
                        log.warn("LogThreadPool 线程池未能在 60 秒内完成所有任务，强制关闭...");
                        service.shutdownNow();
                    } else {
                        log.info("LogThreadPool 线程池已优雅关闭");
                    }
                } catch (InterruptedException e) {
                    log.error("关闭 LogThreadPool 线程池时被中断", e);
                    service.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 检查线程池是否已初始化
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 获取当前线程池（用于测试和监控）
     */
    public static ExecutorService getService() {
        return service;
    }
}
