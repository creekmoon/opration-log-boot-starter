package cn.jy.operationLog.core;


import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;


/**
 * 专用于后置处理的线程池
 */
@Slf4j
public class LogThreadPool {

    private static ThreadFactory namedThreadFactory;

    static {
        ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder().setNamePrefix("logs-thread");
        threadFactoryBuilder.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            log.error("操作日志记录线程出错!", e);
        });
        namedThreadFactory = threadFactoryBuilder.build();
    }

    private static ExecutorService service1 = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(768),
            namedThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy());
    private static ExecutorService service2 = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(768),
            namedThreadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy());

    public static void runTask(Long primaryId, Runnable r) {
        if (primaryId == null) {
            return;
        }
        /* 取余数决定当前任务去2个线程中的哪一个  因为如果要保证先后顺序 那么相同id必须分配到相同的线程 这里应该是用用户ID作为primaryId*/
        long index = primaryId % 2;
        if (index == 0L) {
            service1.execute(r);
        } else if (index == 1L) {
            service2.execute(r);
        }
    }


}
