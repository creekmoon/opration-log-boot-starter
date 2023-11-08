package cn.creekmoon.operationLog.core;

/**
 * 实现这个接口,需要保证线程安全!
 */
public interface OperationLogHandler {


    /**
     * 定义如何处理这个日志记录
     *
     * @param logRecord
     */
    void handle(LogRecord logRecord);


}
