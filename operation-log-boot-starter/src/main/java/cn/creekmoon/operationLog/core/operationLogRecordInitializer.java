package cn.creekmoon.operationLog.core;

/**
 * 日志提供器
 */
public interface operationLogRecordInitializer {
    
    LogRecord createNewLogRecord();



    /**
     * 在执行APO切面之后，会再次回调这里，通常用于定义本次方法是否成功,
     *
     * @param currentLogRecord 当前的日志对象
     * @param returnValue      当前注解所在的方法返回值
     */
    default void postProcess(LogRecord currentLogRecord, Object returnValue) {
            // 默认不做任何处理
    }


}
