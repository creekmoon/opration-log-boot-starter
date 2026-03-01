package cn.creekmoon.operationLog.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * OperationLogProperties 测试类
 */
class OperationLogPropertiesTest {

    @Test
    void testDefaultValues() {
        OperationLogProperties properties = new OperationLogProperties();
        
        assertFalse(properties.isHeatmapGlobalEnabled());
        assertFalse(properties.isProfileGlobalEnabled());
        assertFalse(properties.isHandleOnFailGlobalEnabled());
        assertFalse(properties.isUseValueAsType());
        assertNotNull(properties.getThreadPool());
    }

    @Test
    void testSettersAndGetters() {
        OperationLogProperties properties = new OperationLogProperties();
        
        properties.setHeatmapGlobalEnabled(true);
        assertTrue(properties.isHeatmapGlobalEnabled());
        
        properties.setProfileGlobalEnabled(true);
        assertTrue(properties.isProfileGlobalEnabled());
        
        properties.setHandleOnFailGlobalEnabled(true);
        assertTrue(properties.isHandleOnFailGlobalEnabled());
        
        properties.setUseValueAsType(true);
        assertTrue(properties.isUseValueAsType());
    }

    @Test
    void testThreadPoolProperties() {
        OperationLogProperties.ThreadPoolProperties threadPool = new OperationLogProperties.ThreadPoolProperties();
        
        // 测试默认值
        assertEquals(0, threadPool.getCoreSize());
        assertEquals(4, threadPool.getMaxSize());
        assertEquals(512, threadPool.getQueueCapacity());
        assertEquals(60, threadPool.getKeepAliveSeconds());
        
        // 测试 setter
        threadPool.setCoreSize(2);
        threadPool.setMaxSize(8);
        threadPool.setQueueCapacity(1024);
        threadPool.setKeepAliveSeconds(120);
        
        assertEquals(2, threadPool.getCoreSize());
        assertEquals(8, threadPool.getMaxSize());
        assertEquals(1024, threadPool.getQueueCapacity());
        assertEquals(120, threadPool.getKeepAliveSeconds());
    }

    @Test
    void testThreadPoolPropertiesEqualsAndHashCode() {
        OperationLogProperties.ThreadPoolProperties pool1 = new OperationLogProperties.ThreadPoolProperties();
        OperationLogProperties.ThreadPoolProperties pool2 = new OperationLogProperties.ThreadPoolProperties();
        
        assertEquals(pool1, pool2);
        assertEquals(pool1.hashCode(), pool2.hashCode());
        
        pool2.setCoreSize(1);
        assertNotEquals(pool1, pool2);
    }

    @Test
    void testToString() {
        OperationLogProperties properties = new OperationLogProperties();
        String str = properties.toString();
        
        assertNotNull(str);
    }
}
