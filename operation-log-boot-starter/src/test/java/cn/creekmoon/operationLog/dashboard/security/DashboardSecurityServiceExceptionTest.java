package cn.creekmoon.operationLog.dashboard.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * DashboardSecurityService 异常处理分支测试（v2.3+ 简化版）
 */
@DisplayName("DashboardSecurityService 异常处理测试")
class DashboardSecurityServiceExceptionTest {

    private DashboardSecurityService securityService;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityService = new DashboardSecurityService();
    }

    // ========== 客户端IP获取异常测试 ==========

    @Test
    @DisplayName("X-Forwarded-For包含多个IP - 取第一个")
    void testXForwardedForMultipleIps() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 10.0.0.1");

        String ip = securityService.getClientIp(request);

        assertEquals("203.0.113.195", ip);
    }

    @Test
    @DisplayName("X-Forwarded-For包含空格 - 正确trim")
    void testXForwardedForWithSpaces() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.195  , 70.41.3.18");

        String ip = securityService.getClientIp(request);

        assertEquals("203.0.113.195", ip);
    }

    @Test
    @DisplayName("多个代理头 - 按优先级取")
    void testMultipleProxyHeaders() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("192.168.1.1");

        String ip = securityService.getClientIp(request);

        assertEquals("192.168.1.1", ip);
    }

    @Test
    @DisplayName("所有代理头都为unknown - 使用remoteAddr")
    void testAllProxyHeadersUnknown() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("UNKNOWN");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String ip = securityService.getClientIp(request);

        assertEquals("10.0.0.1", ip);
    }

    @Test
    @DisplayName("代理头包含IPv6 - 正确提取")
    void testProxyHeaderIpv6() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8::1");

        String ip = securityService.getClientIp(request);

        assertEquals("2001:db8::1", ip);
    }

    @Test
    @DisplayName("RemoteAddr为null - 返回unknown")
    void testNullRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        String ip = securityService.getClientIp(request);

        assertEquals("unknown", ip);
    }

    @Test
    @DisplayName("无效IP格式 - 返回unknown")
    void testInvalidIpFormat() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        String ip = securityService.getClientIp(request);

        assertEquals("192.168.1.1", ip);
    }

    // ========== 本地地址检查边界测试 ==========

    @Test
    @DisplayName("本地地址检查 - 大小写不敏感")
    void testLocalhostCaseInsensitive() {
        assertTrue(securityService.isLocalhost("LOCALHOST"));
        assertTrue(securityService.isLocalhost("LocalHost"));
    }

    @Test
    @DisplayName("本地地址检查 - 空值和null")
    void testLocalhostNullAndEmpty() {
        assertFalse(securityService.isLocalhost(null));
        assertFalse(securityService.isLocalhost(""));
    }
}
