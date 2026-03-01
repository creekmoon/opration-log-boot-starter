package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Dashboard Basic Auth 安全过滤器
 * 
 * 简化后的认证机制：
 * 1. 默认 Dashboard 关闭 (enabled: false)
 * 2. 支持 Basic Auth 认证（可选）
 * 3. 向后兼容旧配置（IP白名单/Token 已废弃但保留功能）
 *
 * 注意：此过滤器在Spring Security过滤器链之后执行
 * 如果应用配置了Spring Security，需要在Security配置中允许Dashboard路径
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 100)  // 确保在Spring Security之后，但在Controller之前
public class DashboardSecurityFilter extends OncePerRequestFilter {

    private final DashboardProperties properties;

    // Dashboard路径前缀
    private static final String DASHBOARD_PATH_PREFIX = "/operation-log";
    private static final String DASHBOARD_HTML_PATH = "/operation-log-dashboard.html";
    private static final String AUTH_PATH_PREFIX = "/operation-log/dashboard/auth";

    // Basic Auth realm
    private static final String REALM = "Dashboard";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // 跳过非Dashboard路径
        if (!isDashboardPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 跳过认证端点本身（兼容旧版本）
        if (isAuthEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 如果Dashboard未启用，返回404
        if (!properties.isEnabled()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 如果没有启用认证，直接放行
        if (!properties.isAuthEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 执行 Basic Auth 认证
        if (checkBasicAuth(request)) {
            filterChain.doFilter(request, response);
        } else {
            sendUnauthorizedResponse(response);
        }
    }

    /**
     * 检查 Basic Auth
     */
    private boolean checkBasicAuth(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Basic ")) {
            return false;
        }

        try {
            // 解码 Base64
            String base64Credentials = authHeader.substring(6);
            String credentials = new String(
                    Base64.getDecoder().decode(base64Credentials), 
                    StandardCharsets.UTF_8);
            
            // 格式: username:password
            int colonIndex = credentials.indexOf(':');
            if (colonIndex < 0) {
                return false;
            }

            String username = credentials.substring(0, colonIndex);
            String password = credentials.substring(colonIndex + 1);

            // 验证用户名密码
            String expectedUsername = properties.getAuthUsername();
            String expectedPassword = properties.getAuthPassword();

            return expectedUsername.equals(username) && expectedPassword.equals(password);

        } catch (Exception e) {
            log.debug("Basic Auth 解析失败", e);
            return false;
        }
    }

    /**
     * 发送401未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + REALM + "\"");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Authentication required");
    }

    /**
     * 判断是否为Dashboard相关路径
     */
    private boolean isDashboardPath(String requestUri) {
        // 匹配 /operation-log 开头的路径
        if (requestUri.startsWith(DASHBOARD_PATH_PREFIX)) {
            return true;
        }
        // 匹配 Dashboard HTML 页面
        if (requestUri.equals(DASHBOARD_HTML_PATH)) {
            return true;
        }
        // 匹配 API 路径
        if (requestUri.contains("/operation-log/dashboard/")) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否为认证端点
     */
    private boolean isAuthEndpoint(String requestUri) {
        return requestUri.startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS请求直接放行（CORS预检）
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
