package cn.creekmoon.operationLog.dashboard;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard配置属性
 *
 * 注意: Dashboard 访问路径固定为 /operation-log/dashboard
 * 如需自定义，请通过反向代理（Nginx）实现
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "operation-log.dashboard")
public class DashboardProperties {

    /**
     * 是否启用Dashboard（默认：false，v2.3+ 变更）
     * 为了安全考虑，Dashboard 默认关闭，需要显式开启
     */
    private boolean enabled = false;

    /**
     * 自动刷新间隔（秒）
     * 注意: 仅适用于支持自动刷新的页面版本
     */
    private int refreshInterval = 30;

    // ========== 新认证配置 (v2.3+) ==========

    /**
     * Basic Auth 认证配置
     * 如果不配置 auth 节点，则无需认证（开发环境方便）
     * 如果配置了 auth 节点，则需要 Basic Auth 认证
     */
    private AuthConfig auth;

    /**
     * Basic Auth 配置类
     */
    @Data
    public static class AuthConfig {
        /**
         * 用户名（默认：admin）
         */
        private String username = "admin";

        /**
         * 密码（默认：空）
         * 生产环境强烈建议使用环境变量: ${DASHBOARD_PASSWORD}
         */
        private String password = "";
    }

    // ========== 废弃配置 (v2.3+ 废弃) ==========

    /**
     * 访问控制模式（已废弃，v2.3+ 使用 auth 配置）
     * @deprecated 使用 auth 配置替代，v3.0 将移除
     */
    @Deprecated(since = "2.3", forRemoval = true)
    private AuthMode authMode = AuthMode.OFF;

    /**
     * IP白名单列表（已废弃，v2.3+ 不再支持 IP 白名单）
     * @deprecated v2.3+ 不再支持 IP 白名单，建议使用 Nginx/防火墙实现
     */
    @Deprecated(since = "2.3", forRemoval = true)
    private List<String> allowIps = new ArrayList<>();

    /**
     * Token认证密钥（已废弃，v2.3+ 使用 auth.password 替代）
     * @deprecated 使用 auth.password 替代，v3.0 将移除
     */
    @Deprecated(since = "2.3", forRemoval = true)
    private String authToken = "";

    /**
     * Token请求头名称（已废弃，v2.3+ 使用 Basic Auth）
     * @deprecated v2.3+ 使用 Basic Auth，v3.0 将移除
     */
    @Deprecated(since = "2.3", forRemoval = true)
    private String tokenHeader = "X-Dashboard-Token";

    /**
     * 是否允许通过Query参数传递Token（已废弃）
     * @deprecated v2.3+ 使用 Basic Auth，v3.0 将移除
     */
    @Deprecated(since = "2.3", forRemoval = true)
    private boolean allowTokenInQuery = false;

    /**
     * 认证失败时的响应消息（已废弃，v2.3+ 使用标准 Basic Auth 响应）
     * @deprecated v2.3+ 使用标准响应，v3.0 将移除
     */
    @Deprecated(since = "2.3", forRemoval = true)
    private String authFailureMessage = "Dashboard access denied";

    /**
     * 访问控制模式枚举（已废弃）
     * @deprecated v2.3+ 使用 auth 配置替代
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public enum AuthMode {
        OFF,
        IP_ONLY,
        TOKEN_ONLY,
        IP_AND_TOKEN
    }

    // ========== 兼容性处理 ==========

    /**
     * 检查是否启用了认证
     * v2.3+: 判断 auth 节点是否配置且 password 不为空
     * 兼容: 旧配置的 TOKEN_ONLY/IP_AND_TOKEN 模式
     */
    public boolean isAuthEnabled() {
        // 优先检查新配置
        if (auth != null && !isEmpty(auth.getPassword())) {
            return true;
        }
        // 兼容旧配置：TOKEN_ONLY 或 IP_AND_TOKEN 模式
        if (authMode == AuthMode.TOKEN_ONLY || authMode == AuthMode.IP_AND_TOKEN) {
            return !isEmpty(authToken);
        }
        return false;
    }

    /**
     * 获取认证用户名
     * v2.3+: 返回 auth.username
     * 兼容: 旧配置返回 "admin"
     */
    public String getAuthUsername() {
        if (auth != null && !isEmpty(auth.getUsername())) {
            return auth.getUsername();
        }
        // 兼容旧配置，使用默认用户名
        if ((authMode == AuthMode.TOKEN_ONLY || authMode == AuthMode.IP_AND_TOKEN) && !isEmpty(authToken)) {
            return "admin";
        }
        return "admin";
    }

    /**
     * 获取认证密码
     * v2.3+: 返回 auth.password
     * 兼容: 旧配置的 authToken
     */
    public String getAuthPassword() {
        if (auth != null && !isEmpty(auth.getPassword())) {
            return auth.getPassword();
        }
        // 兼容旧配置：将 authToken 作为 password
        if ((authMode == AuthMode.TOKEN_ONLY || authMode == AuthMode.IP_AND_TOKEN) && !isEmpty(authToken)) {
            return authToken;
        }
        return "";
    }

    /**
     * 配置验证和兼容性检查
     */
    @PostConstruct
    public void validate() {
        // 检查旧配置使用情况
        checkLegacyConfig();

        // 检查新配置安全性
        checkSecurityConfig();
    }

    /**
     * 检查旧配置并打印迁移警告
     */
    private void checkLegacyConfig() {
        if (authMode != AuthMode.OFF) {
            log.warn("[Deprecated] 'auth-mode' 配置已废弃，将在 v3.0 移除。请迁移到新的 'auth' 配置: " +
                    "operation-log.dashboard.auth.username=admin" +
                    "operation-log.dashboard.auth.password=YOUR_PASSWORD");
        }

        if (!allowIps.isEmpty()) {
            log.warn("[Deprecated] 'allow-ips' 配置已废弃，v2.3+ 不再支持 IP 白名单。" +
                    "建议使用 Nginx 或防火墙实现 IP 限制");
        }

        if (!isEmpty(authToken)) {
            log.warn("[Deprecated] 'auth-token' 配置已废弃，将在 v3.0 移除。请使用 'auth.password' 替代");
        }

        if (!"X-Dashboard-Token".equals(tokenHeader)) {
            log.warn("[Deprecated] 'token-header' 配置已废弃，v2.3+ 使用 Basic Auth");
        }

        if (allowTokenInQuery) {
            log.warn("[Deprecated] 'allow-token-in-query' 配置已废弃，v2.3+ 使用 Basic Auth");
        }
    }

    /**
     * 检查安全配置安全性
     */
    private void checkSecurityConfig() {
        if (!enabled) {
            return;
        }

        // 如果启用了认证，检查密码强度
        if (isAuthEnabled()) {
            String password = getAuthPassword();
            if (isEmpty(password)) {
                log.warn("[Security] Dashboard 认证已启用但密码为空，认证将无法通过");
            } else if (password.length() < 6) {
                log.warn("[Security] Dashboard 密码长度较短（建议至少6位），当前长度: {}", password.length());
            } else if ("admin".equals(password) || "password".equals(password) || "123456".equals(password)) {
                log.warn("[Security] Dashboard 使用了弱密码，建议更换更复杂的密码");
            }

            // 检查是否使用了环境变量
            if (auth != null && auth.getPassword() != null) {
                String pwd = auth.getPassword();
                if (!pwd.startsWith("${") && !pwd.contains("$")) {
                    log.info("[Security] Dashboard 密码已配置（硬编码）。生产环境建议使用环境变量: ${DASHBOARD_PASSWORD}");
                }
            }
        } else if (enabled) {
            // Dashboard 启用但没有认证
            log.info("[Security] Dashboard 已启用但未配置认证（auth.username 和 auth.password），任何人都可以访问。" +
                    "生产环境建议配置认证");
        }
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
