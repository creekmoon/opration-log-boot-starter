package cn.creekmoon.operationLog.heatmap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 扩展热力图服务测试类
 */
@ExtendWith(MockitoExtension.class)
class ExtendedHeatmapServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HeatmapProperties properties;

    @Mock
    private HeatmapService heatmapService;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ExtendedHeatmapServiceImpl extendedService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(properties.getRedisKeyPrefix()).thenReturn("operation-log:heatmap");
        when(properties.isEnabled()).thenReturn(true);
        
        extendedService = new ExtendedHeatmapServiceImpl(redisTemplate, properties, heatmapService);
    }

    @Test
    void testGetResponseTimeStats() {
        // Given
        Map<Object, Object> mockData = new HashMap<>();
        mockData.put("p50", 50);
        mockData.put("p95", 100);
        mockData.put("p99", 200);
        mockData.put("avg", 75);
        mockData.put("max", 500);
        mockData.put("min", 10);
        
        when(hashOperations.entries(anyString())).thenReturn(mockData);

        // When
        ExtendedHeatmapService.ResponseTimeStats stats = 
                extendedService.getResponseTimeStats("TestService", "testMethod", HeatmapService.TimeWindow.REALTIME);

        // Then
        assertNotNull(stats);
        assertEquals(50, stats.p50());
        assertEquals(100, stats.p95());
        assertEquals(200, stats.p99());
        assertEquals(75, stats.avg());
        assertEquals(500, stats.max());
        assertEquals(10, stats.min());
    }

    @Test
    void testGetResponseTimeStats_EmptyData() {
        // Given
        when(hashOperations.entries(anyString())).thenReturn(new HashMap<>());

        // When
        ExtendedHeatmapService.ResponseTimeStats stats = 
                extendedService.getResponseTimeStats("TestService", "testMethod", HeatmapService.TimeWindow.REALTIME);

        // Then
        assertNotNull(stats);
        assertEquals(0, stats.p50());
        assertEquals(0, stats.p95());
    }

    @Test
    void testGetGeoDistribution_ServiceDisabled() {
        // Given
        when(properties.isEnabled()).thenReturn(false);

        // When
        Map<String, Long> result = extendedService.getGeoDistribution(HeatmapService.TimeWindow.REALTIME);

        // Then
        assertTrue(result.isEmpty());
    }
}
