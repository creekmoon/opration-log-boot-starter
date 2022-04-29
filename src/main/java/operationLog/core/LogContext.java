package operationLog.core;


import lombok.extern.slf4j.Slf4j;


import javax.servlet.ServletRequest;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志上下文
 * 每一个 request都会独占一个线程.
 * ThreadLocal记录下当前请求的信息 使用过后需要及时清理
 *
 * @author JY
 */
@Slf4j
public class LogContext {
    /*当前是否处于禁用状态*/
    static boolean disable = true;
    /*当前请求*/
    protected static ThreadLocal<ServletRequest> currentServletRequest = new ThreadLocal<>();
    /*跟踪的元数据*/
    protected static ThreadLocal<Callable<Object>> metadataSupplier = new ThreadLocal<>();
    protected static ConcurrentHashMap<ServletRequest, LogRecord> request2Logs = new ConcurrentHashMap(1024);

    /**
     * 传入一个获取数据的方式,会通过这个方式监控数据变化 体现在effectFields字段中
     *
     * @param metadata 元数据,传入需要监控的对象
     */
    public static void follow(Callable<Object> metadata) {
        if (disable) {
            return;
        }
        ServletRequest servletRequest = currentServletRequest.get();
        if (servletRequest == null) {
            log.error("[日志推送]获取当前ServletRequest失败! ");
            return;
        }
        LogRecord record = request2Logs.get(servletRequest);
        if (record == null) {
            log.error("[日志推送]获取日志上下文失败! 请检查是否添加了@OptLog注解!");
            return;
        }
        metadataSupplier.set(metadata);
        try {
            if (metadata != null) {
                record.setPreValue(metadata.call());
            }
        } catch (Exception e) {
            log.warn("[日志推送]跟踪日志对象时报错! 发生位置setPreValue");
        }
    }

    /**
     * 增加标记点
     * 标记点是一个字符串,一个日志可以有多个标记点. 日志可以通过相同的标记点作为查询条件,快速定位对象
     *
     * @param markPoints
     */
    public static void addMarkPoints(String... markPoints) {
        if (disable || markPoints == null) {
            return;
        }
        ServletRequest servletRequest = currentServletRequest.get();
        LogRecord record = request2Logs.get(servletRequest);
        if (record == null) {
            log.error("[日志推送]获取日志上下文失败! 请检查是否添加了@OptLog注解!");
            return;
        }
        for (String markPoint : markPoints) {
            if (markPoint != null) {
                record.getMarkPoints().add(markPoint.trim());
            }
        }
    }
}
