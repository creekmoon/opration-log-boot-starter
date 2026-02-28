package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogAspect;
import cn.creekmoon.operationLog.core.LogThreadPool;
import cn.creekmoon.operationLog.core.OperationLogContext;
import cn.creekmoon.operationLog.heatmap.HeatmapConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@EnableConfigurationProperties(OperationLogProperties.class)
@Import({LogAspect.class,
        DefaultOperationLogHandler.class,
        DefaultOperationLogRecordInitializer.class,
        HeatmapConfiguration.class})
@RequiredArgsConstructor
public class OperationLogAutoConfiguration {

    private final OperationLogProperties operationLogProperties;

    @PostConstruct
    public void init() {
        /*当标记整个服务启用*/
        OperationLogContext.disable = false;
        /*使用配置参数初始化线程池*/
        LogThreadPool.initialize(operationLogProperties);
    }

    @PreDestroy
    public void destroy() {
        /*应用关闭时优雅关闭线程池，防止资源泄漏*/
        LogThreadPool.shutdown();
    }

}
