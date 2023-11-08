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
            1,
            4,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(512),
            namedThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy());

    public static void runTask( Runnable runnable) {
        service.submit(runnable);
    }


}
