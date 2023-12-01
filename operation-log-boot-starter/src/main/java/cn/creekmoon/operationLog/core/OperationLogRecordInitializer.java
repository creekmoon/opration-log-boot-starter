package cn.creekmoon.operationLog.core;

import java.util.function.Function;

/**
 * 日志提供器
 */
public interface OperationLogRecordInitializer {

    LogRecord init(LogRecord newLogRecord);



    /**
     * 注解所在的方法执行后置处理
     *
     * @param currentLogRecord 当前的日志对象
     * @param returnValue      当前注解所在的方法返回值
     */
    default void functionPostProcess(LogRecord currentLogRecord, Object returnValue) {
            // 默认不做任何处理
    }


}
