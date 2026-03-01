package cn.creekmoon.operationLog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 操作日志全局配置属性
 */
@Data
@ConfigurationProperties(prefix = "operation-log")
public class OperationLogProperties {

    /**
     * 是否全局启用操作日志
     */
    private boolean enabled = true;

    /**
     * 是否全局启用热力图统计
     */
    private boolean heatmapGlobalEnabled = false;

    /**
     * 是否全局启用用户画像统计
     */
    private boolean profileGlobalEnabled = false;

    /**
     * 是否全局默认在失败时记录日志
     */
    private boolean handleOnFailGlobalEnabled = false;

    /**
     * 是否全局使用value作为操作类型
     * 为true时，type字段将自动取value的值
     */
    private boolean useValueAsType = false;

    /**
     * 指标收集配置
     */
    private MetricsProperties metrics = new MetricsProperties();

    /**
     * 线程池配置
     */
    private ThreadPoolProperties threadPool = new ThreadPoolProperties();

    /**
     * 指标收集配置属性
     */
    @Data
    public static class MetricsProperties {
        /**
         * 存储模式: memory (默认), redis
         * - memory: 本地内存存储，适用于单实例
         * - redis: Redis中心化存储，适用于多实例
         */
        private String storageMode = "memory";

        /**
         * Redis 配置（仅在 storage-mode=redis 时生效）
         */
        private RedisMetricsProperties redis = new RedisMetricsProperties();
    }

    /**
     * Redis 指标配置属性
     */
    @Data
    public static class RedisMetricsProperties {
        /**
         * 是否启用全局采样（多副本采样一致性）
         */
        private boolean globalSamplerEnabled = true;

        /**
         * 是否启用用户行为关联（跨副本用户追踪）
         */
        private boolean userInsightEnabled = true;

        /**
         * 降级配置
         */
        private FallbackProperties fallback = new FallbackProperties();
    }

    /**
     * 降级配置属性
     */
    @Data
    public static class FallbackProperties {
        /**
         * Redis 不可用时是否降级到内存模式
         */
        private boolean enabled = true;

        /**
         * 降级时采样率（减少负载）
         */
        private double sampleRate = 0.1;
    }

    /**
     * 线程池配置属性
     */
    @Data
    public static class ThreadPoolProperties {
        /**
         * 核心线程数，默认 0（根据需要创建）
         */
        private int coreSize = 0;

        /**
         * 最大线程数，默认 4
         */
        private int maxSize = 4;

        /**
         * 队列容量，默认 512
         */
        private int queueCapacity = 512;

        /**
         * 线程保持存活时间（秒），默认 60
         */
        private long keepAliveSeconds = 60;

        /**
         * 线程名称前缀，默认 "log-thread-"
         */
        private String threadNamePrefix = "log-thread-";

        /**
         * 是否允许核心线程超时，默认 true
         */
        private boolean allowCoreThreadTimeout = true;
    }
}
