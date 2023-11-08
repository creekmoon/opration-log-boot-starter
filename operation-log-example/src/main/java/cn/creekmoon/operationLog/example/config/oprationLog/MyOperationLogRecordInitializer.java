package cn.creekmoon.operationLog.example.config.oprationLog;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.OperationLogRecordInitializer;
import org.springframework.stereotype.Component;

@Component
public class MyOperationLogRecordInitializer implements OperationLogRecordInitializer {
    @Override
    public LogRecord createNewLogRecord() {
        return new LogRecord();
    }


    @Override
    public void postProcess(LogRecord currentLogRecord, Object returnValue) {
        if (returnValue instanceof String) {
            if ("error but success".equals((String) returnValue)) {
                currentLogRecord.setRequestResult(false);
            }
        }
    }
}
