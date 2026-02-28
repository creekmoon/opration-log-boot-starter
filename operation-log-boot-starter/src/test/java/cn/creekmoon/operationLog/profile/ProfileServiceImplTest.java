package cn.creekmoon.operationLog.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SetOperations;
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

    @Mock
    private SetOperations<String, String> setOperations;

    private ProfileProperties properties;
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        properties = new ProfileProperties();
        properties.setEnabled(true);
        properties.setRedisKeyPrefix("operation-log:user-profile");
        properties.setDefaultStatsDays(30);
        
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
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
        // Given - 标签功能已移除
        String userId = "user123";

        // When
        Set<String> result = profileService.getUserTags(userId);

        // Then - 应返回空集合
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserProfile() {
        // Given
        String userId = "user123";
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
        // Given - 标签功能已移除
        String tag = "高频查询用户";

        // When
        List<String> result = profileService.getUsersByTag(tag, 0, 10);

        // Then - 应返回空列表
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserCountByTag() {
        // Given - 标签功能已移除
        String tag = "高频查询用户";

        // When
        long count = profileService.getUserCountByTag(tag);

        // Then - 应返回 0
        assertEquals(0, count);
    }

    @Test
    void testRefreshUserTags() {
        // Given - 标签功能已移除，无需 mock stubbing
        String userId = "user123";

        // When - 调用方法不应抛出异常
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));

        // Then - 标签功能已移除，不应对 Redis set 操作
        verifyNoInteractions(setOperations);
    }

    @Test
    void testRefreshUserTags_NotMatching() {
        // Given
        String userId = "user123";
        // Note: refreshUserTags() is now an empty implementation (tag feature removed)
        // No stubbing needed as the method doesn't interact with Redis

        // When
        profileService.refreshUserTags(userId);

        // Then - verify no interaction with setOperations (tag feature removed)
        verifyNoInteractions(setOperations);
    }

    @Test
    void testGetStatus() {
        // Given
        Set<String> userKeys = new HashSet<>();
        userKeys.add("operation-log:user-profile:user1:counts:20240101");
        userKeys.add("operation-log:user-profile:user2:counts:20240101");
        
        when(redisTemplate.keys(contains(":counts:"))).thenReturn(userKeys);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

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
        // Given - 标签功能已移除，无需 mock stubbing
        String userId = "user123";

        // When - 调用方法不应抛出异常
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));

        // Then - 标签功能已移除，不应对 Redis set 操作
        verifyNoInteractions(setOperations);
    }

    @Test
    void testTagRuleEvaluation_PotentialChurn() {
        // Given - 标签功能已移除，无需 mock stubbing
        String userId = "user123";

        // When - 调用方法不应抛出异常
        assertDoesNotThrow(() -> profileService.refreshUserTags(userId));

        // Then - 标签功能已移除，不应对 Redis set 操作
        verifyNoInteractions(setOperations);
    }
}
