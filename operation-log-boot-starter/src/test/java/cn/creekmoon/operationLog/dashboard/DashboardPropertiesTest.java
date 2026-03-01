package cn.creekmoon.operationLog.dashboard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DashboardProperties 测试类
 */
class DashboardPropertiesTest {

    @Test
    void testDefaultValues() {
        DashboardProperties properties = new DashboardProperties();
        
        assertFalse(properties.isEnabled());
        assertEquals(30, properties.getRefreshInterval());
        assertNull(properties.getAuth());
    }

    @Test
    void testSettersAndGetters() {
        DashboardProperties properties = new DashboardProperties();
        
        properties.setEnabled(true);
        assertTrue(properties.isEnabled());
        
        properties.setRefreshInterval(60);
        assertEquals(60, properties.getRefreshInterval());
    }

    @Test
    void testAuthConfigDefaultValues() {
        DashboardProperties.AuthConfig authConfig = new DashboardProperties.AuthConfig();
        
        assertEquals("admin", authConfig.getUsername());
        assertEquals("", authConfig.getPassword());
    }

    @Test
    void testAuthConfigSettersAndGetters() {
        DashboardProperties.AuthConfig authConfig = new DashboardProperties.AuthConfig();
        
        authConfig.setUsername("customuser");
        assertEquals("customuser", authConfig.getUsername());
        
        authConfig.setPassword("custompass");
        assertEquals("custompass", authConfig.getPassword());
    }

    @Test
    void testAuthEnabled_WithAuthConfig() {
        DashboardProperties properties = new DashboardProperties();
        
        DashboardProperties.AuthConfig auth = new DashboardProperties.AuthConfig();
        auth.setPassword("testpassword");
        properties.setAuth(auth);
        
        assertTrue(properties.isAuthEnabled());
        assertEquals("admin", properties.getAuthUsername());
        assertEquals("testpassword", properties.getAuthPassword());
    }

    @Test
    void testAuthEnabled_WithoutAuthConfig() {
        DashboardProperties properties = new DashboardProperties();
        
        assertFalse(properties.isAuthEnabled());
        assertEquals("admin", properties.getAuthUsername());
        assertEquals("", properties.getAuthPassword());
    }

    @Test
    void testAuthEnabled_WithEmptyPassword() {
        DashboardProperties properties = new DashboardProperties();
        
        DashboardProperties.AuthConfig auth = new DashboardProperties.AuthConfig();
        auth.setPassword("");
        properties.setAuth(auth);
        
        assertFalse(properties.isAuthEnabled());
    }

    @Test
    void testDeprecatedAuthMode() {
        DashboardProperties properties = new DashboardProperties();
        
        // 使用废弃的 AuthMode 仍然可以工作
        properties.setAuthMode(DashboardProperties.AuthMode.TOKEN_ONLY);
        properties.setAuthToken("testtoken");
        
        assertTrue(properties.isAuthEnabled());
        assertEquals("testtoken", properties.getAuthPassword());
    }

    @Test
    void testToString() {
        DashboardProperties properties = new DashboardProperties();
        String str = properties.toString();
        
        assertNotNull(str);
    }

    @Test
    void testAuthModeEnum() {
        // 测试 AuthMode 枚举值存在
        DashboardProperties.AuthMode[] modes = DashboardProperties.AuthMode.values();
        assertEquals(4, modes.length);
        assertNotNull(DashboardProperties.AuthMode.OFF);
        assertNotNull(DashboardProperties.AuthMode.IP_ONLY);
        assertNotNull(DashboardProperties.AuthMode.TOKEN_ONLY);
        assertNotNull(DashboardProperties.AuthMode.IP_AND_TOKEN);
    }
}
