package cn.creekmoon.operationLog.heatmap;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HeatmapProperties 测试类
 */
class HeatmapPropertiesTest {

    @Test
    void testDefaultValues() {
        HeatmapProperties properties = new HeatmapProperties();
        
        assertTrue(properties.isEnabled());
        assertEquals("operation-log:heatmap", properties.getRedisKeyPrefix());
        assertEquals(24, properties.getRealtimeRetentionHours());
        assertEquals(7, properties.getHourlyRetentionDays());
        assertEquals(90, properties.getDailyRetentionDays());
        assertEquals(10, properties.getTopNDefaultSize());
        assertEquals(100, properties.getTopNMaxSize());
        assertTrue(properties.isFallbackEnabled());
        assertEquals(1000, properties.getFallbackMaxSize());
        assertNotNull(properties.getExcludeOperationTypes());
        assertTrue(properties.getExcludeOperationTypes().isEmpty());
        assertEquals(1.0, properties.getSampleRate());
    }

    @Test
    void testSettersAndGetters() {
        HeatmapProperties properties = new HeatmapProperties();
        
        properties.setEnabled(false);
        assertFalse(properties.isEnabled());
        
        properties.setRedisKeyPrefix("custom:heatmap");
        assertEquals("custom:heatmap", properties.getRedisKeyPrefix());
        
        properties.setRealtimeRetentionHours(48);
        assertEquals(48, properties.getRealtimeRetentionHours());
        
        properties.setHourlyRetentionDays(14);
        assertEquals(14, properties.getHourlyRetentionDays());
        
        properties.setDailyRetentionDays(180);
        assertEquals(180, properties.getDailyRetentionDays());
        
        properties.setTopNDefaultSize(20);
        assertEquals(20, properties.getTopNDefaultSize());
        
        properties.setTopNMaxSize(200);
        assertEquals(200, properties.getTopNMaxSize());
        
        properties.setFallbackEnabled(false);
        assertFalse(properties.isFallbackEnabled());
        
        properties.setFallbackMaxSize(500);
        assertEquals(500, properties.getFallbackMaxSize());
        
        properties.setExcludeOperationTypes(Arrays.asList("LOGIN", "LOGOUT"));
        assertEquals(2, properties.getExcludeOperationTypes().size());
        
        properties.setSampleRate(0.5);
        assertEquals(0.5, properties.getSampleRate());
    }

    @Test
    void testToString() {
        HeatmapProperties properties = new HeatmapProperties();
        String str = properties.toString();
        
        assertNotNull(str);
    }

    @Test
    void testEqualsAndHashCode() {
        HeatmapProperties properties1 = new HeatmapProperties();
        HeatmapProperties properties2 = new HeatmapProperties();
        
        assertEquals(properties1, properties2);
        assertEquals(properties1.hashCode(), properties2.hashCode());
        
        properties2.setRealtimeRetentionHours(48);
        assertNotEquals(properties1, properties2);
    }
}
