package cn.jy.operationLog.example.config.oprationLog;

import cn.jy.operationLog.core.LogRecord;
import cn.jy.operationLog.core.OperationLogHandler;
import org.springframework.stereotype.Component;

@Component
public class MyOprationLogHandler implements OperationLogHandler {
    @Override
    public void handle(LogRecord logRecord) {
        System.out.println("logRecord = " + logRecord);
    }
}
