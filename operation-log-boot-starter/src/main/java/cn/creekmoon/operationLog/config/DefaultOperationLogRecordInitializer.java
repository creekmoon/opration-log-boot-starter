package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.OperationLogRecordInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnMissingBean({OperationLogRecordInitializer.class})
@Slf4j
public class DefaultOperationLogRecordInitializer implements OperationLogRecordInitializer {

    public static final String UNKNOWN_STRING = "UNKNOWN";
    public static final Long UNKNOWN_LONG = -1L;

    @Override
    public LogRecord init(LogRecord logRecord) {
        /*初始化LogRecord*/
        logRecord.setUserId(UNKNOWN_LONG);
        logRecord.setUserName(UNKNOWN_STRING);
        logRecord.setProjectName(UNKNOWN_STRING);
        return logRecord;
    }

}
