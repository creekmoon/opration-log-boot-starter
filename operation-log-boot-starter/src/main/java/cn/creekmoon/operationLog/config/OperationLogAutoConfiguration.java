package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogAspect;
import cn.creekmoon.operationLog.core.OperationLogContext;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@Import({LogAspect.class,
        DefaultOperationLogHandler.class,
        DefaultOperationLogRecordInitializer.class})
public class OperationLogAutoConfiguration {
    @PostConstruct
    public void init() {
        /*当标记整个服务启用*/
        OperationLogContext.disable = false;
    }


}
