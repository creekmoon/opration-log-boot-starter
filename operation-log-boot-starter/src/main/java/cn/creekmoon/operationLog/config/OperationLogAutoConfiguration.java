package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogAspect;
import cn.creekmoon.operationLog.core.LogThreadPool;
import cn.creekmoon.operationLog.core.OperationLogContext;
import cn.creekmoon.operationLog.heatmap.HeatmapConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@EnableConfigurationProperties(OperationLogProperties.class)
@Import({LogAspect.class,
        DefaultOperationLogHandler.class,
        DefaultOperationLogRecordInitializer.class,
        HeatmapConfiguration.class})
public class OperationLogAutoConfiguration {
    @PostConstruct
    public void init() {
        /*当标记整个服务启用*/
        OperationLogContext.disable = false;
    }

    @PreDestroy
    public void destroy() {
        /*应用关闭时优雅关闭线程池，防止资源泄漏*/
        LogThreadPool.shutdown();
    }


}
