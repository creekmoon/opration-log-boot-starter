package cn.jy.operationLog.config;

import cn.jy.operationLog.core.LogRecord;
import cn.jy.operationLog.core.OperationLogHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean({OperationLogHandler.class})
@Slf4j
public class DefaultOperationLogHandler implements OperationLogHandler {

    @Override
    public boolean requestIsSuccess(Object functionResult) {
        return true;
    }

    @Override
    public void save(LogRecord logRecord) {
        log.error("请实现OperationLogHandler接口,并交给Spring管理");
        log.error("请实现OperationLogHandler接口,并交给Spring管理");
        log.error("请实现OperationLogHandler接口,并交给Spring管理");
        log.error("请实现OperationLogHandler接口,并交给Spring管理");
        log.info(logRecord.toString());
    }
}
