package cn.creekmoon.operationLog.example.config.oprationLog;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.OperationLogRecordFactory;
import org.springframework.stereotype.Component;

@Component
public class MyOperationLogRecordFactory implements OperationLogRecordFactory {
    @Override
    public LogRecord createNewLogRecord() {
        return new LogRecord();
    }

    @Override
    public void afterReturn(LogRecord currentLogRecord, Object returnValue) {
        if (returnValue instanceof String) {
            if ("error but success".equals((String) returnValue)) {
                currentLogRecord.setRequestResult(false);
            }
        }
    }
}
