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
 * Dashboard 安全服务测试类（v2.3+ 简化版）
 */
@DisplayName("DashboardSecurityService 测试")
class DashboardSecurityServiceTest {

    private DashboardSecurityService securityService;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityService = new DashboardSecurityService();
    }

    // ========== 客户端IP获取测试 ==========

    @Test
    @DisplayName("从X-Forwarded-For获取真实IP")
    void testGetClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18");

        String ip = securityService.getClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    @DisplayName("从X-Real-IP获取真实IP")
    void testGetClientIpFromXRealIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.195");

        String ip = securityService.getClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    @DisplayName("无代理头时使用RemoteAddr")
    void testGetClientIpFromRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        String ip = securityService.getClientIp(request);
        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("X-Forwarded-For包含unknown时跳过")
    void testSkipUnknownIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.100");

        String ip = securityService.getClientIp(request);
        assertEquals("192.168.1.100", ip);
    }

    @Test
    @DisplayName("空请求返回unknown")
    void testEmptyRequest() {
        String ip = securityService.getClientIp(request);
        assertEquals("unknown", ip);
    }

    // ========== 本地地址检查测试 ==========

    @Test
    @DisplayName("本地回环地址识别")
    void testLocalhostDetection() {
        assertTrue(securityService.isLocalhost("127.0.0.1"));
        assertTrue(securityService.isLocalhost("localhost"));
        assertTrue(securityService.isLocalhost("::1"));
        assertTrue(securityService.isLocalhost("0:0:0:0:0:0:0:1"));

        assertFalse(securityService.isLocalhost("192.168.1.1"));
        assertFalse(securityService.isLocalhost("10.0.0.1"));
    }
}
