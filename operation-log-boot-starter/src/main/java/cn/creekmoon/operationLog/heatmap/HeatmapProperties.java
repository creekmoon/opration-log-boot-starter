package cn.creekmoon.operationLog.heatmap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 热力图配置属性类
 * 配置前缀: operation-log.heatmap
 * 
 * 注意: 全局启用开关在 operation-log.heatmap-global-enabled
 * 位于 {@link cn.creekmoon.operationLog.config.OperationLogProperties}
 */
@Data
@ConfigurationProperties(prefix = "operation-log.heatmap")
public class HeatmapProperties {

    /**
     * 是否启用热力图模块
     * 注意: 全局开关请使用 operation-log.heatmap-global-enabled
     */
    private boolean enabled = true;

    /**
     * Redis key前缀
     */
    private String redisKeyPrefix = "operation-log:heatmap";

    /**
     * 实时数据保留时间(小时)
     */
    private int realtimeRetentionHours = 24;

    /**
     * 小时级数据保留时间(天)
     */
    private int hourlyRetentionDays = 7;

    /**
     * 天级数据保留时间(天)
     */
    private int dailyRetentionDays = 90;

    /**
     * TopN查询默认返回数量
     */
    private int topNDefaultSize = 10;

    /**
     * TopN查询最大返回数量
     */
    private int topNMaxSize = 100;

    /**
     * 是否启用降级策略(当Redis不可用时)
     */
    private boolean fallbackEnabled = true;

    /**
     * 降级时的最大本地缓存数量
     */
    private int fallbackMaxSize = 1000;

    /**
     * 需要排除统计的操作类型列表
     */
    private List<String> excludeOperationTypes = new ArrayList<>();

    /**
     * 采样率(0.0-1.0, 1.0表示全量)
     */
    private double sampleRate = 1.0;
}
