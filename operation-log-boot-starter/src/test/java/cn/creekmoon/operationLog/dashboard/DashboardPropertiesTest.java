package cn.creekmoon.operationLog.dashboard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DashboardProperties 测试类（v2.3+ 简化版）
 */
class DashboardPropertiesTest {

    @Test
    void testDefaultValues() {
        DashboardProperties properties = new DashboardProperties();

        assertTrue(properties.isEnabled());
        assertEquals("admin", properties.getUsername());
        assertNull(properties.getPassword());
        assertFalse(properties.isAuthEnabled());
    }

    @Test
    void testSettersAndGetters() {
        DashboardProperties properties = new DashboardProperties();

        properties.setEnabled(false);
        assertFalse(properties.isEnabled());

        properties.setEnabled(true);
        assertTrue(properties.isEnabled());
    }

    @Test
    void testUsernameConfiguration() {
        DashboardProperties properties = new DashboardProperties();

        assertEquals("admin", properties.getUsername());

        properties.setUsername("customuser");
        assertEquals("customuser", properties.getUsername());
    }

    @Test
    void testAuthEnabled_WithPassword() {
        DashboardProperties properties = new DashboardProperties();

        properties.setPassword("testpassword");

        assertTrue(properties.isAuthEnabled());
        assertEquals("admin", properties.getUsername());
        assertEquals("testpassword", properties.getPassword());
    }

    @Test
    void testAuthEnabled_WithoutPassword() {
        DashboardProperties properties = new DashboardProperties();

        assertFalse(properties.isAuthEnabled());
        assertNull(properties.getPassword());
    }

    @Test
    void testAuthEnabled_WithEmptyPassword() {
        DashboardProperties properties = new DashboardProperties();

        properties.setPassword("");

        assertFalse(properties.isAuthEnabled());
    }

    @Test
    void testAuthEnabled_WithBlankPassword() {
        DashboardProperties properties = new DashboardProperties();

        properties.setPassword("   ");

        assertFalse(properties.isAuthEnabled());
    }

    @Test
    void testFullConfiguration() {
        DashboardProperties properties = new DashboardProperties();

        properties.setEnabled(true);
        properties.setUsername("operator");
        properties.setPassword("securepass123");

        assertTrue(properties.isEnabled());
        assertTrue(properties.isAuthEnabled());
        assertEquals("operator", properties.getUsername());
        assertEquals("securepass123", properties.getPassword());
    }

    @Test
    void testToString() {
        DashboardProperties properties = new DashboardProperties();
        String str = properties.toString();

        assertNotNull(str);
    }
}
