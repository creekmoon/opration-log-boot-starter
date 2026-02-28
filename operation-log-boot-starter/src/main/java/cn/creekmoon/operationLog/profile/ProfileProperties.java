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
