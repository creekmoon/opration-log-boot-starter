package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Dashboard 安全校验服务（已简化）
 * 
 * v2.3+ 变更说明：
 * - 主要认证逻辑已移至 DashboardSecurityFilter（使用 Basic Auth）
 * - 此类保留用于向后兼容，v3.0 可能移除
 * - IP 白名单和 Token 认证功能已废弃
 * 
 * @deprecated v2.3+ 使用 Basic Auth，此类保留用于向后兼容
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated(since = "2.3", forRemoval = true)
public class DashboardSecurityService {

    private final DashboardProperties properties;

    // 本地回环地址常量
    private static final List<String> LOCALHOST_VARIANTS = Arrays.asList(
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", "::1"
    );

    /**
     * 校验请求是否允许访问Dashboard
     * 
     * @deprecated v2.3+ 认证逻辑在 DashboardSecurityFilter 中实现
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public AuthResult checkAccess(HttpServletRequest request) {
        DashboardProperties.AuthMode mode = properties.getAuthMode();
        
        // 如果使用了新配置（auth.username/password），直接返回成功
        // 因为认证已在 DashboardSecurityFilter 中完成
        if (properties.getAuth() != null) {
            log.debug("检测到新 auth 配置，认证由 DashboardSecurityFilter 处理");
            return AuthResult.success();
        }

        // 向后兼容：旧配置模式下执行原有逻辑
        // 无认证模式，直接放行
        if (mode == DashboardProperties.AuthMode.OFF) {
            return AuthResult.success();
        }

        String clientIp = getClientIp(request);

        // IP白名单校验
        if (mode == DashboardProperties.AuthMode.IP_ONLY ||
            mode == DashboardProperties.AuthMode.IP_AND_TOKEN) {
            if (!checkIpAllowed(clientIp)) {
                log.warn("Dashboard访问被拒绝 - IP不在白名单中: clientIp={}", clientIp);
                return AuthResult.failure(AuthFailureReason.IP_NOT_ALLOWED, clientIp);
            }
        }

        // Token校验
        if (mode == DashboardProperties.AuthMode.TOKEN_ONLY ||
            mode == DashboardProperties.AuthMode.IP_AND_TOKEN) {
            String token = extractToken(request);
            if (!checkTokenValid(token)) {
                log.warn("Dashboard访问被拒绝 - Token无效: clientIp={}", clientIp);
                return AuthResult.failure(AuthFailureReason.TOKEN_INVALID, clientIp);
            }
        }

        return AuthResult.success();
    }

    /**
     * 检查IP是否在白名单中
     * 
     * @deprecated v2.3+ 不再支持 IP 白名单
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public boolean checkIpAllowed(String clientIp) {
        log.warn("[Deprecated] IP 白名单功能已废弃，v2.3+ 使用 Basic Auth");
        
        if (!StringUtils.hasText(clientIp)) {
            return false;
        }

        // 默认允许本地访问
        if (isLocalhost(clientIp)) {
            return true;
        }

        List<String> allowedIps = properties.getAllowIps();
        if (allowedIps == null || allowedIps.isEmpty()) {
            return false;
        }

        for (String allowedIp : allowedIps) {
            if (matchIp(clientIp, allowedIp.trim())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查Token是否有效
     * 
     * @deprecated v2.3+ 使用 Basic Auth
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public boolean checkTokenValid(String token) {
        log.warn("[Deprecated] Token 认证已废弃，v2.3+ 使用 Basic Auth");
        
        String configToken = properties.getAuthToken();
        if (!StringUtils.hasText(configToken)) {
            return false;
        }
        return configToken.equals(token);
    }

    /**
     * 从请求中提取Token
     * 
     * @deprecated v2.3+ 使用 Basic Auth
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public String extractToken(HttpServletRequest request) {
        log.warn("[Deprecated] Token 认证已废弃，v2.3+ 使用 Basic Auth");

        /* 从 Header 提取 token */
        String tokenHeader = properties.getTokenHeader();
        String token = request.getHeader(tokenHeader);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }

        /* 允许 query 参数时，从 query 提取 token */
        if (properties.isAllowTokenInQuery()) {
            token = request.getParameter("token");
            if (StringUtils.hasText(token)) {
                return token.trim();
            }
        }

        return null;
    }

    /**
     * 获取客户端真实IP
     * 支持X-Forwarded-For等代理头
     * 
     * @deprecated v2.3+ 不再使用 IP 白名单，但此方法保留供其他用途
     */
    @Deprecated(since = "2.3")
    public String getClientIp(HttpServletRequest request) {
        // 按优先级检查各种代理头
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For可能包含多个IP，取第一个
                String firstIp = value.split(",")[0].trim();
                if (isValidIp(firstIp)) {
                    return firstIp;
                }
            }
        }

        // 如果没有代理头，使用远程地址
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    // ========== 私有辅助方法 ==========

    private boolean isLocalhost(String ip) {
        return LOCALHOST_VARIANTS.contains(ip.toLowerCase());
    }

    private boolean matchIp(String clientIp, String allowedIp) {
        if (!StringUtils.hasText(allowedIp)) {
            return false;
        }

        // 精确匹配
        if (!allowedIp.contains("/")) {
            return clientIp.equals(allowedIp);
        }

        // CIDR匹配
        return matchCidr(clientIp, allowedIp);
    }

    private boolean matchCidr(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            // 简单实现：仅支持IPv4
            if (!clientIp.contains(":")) {
                return matchIpv4Cidr(clientIp, network, prefixLength);
            }

            return false;
        } catch (Exception e) {
            log.warn("CIDR解析失败: {}", cidr, e);
            return false;
        }
    }

    private boolean matchIpv4Cidr(String clientIp, String network, int prefixLength) {
        try {
            long clientIpLong = ipToLong(clientIp);
            long networkIpLong = ipToLong(network);
            long mask = -1L << (32 - prefixLength);
            return (clientIpLong & mask) == (networkIpLong & mask);
        } catch (Exception e) {
            log.warn("IPv4 CIDR匹配失败: clientIp={}, network={}", clientIp, network, e);
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(parts[i]);
        }
        return result;
    }

    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$") || ip.contains(":");
    }

    // ========== 内部类定义 ==========

    /**
     * 认证结果
     * @deprecated v2.3+ 使用 Basic Auth
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public record AuthResult(boolean allowed, AuthFailureReason reason, String clientIp) {
        public static AuthResult success() {
            return new AuthResult(true, null, null);
        }

        public static AuthResult failure(AuthFailureReason reason, String clientIp) {
            return new AuthResult(false, reason, clientIp);
        }
    }

    /**
     * 认证失败原因
     * @deprecated v2.3+ 使用 Basic Auth
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public enum AuthFailureReason {
        IP_NOT_ALLOWED("IP_NOT_ALLOWED", "IP地址不在白名单中"),
        TOKEN_INVALID("TOKEN_INVALID", "Token无效或缺失");

        private final String code;
        private final String message;

        AuthFailureReason(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
