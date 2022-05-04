package cn.jy.operationLog.config;

import cn.jy.operationLog.core.LogAspect;
import cn.jy.operationLog.core.LogContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

@Configuration
@Import({LogAspect.class,
        DefaultOperationLogHandler.class,
        DefaultOperationLogUserInfoProvider.class})
public class OptLogAutoConfiguration {
    @PostConstruct
    public void init() {
        /*当标记整个服务启用*/
        LogContext.disable = false;
    }


}
