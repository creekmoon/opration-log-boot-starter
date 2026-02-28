package cn.creekmoon.operationLog.profile;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户行为画像配置属性类
 * 配置前缀: operation-log.profile
 */
@Data
@ConfigurationProperties(prefix = "operation-log.profile")
public class ProfileProperties {

    /**
     * 是否启用用户行为画像
     */
    private boolean enabled = true;

    /**
     * 是否全局启用用户画像
     * 为true时，所有@OperationLog注解的方法都会自动开启用户画像
     * 为false时，需要在注解上显式设置 profile = true
     */
    private boolean globalEnabled = false;

    /**
     * 是否自动推断操作类型
     * 为true时，从@OperationLog的value中自动推断操作类型
     */
    private boolean autoInferType = true;

    /**
     * 是否启用默认标签策略
     * 为true时，自动生成标签（高频用户、活跃用户等）
     */
    private boolean defaultTagsEnabled = true;

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
     * 用户标签保留时间(天)
     */
    private int userTagsRetentionDays = 90;

    /**
     * 是否启用降级策略
     */
    private boolean fallbackEnabled = true;

    /**
     * 异步更新队列大小
     */
    private int asyncQueueSize = 512;
}
