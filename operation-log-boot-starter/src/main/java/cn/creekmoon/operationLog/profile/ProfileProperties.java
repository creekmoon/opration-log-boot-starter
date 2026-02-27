package cn.creekmoon.operationLog.profile;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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
     * 是否启用标签规则引擎
     */
    private boolean tagEngineEnabled = true;

    /**
     * 标签规则配置列表
     */
    private List<TagRule> tagRules = new ArrayList<>();

    /**
     * 是否启用降级策略
     */
    private boolean fallbackEnabled = true;

    /**
     * 异步更新队列大小
     */
    private int asyncQueueSize = 512;

    /**
     * 标签规则配置
     */
    @Data
    public static class TagRule {
        /**
         * 标签名称
         */
        private String name;

        /**
         * 规则条件表达式
         * 格式: OPERATION_TYPE > count [AND|OR] OPERATION_TYPE2 > count2
         * 示例: ORDER_QUERY > 50
         *       ORDER_SUBMIT > 10 AND ORDER_REFUND < 2
         */
        private String condition;

        /**
         * 规则优先级(数字越小优先级越高)
         */
        private int priority = 100;

        /**
         * 规则描述
         */
        private String description;
    }

    /**
     * 初始化默认标签规则
     */
    public ProfileProperties() {
        // 高频查询用户: ORDER_QUERY > 50
        TagRule highFreqQuery = new TagRule();
        highFreqQuery.setName("高频查询用户");
        highFreqQuery.setCondition("ORDER_QUERY > 50");
        highFreqQuery.setPriority(10);
        highFreqQuery.setDescription("查询操作超过50次");
        tagRules.add(highFreqQuery);

        // 高价值用户: ORDER_SUBMIT > 10 AND ORDER_REFUND < 2
        TagRule highValue = new TagRule();
        highValue.setName("高价值用户");
        highValue.setCondition("ORDER_SUBMIT > 10 AND ORDER_REFUND < 2");
        highValue.setPriority(20);
        highValue.setDescription("下单超过10次且退款少于2次");
        tagRules.add(highValue);

        // 潜在流失用户: ORDER_QUERY > 30 AND ORDER_SUBMIT = 0
        TagRule potentialChurn = new TagRule();
        potentialChurn.setName("潜在流失用户");
        potentialChurn.setCondition("ORDER_QUERY > 30 AND ORDER_SUBMIT = 0");
        potentialChurn.setPriority(30);
        potentialChurn.setDescription("查询超过30次但从不下单");
        tagRules.add(potentialChurn);

        // 高频退款用户: ORDER_REFUND > 5
        TagRule highRefund = new TagRule();
        highRefund.setName("高频退款用户");
        highRefund.setCondition("ORDER_REFUND > 5");
        highRefund.setPriority(40);
        highRefund.setDescription("退款超过5次");
        tagRules.add(highRefund);
    }
}
