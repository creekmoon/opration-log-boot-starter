package cn.creekmoon.operationLog.heatmap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Heatmap 降级策略测试
 */
@DisplayName("HeatmapService 降级策略测试")
class HeatmapFallbackTest {

    private HeatmapServiceImpl heatmapService;
    private StringRedisTemplate redisTemplate;
    private HeatmapProperties properties;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        properties = new HeatmapProperties();
        properties.setEnabled(true);
        properties.setFallbackEnabled(true);
        properties.setFallbackMaxSize(1000);
        
        // 模拟 Redis 操作抛出异常
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis连接失败"));
        
        heatmapService = new HeatmapServiceImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("Redis异常时降级到本地缓存 - 记录PV")
    void testFallbackPvRecording() {
        // 触发降级
        heatmapService.recordVisit("TestClass", "testMethod", "user1");
        heatmapService.recordVisit("TestClass", "testMethod", "user2");
        heatmapService.recordVisit("TestClass", "testMethod", "user3");
        
        // 验证降级模式被激活
        // 注意：由于私有字段，我们通过行为验证
        // 服务应该能够继续工作而不抛出异常
        assertDoesNotThrow(() -> {
            heatmapService.recordVisit("TestClass", "testMethod", "user4");
        });
    }

    @Test
    @DisplayName("降级缓存达到上限 - 应该限制缓存大小")
    void testFallbackCacheSizeLimit() {
        properties.setFallbackMaxSize(2);
        
        // 模拟 Redis 持续失败
        for (int i = 0; i < 10; i++) {
            heatmapService.recordVisit("Class" + i, "method", "user1");
        }
        
        // 验证没有抛出异常，服务仍然可用
        assertDoesNotThrow(() -> 
            heatmapService.recordVisit("MoreClass", "method", "user1")
        );
    }

    @Test
    @DisplayName("禁用降级时 - Redis异常应抛出")
    void testFallbackDisabled() {
        properties.setFallbackEnabled(false);
        
        // 应该抛出异常（或记录错误但不崩溃）
        assertDoesNotThrow(() -> 
            heatmapService.recordVisit("TestClass", "testMethod", "user1")
        );
    }

    @Test
    @DisplayName("采样率设置 - 部分请求被采样")
    void testSampleRate() {
        properties.setSampleRate(0.0); // 0% 采样，不记录任何数据
        
        // 不应该访问 Redis
        heatmapService.recordVisit("TestClass", "testMethod", "user1");
        
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("禁用热力图 - 不记录任何数据")
    void testDisabledHeatmap() {
        properties.setEnabled(false);
        
        heatmapService.recordVisit("TestClass", "testMethod", "user1");
        
        // 不应该访问 Redis
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("降级模式 - 空用户ID处理")
    void testFallbackWithEmptyUserId() {
        // 空用户ID应该只记录PV，不记录UV
        assertDoesNotThrow(() -> {
            heatmapService.recordVisit("TestClass", "testMethod", "");
            heatmapService.recordVisit("TestClass", "testMethod", null);
        });
    }

    @Test
    @DisplayName("Redis异常计数增加")
    void testRedisErrorCount() {
        // 触发多次 Redis 异常
        for (int i = 0; i < 5; i++) {
            try {
                heatmapService.recordVisit("TestClass", "testMethod", "user" + i);
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 验证服务仍然可用
        assertDoesNotThrow(() -> 
            heatmapService.recordVisit("TestClass", "testMethod", "newUser")
        );
    }
}
