package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.OperationLogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean({OperationLogHandler.class})
@Slf4j
public class DefaultOperationLogHandler implements OperationLogHandler {


    @Override
    public void handle(LogRecord logRecord) {
        log.info("[operation-log]" + logRecord.toString());
    }


}
