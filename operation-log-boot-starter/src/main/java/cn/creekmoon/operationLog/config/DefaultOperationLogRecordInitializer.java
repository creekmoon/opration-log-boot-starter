package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.operationLogRecordInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnMissingBean({operationLogRecordInitializer.class})
@Slf4j
public class DefaultOperationLogRecordInitializer implements operationLogRecordInitializer {

    public static final String UNKNOWN_STRING = "unknown";
    public static final Long UNKNOWN_LONG = -1L;
    @Override
    public LogRecord createNewLogRecord() {
        /*初始化LogRecord*/
        LogRecord logRecord = new LogRecord();
        logRecord.setOrgId(UNKNOWN_LONG);
        logRecord.setUserId(UNKNOWN_LONG);
        logRecord.setUserName(UNKNOWN_STRING);
        logRecord.setProjectName(UNKNOWN_STRING);
        return logRecord;
    }



}
