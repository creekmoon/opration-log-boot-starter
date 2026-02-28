package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard 访问控制过滤器
 * 拦截所有Dashboard相关请求，执行IP白名单和Token认证
 *
 * 注意：此过滤器在Spring Security过滤器链之后执行
 * 如果应用配置了Spring Security，需要在Security配置中允许Dashboard路径
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE - 100)  // 确保在Spring Security之后，但在Controller之前
public class DashboardSecurityFilter extends OncePerRequestFilter {

    private final DashboardSecurityService securityService;
    private final DashboardProperties properties;

    // Dashboard路径前缀
    private static final String DASHBOARD_PATH_PREFIX = "/operation-log";
    private static final String DASHBOARD_HTML_PATH = "/operation-log-dashboard.html";
    private static final String AUTH_PATH_PREFIX = "/operation-log/dashboard/auth";

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

        // 跳过认证端点本身（避免循环）
        if (isAuthEndpoint(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 如果Dashboard未启用，返回404
        if (!properties.isEnabled()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 无认证模式，直接放行
        if (properties.getAuthMode() == DashboardProperties.AuthMode.OFF) {
            filterChain.doFilter(request, response);
            return;
        }

        // 执行安全校验
        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);

        if (result.allowed()) {
            filterChain.doFilter(request, response);
        } else {
            sendUnauthorizedResponse(response, result);
        }
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

    /**
     * 发送401未授权响应
     */
    private void sendUnauthorizedResponse(HttpServletResponse response,
                                          DashboardSecurityService.AuthResult result) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", 401);
        errorBody.put("message", properties.getAuthFailureMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("mode", properties.getAuthMode().name());
        if (result.reason() != null) {
            details.put("reason", result.reason().getCode());
        }
        if (result.clientIp() != null) {
            details.put("clientIp", result.clientIp());
        }
        errorBody.put("details", details);

        response.getWriter().write(JSON.toJSONString(errorBody));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS请求直接放行（CORS预检）
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
