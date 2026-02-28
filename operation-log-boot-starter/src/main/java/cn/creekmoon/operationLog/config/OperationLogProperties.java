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
}
