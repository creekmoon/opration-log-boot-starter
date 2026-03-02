package cn.creekmoon.operationLog.dashboard;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Dashboard 配置属性（简化版 v2.3）
 *
 * 核心配置（3个）：
 * 1. enabled - 是否启用 Dashboard（默认 true）
 * 2. username - Basic Auth 用户名（默认 admin）
 * 3. password - Basic Auth 密码（空则不认证）
 *
 * 访问路径: /operation-log/dashboard
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "operation-log.dashboard")
public class DashboardProperties {

    /**
     * 是否启用 Dashboard（默认：true）
     * 设为 false 可完全关闭 Dashboard 功能
     */
    private boolean enabled = true;

    /**
     * Basic Auth 用户名（默认：admin）
     */
    private String username = "admin";

    /**
     * Basic Auth 密码
     * - 为空/不配置：无需认证（开发环境方便）
     * - 配置了密码：需要 Basic Auth 认证
     * 生产环境建议通过环境变量配置: ${DASHBOARD_PASSWORD}
     */
    private String password;

    // ========== 内部方法 ==========

    /**
     * 是否启用了认证
     */
    public boolean isAuthEnabled() {
        return password != null && !password.trim().isEmpty();
    }

    /**
     * 配置验证
     */
    @PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }

        if (isAuthEnabled()) {
            if (password.length() < 6) {
                log.warn("[Security] Dashboard 密码长度较短（建议至少6位），当前长度: {}", password.length());
            }
            if ("admin".equals(password) || "password".equals(password) || "123456".equals(password)) {
                log.warn("[Security] Dashboard 使用了弱密码，建议更换更复杂的密码");
            }
            if (!password.startsWith("${") && !password.contains("$")) {
                log.info("[Security] Dashboard 密码已配置（硬编码）。生产环境建议使用环境变量: ${DASHBOARD_PASSWORD}");
            }
        } else {
            log.info("[Security] Dashboard 已启用但未配置密码，任何人都可以访问。生产环境建议设置密码");
        }
    }
}
