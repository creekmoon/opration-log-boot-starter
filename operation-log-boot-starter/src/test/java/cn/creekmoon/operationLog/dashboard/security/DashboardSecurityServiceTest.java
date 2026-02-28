package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Dashboard 安全服务测试类
 */
@DisplayName("DashboardSecurityService 安全校验测试")
class DashboardSecurityServiceTest {

    private DashboardProperties properties;
    private DashboardSecurityService securityService;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new DashboardProperties();
        securityService = new DashboardSecurityService(properties);
    }

    // ========== IP白名单测试 ==========

    @Test
    @DisplayName("本地回环地址应始终允许访问")
    void testLocalhostAlwaysAllowed() {
        assertTrue(securityService.checkIpAllowed("127.0.0.1"));
        assertTrue(securityService.checkIpAllowed("localhost"));
        assertTrue(securityService.checkIpAllowed("::1"));
        assertTrue(securityService.checkIpAllowed("0:0:0:0:0:0:0:1"));
    }

    @Test
    @DisplayName("精确IP匹配")
    void testExactIpMatch() {
        properties.setAllowIps(Collections.singletonList("192.168.1.100"));
        assertTrue(securityService.checkIpAllowed("192.168.1.100"));
        assertFalse(securityService.checkIpAllowed("192.168.1.101"));
    }

    @Test
    @DisplayName("CIDR格式IP白名单 - /24网段")
    void testCidrIpMatch_24() {
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));

        // 应该允许的IP
        assertTrue(securityService.checkIpAllowed("192.168.1.1"));
        assertTrue(securityService.checkIpAllowed("192.168.1.100"));
        assertTrue(securityService.checkIpAllowed("192.168.1.254"));

        // 不应该允许的IP
        assertFalse(securityService.checkIpAllowed("192.168.2.1"));
        assertFalse(securityService.checkIpAllowed("10.0.0.1"));
    }

    @Test
    @DisplayName("CIDR格式IP白名单 - /16网段")
    void testCidrIpMatch_16() {
        properties.setAllowIps(Collections.singletonList("10.0.0.0/8"));

        assertTrue(securityService.checkIpAllowed("10.0.0.1"));
        assertTrue(securityService.checkIpAllowed("10.1.2.3"));
        assertTrue(securityService.checkIpAllowed("10.255.255.255"));

        assertFalse(securityService.checkIpAllowed("11.0.0.1"));
    }

    @Test
    @DisplayName("多IP白名单 - 混合精确IP和CIDR")
    void testMultipleIpWhitelist() {
        properties.setAllowIps(Arrays.asList(
                "127.0.0.1",
                "192.168.1.0/24",
                "10.0.0.50"
        ));

        assertTrue(securityService.checkIpAllowed("127.0.0.1"));
        assertTrue(securityService.checkIpAllowed("192.168.1.100"));
        assertTrue(securityService.checkIpAllowed("10.0.0.50"));

        assertFalse(securityService.checkIpAllowed("10.0.0.51"));
    }

    @Test
    @DisplayName("空IP白名单 - 非本地地址应被拒绝")
    void testEmptyWhitelist() {
        properties.setAllowIps(Collections.emptyList());
        assertFalse(securityService.checkIpAllowed("192.168.1.100"));
        // 但本地地址仍然允许
        assertTrue(securityService.checkIpAllowed("127.0.0.1"));
    }

    @Test
    @DisplayName("空或null IP应被拒绝")
    void testNullOrEmptyIp() {
        properties.setAllowIps(Collections.singletonList("192.168.1.100"));
        assertFalse(securityService.checkIpAllowed(""));
        assertFalse(securityService.checkIpAllowed(null));
    }

    // ========== Token校验测试 ==========

    @Test
    @DisplayName("正确Token验证通过")
    void testValidToken() {
        properties.setAuthToken("secret-token-123");
        assertTrue(securityService.checkTokenValid("secret-token-123"));
    }

    @Test
    @DisplayName("错误Token验证失败")
    void testInvalidToken() {
        properties.setAuthToken("secret-token-123");
        assertFalse(securityService.checkTokenValid("wrong-token"));
    }

    @Test
    @DisplayName("未配置Token时任何Token都失败")
    void testEmptyTokenConfig() {
        properties.setAuthToken("");
        assertFalse(securityService.checkTokenValid("any-token"));
        assertFalse(securityService.checkTokenValid(null));
    }

    @Test
    @DisplayName("Token为空或null验证失败")
    void testNullOrEmptyToken() {
        properties.setAuthToken("secret-token");
        assertFalse(securityService.checkTokenValid(""));
        assertFalse(securityService.checkTokenValid(null));
    }

    // ========== Token提取测试 ==========

    @Test
    @DisplayName("从Header提取Token")
    void testExtractTokenFromHeader() {
        properties.setTokenHeader("X-Dashboard-Token");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("my-token");

        String token = securityService.extractToken(request);
        assertEquals("my-token", token);
    }

    @Test
    @DisplayName("从Query Parameter提取Token")
    void testExtractTokenFromQuery() {
        properties.setTokenHeader("X-Dashboard-Token");
        properties.setAllowTokenInQuery(true);
        when(request.getHeader("X-Dashboard-Token")).thenReturn(null);
        when(request.getParameter("token")).thenReturn("query-token");

        String token = securityService.extractToken(request);
        assertEquals("query-token", token);
    }

    @Test
    @DisplayName("Header优先于Query Parameter")
    void testHeaderPriorityOverQuery() {
        properties.setTokenHeader("X-Dashboard-Token");
        properties.setAllowTokenInQuery(true);
        when(request.getHeader("X-Dashboard-Token")).thenReturn("header-token");
        when(request.getParameter("token")).thenReturn("query-token");

        String token = securityService.extractToken(request);
        assertEquals("header-token", token);
    }

    @Test
    @DisplayName("禁用Query Parameter时只检查Header")
    void testQueryDisabled() {
        properties.setTokenHeader("X-Dashboard-Token");
        properties.setAllowTokenInQuery(false);
        when(request.getHeader("X-Dashboard-Token")).thenReturn(null);

        String token = securityService.extractToken(request);
        assertNull(token);
    }

    @Test
    @DisplayName("自定义Token Header名称")
    void testCustomTokenHeader() {
        properties.setTokenHeader("X-Custom-Auth");
        when(request.getHeader("X-Custom-Auth")).thenReturn("custom-token");

        String token = securityService.extractToken(request);
        assertEquals("custom-token", token);
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

    // ========== 综合访问校验测试 ==========

    @Test
    @DisplayName("OFF模式 - 始终允许")
    void testAccessCheck_OffMode() {
        properties.setAuthMode(DashboardProperties.AuthMode.OFF);

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertTrue(result.allowed());
        assertNull(result.reason());
    }

    @Test
    @DisplayName("IP_ONLY模式 - IP白名单通过")
    void testAccessCheck_IpOnly_Allowed() {
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("IP_ONLY模式 - IP白名单拒绝")
    void testAccessCheck_IpOnly_Denied() {
        properties.setAuthMode(DashboardProperties.AuthMode.IP_ONLY);
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertFalse(result.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED, result.reason());
    }

    @Test
    @DisplayName("TOKEN_ONLY模式 - Token验证通过")
    void testAccessCheck_TokenOnly_Allowed() {
        properties.setAuthMode(DashboardProperties.AuthMode.TOKEN_ONLY);
        properties.setAuthToken("secret-token");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("secret-token");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("TOKEN_ONLY模式 - Token验证失败")
    void testAccessCheck_TokenOnly_Denied() {
        properties.setAuthMode(DashboardProperties.AuthMode.TOKEN_ONLY);
        properties.setAuthToken("secret-token");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("wrong-token");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertFalse(result.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.TOKEN_INVALID, result.reason());
    }

    @Test
    @DisplayName("IP_AND_TOKEN模式 - 两者都通过")
    void testAccessCheck_IpAndToken_Allowed() {
        properties.setAuthMode(DashboardProperties.AuthMode.IP_AND_TOKEN);
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));
        properties.setAuthToken("secret-token");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("secret-token");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertTrue(result.allowed());
    }

    @Test
    @DisplayName("IP_AND_TOKEN模式 - IP失败直接拒绝")
    void testAccessCheck_IpAndToken_IpDenied() {
        properties.setAuthMode(DashboardProperties.AuthMode.IP_AND_TOKEN);
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));
        properties.setAuthToken("secret-token");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertFalse(result.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED, result.reason());
    }

    @Test
    @DisplayName("IP_AND_TOKEN模式 - IP通过但Token失败")
    void testAccessCheck_IpAndToken_TokenDenied() {
        properties.setAuthMode(DashboardProperties.AuthMode.IP_AND_TOKEN);
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));
        properties.setAuthToken("secret-token");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("wrong-token");

        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        assertFalse(result.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.TOKEN_INVALID, result.reason());
    }
}
