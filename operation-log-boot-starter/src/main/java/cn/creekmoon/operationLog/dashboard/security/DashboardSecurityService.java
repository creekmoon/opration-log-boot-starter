package cn.creekmoon.operationLog.dashboard.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Dashboard 安全服务（v2.3+ 简化版）
 *
 * v2.3 变更说明：
 * - 认证逻辑已完全移至 DashboardSecurityFilter
 * - 此类仅保留获取客户端 IP 的实用方法
 * - IP 白名单和 Token 认证已移除
 */
@Slf4j
@Service
public class DashboardSecurityService {

    // 本地回环地址常量
    private static final java.util.List<String> LOCALHOST_VARIANTS = java.util.Arrays.asList(
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", "::1"
    );

    /**
     * 获取客户端真实IP
     * 支持 X-Forwarded-For 等代理头
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
                // X-Forwarded-For 可能包含多个 IP，取第一个
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
     * 检查是否为本地地址
     */
    public boolean isLocalhost(String ip) {
        if (ip == null) {
            return false;
        }
        return LOCALHOST_VARIANTS.contains(ip.toLowerCase());
    }

    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$") || ip.contains(":");
    }
}
