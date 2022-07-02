package cn.jy.operationLog.config;

import cn.jy.operationLog.core.LogRecord;
import cn.jy.operationLog.core.OperationLogHandler;
import cn.jy.operationLog.elasticSupport.Push2ElasticSearchOperationLogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean({OperationLogHandler.class, Push2ElasticSearchOperationLogHandler.class})
@Slf4j
public class DefaultOperationLogHandler implements OperationLogHandler {


    @Override
    public void handle(LogRecord logRecord) {
        log.info("operation-log:" + logRecord.toString());
    }
}
