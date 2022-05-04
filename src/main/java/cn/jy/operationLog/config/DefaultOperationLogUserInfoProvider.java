package cn.jy.operationLog.config;

import cn.jy.operationLog.core.OperationLogHandler;
import cn.jy.operationLog.core.OperationLogUserInfoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnMissingBean({OperationLogUserInfoProvider.class})
@Slf4j
public class DefaultOperationLogUserInfoProvider implements OperationLogUserInfoProvider {

    public static final String unknownString = "unknown";
    public static final Long unknownLong = 1L;

    @Override
    public String getUserName() {
        log.error("请实现OperationLogUserInfoProvider接口,并交给Spring管理");
        return unknownString;
    }

    @Override
    public Long getUserId() {
        Exception exception = new Exception("请实现OperationLogUserInfoProvider接口,并交给Spring管理");
        exception.printStackTrace();
        log.error("请实现OperationLogUserInfoProvider接口,并交给Spring管理");
        return unknownLong;
    }

    @Override
    public Long getOrgId() {
        log.error("请实现OperationLogUserInfoProvider接口,并交给Spring管理");
        return unknownLong;
    }

    @Override
    public String getProjectName() {
        log.error("请实现OperationLogUserInfoProvider接口,并交给Spring管理");
        return unknownString;
    }
}
