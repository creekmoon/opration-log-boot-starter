package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Dashboard 安全过滤器测试类
 */
@DisplayName("DashboardSecurityFilter 过滤器测试")
class DashboardSecurityFilterTest {

    private DashboardSecurityFilter securityFilter;
    private DashboardProperties properties;

    @Mock
    private DashboardSecurityService securityService;

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
        securityFilter = new DashboardSecurityFilter(securityService, properties);
        
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
    @DisplayName("Dashboard路径 - 需要认证")
    void testDashboardPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.success()
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Dashboard HTML页面 - 需要认证")
    void testDashboardHtmlPath() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/operation-log-dashboard.html");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.success()
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("认证端点 - 跳过认证")
    void testAuthEndpointSkipped() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/auth/login");
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityService);
    }

    // ========== 认证模式测试 ==========

    @Test
    @DisplayName("OFF模式 - Dashboard启用时直接放行")
    void testOffMode_Allowed() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.OFF);
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityService);
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

    @Test
    @DisplayName("IP_ONLY模式 - 访问被拒绝")
    void testIpOnly_Denied() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        properties.setAuthFailureMessage("Access Denied");
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.failure(
                DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED, 
                "192.168.1.100"
            )
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).setCharacterEncoding(StandardCharsets.UTF_8.name());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("TOKEN_ONLY模式 - Token无效")
    void testTokenOnly_Invalid() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.TOKEN_ONLY);
        properties.setAuthFailureMessage("Invalid Token");
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.failure(
                DashboardSecurityService.AuthFailureReason.TOKEN_INVALID, 
                "10.0.0.1"
            )
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("IP_AND_TOKEN模式 - 访问通过")
    void testIpAndToken_Allowed() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.IP_AND_TOKEN);
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.success()
        );
        
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

    // ========== 错误响应测试 ==========

    @Test
    @DisplayName("验证错误响应格式 - IP被拒绝")
    void testErrorResponseFormat_IpDenied() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        properties.setAuthFailureMessage("IP Not Allowed");
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.failure(
                DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED, 
                "10.0.0.1"
            )
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        String responseBody = responseWriter.toString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("401"));
        assertTrue(responseBody.contains("IP_NOT_ALLOWED"));
        assertTrue(responseBody.contains("10.0.0.1"));
    }

    @Test
    @DisplayName("验证错误响应格式 - Token无效")
    void testErrorResponseFormat_TokenInvalid() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.TOKEN_ONLY);
        properties.setAuthFailureMessage("Token Invalid");
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.failure(
                DashboardSecurityService.AuthFailureReason.TOKEN_INVALID, 
                "192.168.1.100"
            )
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        String responseBody = responseWriter.toString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("401"));
        assertTrue(responseBody.contains("TOKEN_INVALID"));
        assertTrue(responseBody.contains("192.168.1.100"));
    }

    @Test
    @DisplayName("验证错误响应格式 - 无失败原因")
    void testErrorResponseFormat_NoReason() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        properties.setAuthFailureMessage("Access Denied");
        when(request.getRequestURI()).thenReturn("/operation-log/dashboard/stats");
        when(securityService.checkAccess(request)).thenReturn(
            new DashboardSecurityService.AuthResult(false, null, "192.168.1.100")
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        String responseBody = responseWriter.toString();
        assertNotNull(responseBody);
        assertTrue(responseBody.contains("401"));
        assertTrue(responseBody.contains("192.168.1.100"));
    }

    // ========== 路径匹配边界测试 ==========

    @Test
    @DisplayName("路径匹配 - /operation-log开头")
    void testPathMatching_OperationLogPrefix() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        when(request.getRequestURI()).thenReturn("/operation-log/api/data");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.success()
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(securityService).checkAccess(request);
    }

    @Test
    @DisplayName("路径匹配 - 包含dashboard路径")
    void testPathMatching_ContainsDashboard() throws ServletException, IOException {
        properties.setEnabled(true);
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        when(request.getRequestURI()).thenReturn("/api/operation-log/dashboard/config");
        when(securityService.checkAccess(request)).thenReturn(
            DashboardSecurityService.AuthResult.success()
        );
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(securityService).checkAccess(request);
    }

    @Test
    @DisplayName("路径不匹配 - 不包含operation-log")
    void testPathNotMatching() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/health");
        
        securityFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(securityService);
    }
}
