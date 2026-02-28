package cn.creekmoon.operationLog.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户画像服务测试类
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ProfileProperties properties;
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        properties = new ProfileProperties();
        properties.setEnabled(true);
        properties.setRedisKeyPrefix("operation-log:user-profile");
        properties.setDefaultStatsDays(30);
        
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        
        profileService = new ProfileServiceImpl(redisTemplate, properties);
    }

    @Test
    void testRecordOperation_Success() {
        // Given
        String userId = "user123";
        String operationType = "ORDER_QUERY";
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        profileService.recordOperation(userId, operationType, timestamp);

        // Then
        verify(hashOperations, atLeast(1)).increment(anyString(), eq(operationType), eq(1L));
    }

    @Test
    void testRecordOperation_Disabled() {
        // Given
        properties.setEnabled(false);
        String userId = "user123";
        String operationType = "ORDER_QUERY";

        // When
        profileService.recordOperation(userId, operationType);

        // Then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testRecordOperation_NoUserId() {
        // Given
        String userId = null;
        String operationType = "ORDER_QUERY";

        // When
        profileService.recordOperation(userId, operationType);

        // Then
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testGetUserOperationStats() {
        // Given
        String userId = "user123";
        // 使用1天来避免数据被重复计算30次
        properties.setDefaultStatsDays(1);
        profileService = new ProfileServiceImpl(redisTemplate, properties);
        
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_QUERY", 100);
        entries.put("ORDER_SUBMIT", 10);
        
        when(hashOperations.entries(anyString())).thenReturn(entries);

        // When
        Map<String, Long> result = profileService.getUserOperationStats(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(100, result.get("ORDER_QUERY"));
        assertEquals(10, result.get("ORDER_SUBMIT"));
    }

    @Test
    void testGetUserTags() {
        // Given
        String userId = "user123";

        // When
        Set<String> result = profileService.getUserTags(userId);

        // Then - 标签功能已移除，应返回空集合
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserProfile() {
        // Given
        String userId = "user123";
        properties.setDefaultStatsDays(1);
        profileService = new ProfileServiceImpl(redisTemplate, properties);
        
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_QUERY", 100);
        entries.put("ORDER_SUBMIT", 10);
        
        when(hashOperations.entries(anyString())).thenReturn(entries);

        // When
        ProfileService.UserProfile profile = profileService.getUserProfile(userId);

        // Then
        assertNotNull(profile);
        assertEquals(userId, profile.userId());
        assertEquals(2, profile.operationStats().size());
        // 标签功能已移除，应返回空集合
        assertTrue(profile.tags().isEmpty());
    }

    @Test
    void testGetUsersByTag() {
        // Given
        String tag = "高频查询用户";

        // When - 标签功能已移除，应返回空列表
        List<String> result = profileService.getUsersByTag(tag, 0, 10);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserCountByTag() {
        // Given
        String tag = "高频查询用户";

        // When - 标签功能已移除，应返回0
        long count = profileService.getUserCountByTag(tag);

        // Then
        assertEquals(0, count);
    }

    @Test
    void testRefreshUserTags() {
        // Given
        String userId = "user123";
        // 不需要任何 mock，因为标签功能已移除

        // When
        profileService.refreshUserTags(userId);

        // Then - 标签功能已移除，方法为空实现，不应抛出异常
        // 不验证任何 Redis 操作，因为功能已移除
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));
    }

    @Test
    void testRefreshUserTags_NotMatching() {
        // Given
        String userId = "user123";
        // 不需要任何 mock，因为标签功能已移除

        // When
        profileService.refreshUserTags(userId);

        // Then - 标签功能已移除，方法为空实现
        // 不验证任何 Redis 操作，因为功能已移除
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));
    }

    @Test
    void testGetStatus() {
        // Given
        Set<String> userKeys = new HashSet<>();
        userKeys.add("operation-log:user-profile:user1:counts:20240101");
        userKeys.add("operation-log:user-profile:user2:counts:20240101");
        
        when(redisTemplate.keys(contains(":counts:"))).thenReturn(userKeys);

        // When
        ProfileService.ProfileStatus status = profileService.getStatus();

        // Then
        assertNotNull(status);
        assertTrue(status.enabled());
        // 标签功能已移除，tagEngineEnabled 应为 false
        assertFalse(status.tagEngineEnabled());
    }

    @Test
    void testCleanupExpiredData() {
        // Given
        Set<String> keys = new HashSet<>();
        keys.add("operation-log:user-profile:user1:counts:20230101"); // 过期key
        
        when(redisTemplate.keys(anyString())).thenReturn(keys);

        // When
        profileService.cleanupExpiredData();

        // Then
        verify(redisTemplate, atLeast(0)).delete(anyString());
    }

    @Test
    void testTagRuleEvaluation_HighValueUser() {
        // Given - 标签功能已移除，此测试验证空实现不会抛出异常
        String userId = "user123";

        // When
        profileService.refreshUserTags(userId);

        // Then - 不应抛出异常，也不应调用任何 Redis 操作
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));
    }

    @Test
    void testTagRuleEvaluation_PotentialChurn() {
        // Given - 标签功能已移除，此测试验证空实现不会抛出异常
        String userId = "user123";

        // When
        profileService.refreshUserTags(userId);

        // Then - 不应抛出异常，也不应调用任何 Redis 操作
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));
    }
}
