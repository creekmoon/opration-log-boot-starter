package cn.creekmoon.operationLog.dashboard;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dashboard配置属性
 */
@Data
@ConfigurationProperties(prefix = "operation-log.dashboard")
public class DashboardProperties {

    /**
     * 是否启用Dashboard
     */
    private boolean enabled = true;

    /**
     * 访问路径
     */
    private String path = "/operation-log/dashboard";

    /**
     * 自动刷新间隔（秒）
     */
    private int refreshInterval = 30;
}
