package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.MemoryMetricsCollector;
import cn.creekmoon.operationLog.core.UnifiedMetricsCollector;
import cn.creekmoon.operationLog.redis.RedisFailoverManager;
import cn.creekmoon.operationLog.redis.RedisUnifiedMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 指标收集器自动配置
 * 
 * 根据配置自动选择使用内存存储还是 Redis 存储
 * 
 * @author CodeSmith
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MetricsCollectorConfiguration {

    /**
     * 内存版指标收集器
     * 
     * 条件：
     * 1. storage-mode=memory（默认）
     * 2. 或 Redis 不可用时作为降级方案
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "operation-log.metrics",
            name = "storage-mode",
            havingValue = "memory",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(UnifiedMetricsCollector.class)
    public UnifiedMetricsCollector memoryMetricsCollector() {
        log.info("[operation-log] 使用内存版指标收集器 (storage-mode=memory)");
        return new MemoryMetricsCollector();
    }

    /**
     * Redis 版指标收集器
     * 
     * 条件：
     * 1. storage-mode=redis
     * 2. Redis 相关 Bean 可用
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "operation-log.metrics",
            name = "storage-mode",
            havingValue = "redis"
    )
    @ConditionalOnBean({StringRedisTemplate.class, RedisTemplate.class})
    public UnifiedMetricsCollector redisMetricsCollector(
            StringRedisTemplate redisTemplate,
            RedisTemplate<String, Object> objectRedisTemplate,
            RedisFailoverManager failoverManager) {
        log.info("[operation-log] 使用 Redis 版指标收集器 (storage-mode=redis)");
        return new RedisUnifiedMetricsCollector(redisTemplate, objectRedisTemplate, failoverManager);
    }

    /**
     * 降级版指标收集器（Redis 不可用时）
     * 
     * 当配置了 redis 模式但 Redis 不可用时，自动降级到内存模式
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "operation-log.metrics",
            name = "storage-mode",
            havingValue = "redis"
    )
    @ConditionalOnMissingBean({StringRedisTemplate.class, RedisTemplate.class})
    public UnifiedMetricsCollector fallbackMetricsCollector(OperationLogProperties properties) {
        boolean fallbackEnabled = properties.getMetrics().getRedis().getFallback().isEnabled();
        if (fallbackEnabled) {
            log.warn("[operation-log] Redis 不可用，已降级到内存版指标收集器");
            return new MemoryMetricsCollector();
        }
        throw new IllegalStateException(
                "[operation-log] 配置 storage-mode=redis 但 Redis 不可用，且 fallback.enabled=false。" +
                "请检查 Redis 配置或启用降级模式。"
        );
    }
}
