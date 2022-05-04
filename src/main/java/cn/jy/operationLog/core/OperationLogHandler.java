package cn.jy.operationLog.core;

/**
 * 实现这个接口,需要保证线程安全!
 */
public interface OperationLogHandler {


    /**
     * 定义http本次请求是否成功  如果成功则记录 不成功则不记录
     *
     * @param functionResult 本次执行的结果
     * @return
     */
    boolean requestIsSuccess(Object functionResult);


    /**
     * 定义如何保存这个日志
     *
     * @param logRecord
     */
    void save(LogRecord logRecord);
}
