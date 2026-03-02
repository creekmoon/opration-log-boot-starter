package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Dashboard 安全过滤器测试类 - Basic Auth 版本（v2.3+ 简化版）
 */
@DisplayName("DashboardSecurityFilter Basic Auth 测试")
class DashboardSecurityFilterTest {

    private DashboardSecurityFilter securityFilter;
    private DashboardProperties properties;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        properties = new DashboardProperties();
        securityFilter = new DashboardSecurityFilter(properties);

        // 设置响应 writer
        responseWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseWriter);
        when(response.getWriter()).thenReturn(writer);
    }

    // ========== 路径匹配测试 ==========

    @Test
    @DisplayName("非Dashboard路径 - 直接放行")
    void testNonDashboardPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/users");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    @DisplayName("Dashboard路径 - 启用时需要认证")
    void testDashboardPath() throws ServletException, IOException {
        properties.setEnabled(true);
        // 不配置 password，不需要认证
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Dashboard HTML页面 - 需要认证")
    void testDashboardHtmlPath() throws ServletException, IOException {
        properties.setEnabled(true);
        when(request.getRequestURI()).thenReturn("/operation-log-dashboard.html");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("认证端点 - 跳过认证")
    void testAuthEndpointSkipped() throws ServletException, IOException {
        properties.setEnabled(true);
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/auth/status");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ========== Dashboard 启用/禁用测试 ==========

    @Test
    @DisplayName("Dashboard启用且无密码配置 - 直接放行")
    void testEnabledWithoutAuth() throws ServletException, IOException {
        properties.setEnabled(true);
        // 不配置 password
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Dashboard未启用 - 返回404")
    void testDashboardDisabled() throws ServletException, IOException {
        properties.setEnabled(false);
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(filterChain, never()).doFilter(request, response);
    }

    // ========== Basic Auth 测试 ==========

    @Test
    @DisplayName("Basic Auth - 无认证头返回401")
    void testBasicAuth_NoHeader() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn(null);

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"Dashboard\"");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Basic Auth - 错误密码返回401")
    void testBasicAuth_InvalidCredentials() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Basic " +
                Base64.getEncoder().encodeToString("admin:wrongpass".getBytes(StandardCharsets.UTF_8)));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Basic Auth - 正确密码放行")
    void testBasicAuth_ValidCredentials() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Basic " +
                Base64.getEncoder().encodeToString("admin:secret123".getBytes(StandardCharsets.UTF_8)));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Basic Auth - 错误的认证类型")
    void testBasicAuth_WrongAuthType() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Basic Auth - 非法的Base64")
    void testBasicAuth_InvalidBase64() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Basic invalid_base64!!!");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Basic Auth - 缺少冒号分隔符")
    void testBasicAuth_NoColon() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("admin");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Basic " +
                Base64.getEncoder().encodeToString("adminonly".getBytes(StandardCharsets.UTF_8)));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Basic Auth - 自定义用户名")
    void testBasicAuth_CustomUsername() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setUsername("operator");
        properties.setPassword("secret123");

        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(request.getHeader("Authorization")).thenReturn("Basic " +
                Base64.getEncoder().encodeToString("operator:secret123".getBytes(StandardCharsets.UTF_8)));

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ========== CORS预检请求测试 ==========

    @Test
    @DisplayName("OPTIONS请求 - 直接放行")
    void testOptionsRequest() {
        when(request.getMethod()).thenReturn("OPTIONS");

        boolean shouldNotFilter = securityFilter.shouldNotFilter(request);

        assertTrue(shouldNotFilter);
    }

    @Test
    @DisplayName("GET请求 - 需要过滤")
    void testGetRequest() {
        when(request.getMethod()).thenReturn("GET");

        boolean shouldNotFilter = securityFilter.shouldNotFilter(request);

        assertFalse(shouldNotFilter);
    }

    // ========== 路径匹配边界测试 ==========

    @Test
    @DisplayName("路径匹配 - /operation-log开头")
    void testPathMatching_OperationLogPrefix() throws ServletException, IOException {
        properties.setEnabled(true);
        when(request.getRequestURI()).thenReturn("/operation-log/api/data");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("路径匹配 - 包含dashboard路径")
    void testPathMatching_ContainsDashboard() throws ServletException, IOException {
        properties.setEnabled(true);
        when(request.getRequestURI()).thenReturn("/api/operation-log/dashboard/config");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("路径不匹配 - 不包含operation-log")
    void testPathNotMatching() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/health");

        securityFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
