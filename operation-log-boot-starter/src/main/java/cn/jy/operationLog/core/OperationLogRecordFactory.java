package cn.jy.operationLog.core;

/**
 * 日志细节提供器
 */
public interface OperationLogRecordFactory {
    LogRecord createNewLogRecord();
}
