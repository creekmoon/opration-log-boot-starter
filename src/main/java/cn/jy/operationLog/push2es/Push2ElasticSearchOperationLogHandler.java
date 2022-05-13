package cn.jy.operationLog.push2es;

import cn.jy.operationLog.core.LogRecord;
import cn.jy.operationLog.core.OperationLogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean({OperationLogHandler.class})
@Slf4j
public class Push2ElasticSearchOperationLogHandler implements OperationLogHandler {
    @Autowired
    OperationLogElasticClient operationLogElasticClient;
    @Override
    public void save(LogRecord logRecord) {
        log.info("operation-log:" + logRecord.toString());
        operationLogElasticClient.save2ElasticSearch(logRecord,2);
    }
}
