package cn.jy.operationLog.example.config.oprationLog;

import cn.jy.operationLog.core.LogRecord;
import cn.jy.operationLog.core.OperationLogRecordFactory;
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
