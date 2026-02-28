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
 * Dashboard 安全校验服务
 * 提供 IP 白名单和 Token 认证的校验逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardSecurityService {

    private final DashboardProperties properties;

    // 本地回环地址常量
    private static final List<String> LOCALHOST_VARIANTS = Arrays.asList(
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", "::1"
    );

    /**
     * 校验请求是否允许访问Dashboard
     *
     * @param request HTTP请求
     * @return 校验结果
     */
    public AuthResult checkAccess(HttpServletRequest request) {
        DashboardProperties.AuthMode mode = properties.getAuthMode();

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
     * @param clientIp 客户端IP
     * @return 是否允许
     */
    public boolean checkIpAllowed(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return false;
        }

        // 默认允许本地访问
        if (isLocalhost(clientIp)) {
            return true;
        }

        List<String> allowedIps = properties.getAllowIps();
        if (allowedIps == null || allowedIps.isEmpty()) {
            // 如果未配置白名单且不是本地访问，则拒绝
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
     * @param token 请求Token
     * @return 是否有效
     */
    public boolean checkTokenValid(String token) {
        String configToken = properties.getAuthToken();
        if (!StringUtils.hasText(configToken)) {
            // 如果未配置Token，则拒绝所有请求（安全默认）
            return false;
        }
        return configToken.equals(token);
    }

    /**
     * 从请求中提取Token
     * 优先级: Header > Query Parameter
     *
     * @param request HTTP请求
     * @return Token值
     */
    public String extractToken(HttpServletRequest request) {
        // 1. 从Header获取
        String tokenHeader = properties.getTokenHeader();
        String token = request.getHeader(tokenHeader);
        if (StringUtils.hasText(token)) {
            return token.trim();
        }

        // 2. 从Query Parameter获取 (如果启用)
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
     * @param request HTTP请求
     * @return 客户端IP
     */
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

    /**
     * 判断是否为本地地址
     */
    private boolean isLocalhost(String ip) {
        return LOCALHOST_VARIANTS.contains(ip.toLowerCase());
    }

    /**
     * IP匹配逻辑
     * 支持精确匹配和CIDR格式
     */
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

    /**
     * CIDR格式IP匹配
     */
    private boolean matchCidr(String clientIp, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            // 简单实现：仅支持IPv4
            if (!clientIp.contains(":")) {
                return matchIpv4Cidr(clientIp, network, prefixLength);
            }

            // IPv6简单处理，暂不完整支持
            return false;
        } catch (Exception e) {
            log.warn("CIDR解析失败: {}", cidr, e);
            return false;
        }
    }

    /**
     * IPv4 CIDR匹配
     */
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

    /**
     * IP地址转长整型
     */
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * 简单IP格式校验
     */
    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        // 简单校验：IPv4或IPv6格式
        return ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$") || ip.contains(":");
    }

    // ========== 内部类定义 ==========

    /**
     * 认证结果
     */
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
     */
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
