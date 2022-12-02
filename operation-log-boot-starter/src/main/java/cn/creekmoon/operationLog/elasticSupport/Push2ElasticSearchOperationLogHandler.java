package cn.creekmoon.operationLog.elasticSupport;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.OperationLogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Push2ElasticSearchOperationLogHandler implements OperationLogHandler {
    @Autowired
    OperationLogElasticClient operationLogElasticClient;
    @Override
    public void handle(LogRecord logRecord) {
        operationLogElasticClient.save2ElasticSearch(logRecord.toFlatJson(), 2);
    }
}
