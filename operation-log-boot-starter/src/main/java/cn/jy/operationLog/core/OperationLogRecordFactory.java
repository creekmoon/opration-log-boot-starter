package cn.jy.operationLog.core;

/**
 * 日志细节提供器
 */
public interface OperationLogRecordFactory {
    LogRecord createNewLogRecord();


    /**
     * 在执行方法返回之后，会再次回调这里，可以根据返回值进行二次处理日志
     *
     * @param currentLogRecord 当前的日志对象
     * @param returnValue      当前注解所在的方法返回值
     */
    default void afterReturn(LogRecord currentLogRecord, Object returnValue) {
            // 默认不做任何处理
    }


}
