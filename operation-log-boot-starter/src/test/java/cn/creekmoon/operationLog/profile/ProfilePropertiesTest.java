package cn.creekmoon.operationLog.profile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ProfileProperties 测试类
 */
class ProfilePropertiesTest {

    @Test
    void testDefaultValues() {
        ProfileProperties properties = new ProfileProperties();
        
        assertTrue(properties.isEnabled());
        assertTrue(properties.isAutoInferType());
        assertEquals("operation-log:user-profile", properties.getRedisKeyPrefix());
        assertEquals(30, properties.getDefaultStatsDays());
        assertEquals(90, properties.getOperationCountRetentionDays());
        assertTrue(properties.isFallbackEnabled());
        assertEquals(512, properties.getAsyncQueueSize());
        assertTrue(properties.isTagEngineEnabled());
    }

    @Test
    void testSettersAndGetters() {
        ProfileProperties properties = new ProfileProperties();
        
        properties.setEnabled(false);
        assertFalse(properties.isEnabled());
        
        properties.setAutoInferType(false);
        assertFalse(properties.isAutoInferType());
        
        properties.setRedisKeyPrefix("custom:prefix");
        assertEquals("custom:prefix", properties.getRedisKeyPrefix());
        
        properties.setDefaultStatsDays(60);
        assertEquals(60, properties.getDefaultStatsDays());
        
        properties.setOperationCountRetentionDays(180);
        assertEquals(180, properties.getOperationCountRetentionDays());
        
        properties.setFallbackEnabled(false);
        assertFalse(properties.isFallbackEnabled());
        
        properties.setAsyncQueueSize(1024);
        assertEquals(1024, properties.getAsyncQueueSize());
        
        properties.setTagEngineEnabled(false);
        assertFalse(properties.isTagEngineEnabled());
    }

    @Test
    void testToString() {
        ProfileProperties properties = new ProfileProperties();
        String str = properties.toString();
        
        assertNotNull(str);
    }

    @Test
    void testEqualsAndHashCode() {
        ProfileProperties properties1 = new ProfileProperties();
        ProfileProperties properties2 = new ProfileProperties();
        
        assertEquals(properties1, properties2);
        assertEquals(properties1.hashCode(), properties2.hashCode());
        
        properties2.setDefaultStatsDays(60);
        assertNotEquals(properties1, properties2);
    }
}
