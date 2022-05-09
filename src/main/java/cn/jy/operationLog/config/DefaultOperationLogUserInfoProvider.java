package cn.jy.operationLog.config;

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
        return unknownString;
    }

    @Override
    public Long getUserId() {
        return unknownLong;
    }

    @Override
    public Long getOrgId() {
        return unknownLong;
    }

    @Override
    public String getProjectName() {
        return unknownString;
    }
}