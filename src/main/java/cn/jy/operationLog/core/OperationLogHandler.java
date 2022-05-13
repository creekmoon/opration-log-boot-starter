package cn.jy.operationLog.core;

/**
 * 实现这个接口,需要保证线程安全!
 */
public interface OperationLogHandler {


    /**
     * 定义处理这个日志对象
     *
     * @param logRecord
     */
    void handle(LogRecord logRecord);


}
