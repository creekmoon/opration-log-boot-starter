package cn.creekmoon.operationLog.profile;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户行为画像配置属性类
 * 配置前缀: operation-log.profile
 *
 * 注意: 全局启用开关在 operation-log.profile-global-enabled
 * 位于 {@link cn.creekmoon.operationLog.config.OperationLogProperties}
 */
@Data
@ConfigurationProperties(prefix = "operation-log.profile")
public class ProfileProperties {

    /**
     * 是否启用用户画像模块
     * 注意: 全局开关请使用 operation-log.profile-global-enabled
     */
    private boolean enabled = true;

    /**
     * 是否自动推断操作类型
     * 为true时，从@OperationLog的value中自动推断操作类型
     */
    private boolean autoInferType = true;

    /**
     * Redis key前缀
     */
    private String redisKeyPrefix = "operation-log:user-profile";

    /**
     * 默认统计时间范围(天)
     */
    private int defaultStatsDays = 30;

    /**
     * 操作计数保留时间(天)
     */
    private int operationCountRetentionDays = 90;

    /**
     * 是否启用降级策略
     */
    private boolean fallbackEnabled = true;

    /**
     * 异步更新队列大小
     */
    private int asyncQueueSize = 512;

    /**
     * 是否启用标签引擎
     */
    private boolean tagEngineEnabled = true;
}
