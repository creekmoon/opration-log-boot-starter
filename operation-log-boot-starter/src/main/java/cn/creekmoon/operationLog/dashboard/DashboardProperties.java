package cn.creekmoon.operationLog.dashboard;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dashboard配置属性
 * 
 * 注意: Dashboard 访问路径固定为 /operation-log/dashboard
 * 如需自定义，请通过反向代理（Nginx）实现
 */
@Data
@ConfigurationProperties(prefix = "operation-log.dashboard")
public class DashboardProperties {

    /**
     * 是否启用Dashboard
     */
    private boolean enabled = true;

    /**
     * 自动刷新间隔（秒）
     * 注意: 仅适用于支持自动刷新的页面版本
     */
    private int refreshInterval = 30;
}
