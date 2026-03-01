package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.MemoryMetricsCollector;
import cn.creekmoon.operationLog.core.UnifiedMetricsCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 指标收集器自动配置
 * 根据配置选择内存模式或 Redis 模式
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "operation-log.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsCollectorConfiguration {

    /**
     * 内存版指标收集器（默认）
     */
    @Bean
    @ConditionalOnMissingBean(UnifiedMetricsCollector.class)
    @ConditionalOnProperty(prefix = "operation-log.metrics", name = "storage-mode", havingValue = "memory", matchIfMissing = true)
    public UnifiedMetricsCollector memoryMetricsCollector() {
        log.info("[operation-log] 使用内存模式指标收集器");
        return new MemoryMetricsCollector();
    }
}
