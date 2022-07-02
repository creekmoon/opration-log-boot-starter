package cn.creekmoon.operationLog.example.config.oprationLog;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.OperationLogHandler;
import org.springframework.stereotype.Component;

@Component
public class MyOprationLogHandler implements OperationLogHandler {
    @Override
    public void handle(LogRecord logRecord) {
        System.out.println("logRecord = " + logRecord);
    }
}
