package cn.creekmoon.operationLog.dashboard;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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

    /**
     * 认证模式
     */
    private AuthMode authMode = AuthMode.OFF;

    /**
     * IP白名单列表
     */
    private List<String> allowIps;

    /**
     * 认证Token
     */
    private String authToken;

    /**
     * Token请求头名称
     */
    private String tokenHeader = "X-Dashboard-Token";

    /**
     * 是否允许从Query参数传递Token
     */
    private boolean allowTokenInQuery = false;

    /**
     * 认证失败提示消息
     */
    private String authFailureMessage = "Dashboard访问未授权";

    /**
     * 认证模式枚举
     */
    public enum AuthMode {
        /**
         * 无认证，直接放行（仅建议开发环境使用）
         */
        OFF,
        
        /**
         * 仅IP白名单认证
         */
        IP_ONLY,
        
        /**
         * 仅Token认证
         */
        TOKEN_ONLY,
        
        /**
         * IP白名单 + Token 双重认证
         */
        IP_AND_TOKEN
    }
}
