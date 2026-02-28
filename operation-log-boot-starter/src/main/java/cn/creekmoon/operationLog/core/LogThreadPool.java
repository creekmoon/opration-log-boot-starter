package cn.creekmoon.operationLog.core;



import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.*;


/**
 * 专用于后置处理的线程池
 */
@Slf4j
public class LogThreadPool {

    private static ThreadFactory namedThreadFactory;

    static {
        namedThreadFactory = new CustomizableThreadFactory("operation-logs-thread");
    }


    private static ExecutorService service = new ThreadPoolExecutor(
            0,
            4,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            namedThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy());

    public static void runTask( Runnable runnable) {
        service.submit(runnable);
    }

    /**
     * 优雅关闭线程池
     */
    public static void shutdown() {
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
