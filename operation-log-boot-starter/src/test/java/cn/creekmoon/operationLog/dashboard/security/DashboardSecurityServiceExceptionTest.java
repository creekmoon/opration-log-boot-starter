package cn.creekmoon.operationLog.dashboard.security;

import cn.creekmoon.operationLog.dashboard.DashboardProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * DashboardSecurityService 异常处理分支测试
 */
@DisplayName("DashboardSecurityService 异常处理测试")
class DashboardSecurityServiceExceptionTest {

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

    // ========== IP匹配异常测试 ==========

    @Test
    @DisplayName("CIDR格式异常 - 无效的前缀长度")
    void testCidrInvalidPrefix() {
        properties.setAllowIps(Collections.singletonList("192.168.1.0/abc"));
        
        // 不应该抛出异常，应该返回 false
        assertFalse(securityService.checkIpAllowed("192.168.1.100"));
    }

    @Test
    @DisplayName("CIDR格式异常 - 缺少斜杠")
    void testCidrMissingSlash() {
        properties.setAllowIps(Collections.singletonList("192.168.1.0"));
        
        // 精确匹配
        assertTrue(securityService.checkIpAllowed("192.168.1.0"));
        assertFalse(securityService.checkIpAllowed("192.168.1.1"));
    }

    @Test
    @DisplayName("CIDR格式异常 - 空网段")
    void testCidrEmptyNetwork() {
        properties.setAllowIps(Collections.singletonList("/24"));
        
        // 不应该抛出异常
        assertFalse(securityService.checkIpAllowed("192.168.1.100"));
    }

    @Test
    @DisplayName("CIDR格式异常 - 负数前缀")
    void testCidrNegativePrefix() {
        properties.setAllowIps(Collections.singletonList("192.168.1.0/-1"));

        // 负数前缀的实际行为，验证不抛出异常即可
        assertDoesNotThrow(() -> securityService.checkIpAllowed("192.168.1.100"));
    }

    @Test
    @DisplayName("CIDR格式异常 - 超大前缀")
    void testCidrHugePrefix() {
        properties.setAllowIps(Collections.singletonList("192.168.1.0/99"));
        
        // 不应该抛出异常
        assertDoesNotThrow(() -> securityService.checkIpAllowed("192.168.1.100"));
    }

    @Test
    @DisplayName("IP地址格式异常 - 非数字段")
    void testIpFormatInvalid() {
        properties.setAllowIps(Collections.singletonList("192.168.1.0/24"));
        
        // 无效IP格式
        assertFalse(securityService.checkIpAllowed("192.168.1.abc"));
        assertFalse(securityService.checkIpAllowed("not-an-ip"));
    }

    @Test
    @DisplayName("IP地址格式异常 - IPv6 CIDR")
    void testIpv6Cidr() {
        properties.setAllowIps(Collections.singletonList("2001:db8::/32"));
        
        // IPv6 CIDR 目前返回 false
        assertFalse(securityService.checkIpAllowed("2001:db8::1"));
    }

    @Test
    @DisplayName("空CIDR白名单 - 非本地地址被拒绝")
    void testEmptyCidrWhitelist() {
        properties.setAllowIps(Collections.emptyList());
        
        assertFalse(securityService.checkIpAllowed("192.168.1.100"));
        // 本地地址仍然允许
        assertTrue(securityService.checkIpAllowed("127.0.0.1"));
    }

    // ========== Token验证异常测试 ==========

    @Test
    @DisplayName("Token为null验证")
    void testNullToken() {
        properties.setAuthToken("secret-token");
        
        assertFalse(securityService.checkTokenValid(null));
    }

    @Test
    @DisplayName("配置Token为null - 视为空字符串")
    void testNullConfigToken() {
        properties.setAuthToken(null);
        
        // null token 配置应该拒绝所有
        assertFalse(securityService.checkTokenValid("any-token"));
        assertFalse(securityService.checkTokenValid(null));
    }

    @Test
    @DisplayName("Token包含空格 - 需要精确匹配")
    void testTokenWithSpaces() {
        properties.setAuthToken("secret-token");
        
        // Token 不匹配（包含空格）
        assertFalse(securityService.checkTokenValid(" secret-token "));
        assertFalse(securityService.checkTokenValid("secret-token "));
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

    // ========== Token提取异常测试 ==========

    @Test
    @DisplayName("Token Header值为空字符串")
    void testEmptyHeaderToken() {
        properties.setTokenHeader("X-Dashboard-Token");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("");
        
        String token = securityService.extractToken(request);
        
        assertNull(token);
    }

    @Test
    @DisplayName("Query参数Token包含空格 - 正确trim")
    void testQueryTokenWithSpaces() {
        properties.setTokenHeader("X-Dashboard-Token");
        properties.setAllowTokenInQuery(true);
        when(request.getHeader("X-Dashboard-Token")).thenReturn(null);
        when(request.getParameter("token")).thenReturn("  my-token  ");
        
        String token = securityService.extractToken(request);
        
        assertEquals("my-token", token);
    }

    @Test
    @DisplayName("Query参数Token为null")
    void testNullQueryToken() {
        properties.setTokenHeader("X-Dashboard-Token");
        properties.setAllowTokenInQuery(true);
        when(request.getHeader("X-Dashboard-Token")).thenReturn(null);
        when(request.getParameter("token")).thenReturn(null);
        
        String token = securityService.extractToken(request);
        
        assertNull(token);
    }

    // ========== 综合异常测试 ==========

    @Test
    @DisplayName("IP_AND_TOKEN模式 - 空Token请求")
    void testIpAndTokenModeEmptyToken() {
        properties.setAuthMode(DashboardProperties.AuthMode.IP_AND_TOKEN);
        properties.setAllowIps(Collections.singletonList("127.0.0.1"));
        properties.setAuthToken("secret-token");
        when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
        when(request.getHeader("X-Dashboard-Token")).thenReturn(null);
        
        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        
        assertFalse(result.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.TOKEN_INVALID, result.reason());
    }

    @Test
    @DisplayName("TOKEN_ONLY模式 - 未配置Token时拒绝所有")
    void testTokenOnlyNoConfigToken() {
        properties.setAuthMode(DashboardProperties.AuthMode.TOKEN_ONLY);
        properties.setAuthToken("");
        when(request.getHeader("X-Dashboard-Token")).thenReturn("some-token");
        
        DashboardSecurityService.AuthResult result = securityService.checkAccess(request);
        
        assertFalse(result.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.TOKEN_INVALID, result.reason());
    }

    @Test
    @DisplayName("AuthResult记录 - 验证所有字段")
    void testAuthResultFields() {
        DashboardSecurityService.AuthResult success = DashboardSecurityService.AuthResult.success();
        assertTrue(success.allowed());
        assertNull(success.reason());
        assertNull(success.clientIp());
        
        DashboardSecurityService.AuthResult failure = DashboardSecurityService.AuthResult.failure(
            DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED, 
            "192.168.1.100"
        );
        assertFalse(failure.allowed());
        assertEquals(DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED, failure.reason());
        assertEquals("192.168.1.100", failure.clientIp());
    }

    @Test
    @DisplayName("AuthFailureReason枚举 - 验证属性和方法")
    void testAuthFailureReasonEnum() {
        DashboardSecurityService.AuthFailureReason ipReason = DashboardSecurityService.AuthFailureReason.IP_NOT_ALLOWED;
        assertEquals("IP_NOT_ALLOWED", ipReason.getCode());
        assertEquals("IP地址不在白名单中", ipReason.getMessage());
        assertEquals("IP_NOT_ALLOWED", ipReason.name());
        
        DashboardSecurityService.AuthFailureReason tokenReason = DashboardSecurityService.AuthFailureReason.TOKEN_INVALID;
        assertEquals("TOKEN_INVALID", tokenReason.getCode());
        assertEquals("Token无效或缺失", tokenReason.getMessage());
    }
}
