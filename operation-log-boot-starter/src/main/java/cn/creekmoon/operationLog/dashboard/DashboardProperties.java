package cn.creekmoon.operationLog.dashboard;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
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

    // ========== 访问控制安全配置 ==========

    /**
     * 访问控制模式
     * OFF: 无认证（开发环境默认）
     * IP_ONLY: 仅IP白名单
     * TOKEN_ONLY: 仅Token认证
     * IP_AND_TOKEN: IP白名单 + Token双重认证（推荐生产环境）
     */
    private AuthMode authMode = AuthMode.OFF;

    /**
     * IP白名单列表
     * 支持格式: 192.168.1.1 (精确IP) 或 192.168.1.0/24 (CIDR)
     * 默认包含 127.0.0.1 和 localhost
     */
    private List<String> allowIps = new ArrayList<>();

    /**
     * Token认证密钥
     * 生产环境建议从环境变量读取: ${DASHBOARD_TOKEN:}
     */
    private String authToken = "";

    /**
     * Token请求头名称
     */
    private String tokenHeader = "X-Dashboard-Token";

    /**
     * 是否允许通过Query参数传递Token
     * 默认false，更安全
     */
    private boolean allowTokenInQuery = false;

    /**
     * 认证失败时的响应消息
     */
    private String authFailureMessage = "Dashboard access denied";

    /**
     * 访问控制模式枚举
     */
    public enum AuthMode {
        /**
         * 无认证
         */
        OFF,
        /**
         * 仅IP白名单
         */
        IP_ONLY,
        /**
         * 仅Token认证
         */
        TOKEN_ONLY,
        /**
         * IP白名单 + Token双重认证
         */
        IP_AND_TOKEN
    }
}
