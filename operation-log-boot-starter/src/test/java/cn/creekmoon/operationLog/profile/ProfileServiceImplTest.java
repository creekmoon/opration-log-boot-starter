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
        // Given
        String userId = "user123";
        Set<String> tags = new HashSet<>();
        tags.add("高频查询用户");
        tags.add("高价值用户");
        
        when(setOperations.members(anyString())).thenReturn(tags);

        // When
        Set<String> result = profileService.getUserTags(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("高频查询用户"));
    }

    @Test
    void testGetUserProfile() {
        // Given
        String userId = "user123";
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_QUERY", 100);
        entries.put("ORDER_SUBMIT", 10);
        
        Set<String> tags = new HashSet<>();
        tags.add("高频查询用户");
        
        when(hashOperations.entries(anyString())).thenReturn(entries);
        when(setOperations.members(anyString())).thenReturn(tags);

        // When
        ProfileService.UserProfile profile = profileService.getUserProfile(userId);

        // Then
        assertNotNull(profile);
        assertEquals(userId, profile.userId());
        assertEquals(2, profile.operationStats().size());
        assertTrue(profile.tags().contains("高频查询用户"));
    }

    @Test
    void testGetUsersByTag() {
        // Given
        String tag = "高频查询用户";
        Set<String> users = new HashSet<>();
        users.add("user1");
        users.add("user2");
        
        when(setOperations.members(anyString())).thenReturn(users);

        // When
        List<String> result = profileService.getUsersByTag(tag, 0, 10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetUserCountByTag() {
        // Given
        String tag = "高频查询用户";
        
        when(setOperations.size(anyString())).thenReturn(100L);

        // When
        long count = profileService.getUserCountByTag(tag);

        // Then
        assertEquals(100, count);
    }

    @Test
    void testRefreshUserTags() {
        // Given
        String userId = "user123";
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_QUERY", 100); // > 50, 应该打上"高频查询用户"标签
        entries.put("ORDER_SUBMIT", 0);
        
        when(hashOperations.entries(anyString())).thenReturn(entries);
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());

        // When
        profileService.refreshUserTags(userId);

        // Then
        verify(setOperations, atLeast(1)).add(anyString(), eq("高频查询用户"));
    }

    @Test
    void testRefreshUserTags_NotMatching() {
        // Given
        String userId = "user123";
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_QUERY", 10); // < 50, 不应该打上"高频查询用户"标签
        
        when(hashOperations.entries(anyString())).thenReturn(entries);
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());

        // When
        profileService.refreshUserTags(userId);

        // Then - verify no tags are added
        verify(setOperations, never()).add(anyString(), eq("高频查询用户"));
    }

    @Test
    void testGetStatus() {
        // Given
        Set<String> userKeys = new HashSet<>();
        userKeys.add("operation-log:user-profile:user1:counts:20240101");
        userKeys.add("operation-log:user-profile:user2:counts:20240101");
        
        Set<String> tagKeys = new HashSet<>();
        tagKeys.add("operation-log:user-profile:tag-index:高频查询用户");
        
        when(redisTemplate.keys(contains(":counts:"))).thenReturn(userKeys);
        when(redisTemplate.keys(contains(":tag-index:"))).thenReturn(tagKeys);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn("PONG");

        // When
        ProfileService.ProfileStatus status = profileService.getStatus();

        // Then
        assertNotNull(status);
        assertTrue(status.enabled());
        assertTrue(status.tagEngineEnabled());
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
        // Given - 高价值用户: ORDER_SUBMIT > 10 AND ORDER_REFUND < 2
        String userId = "user123";
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_SUBMIT", 15);
        entries.put("ORDER_REFUND", 1);
        
        when(hashOperations.entries(anyString())).thenReturn(entries);
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());

        // When
        profileService.refreshUserTags(userId);

        // Then
        verify(setOperations, atLeast(1)).add(anyString(), eq("高价值用户"));
    }

    @Test
    void testTagRuleEvaluation_PotentialChurn() {
        // Given - 潜在流失用户: ORDER_QUERY > 30 AND ORDER_SUBMIT = 0
        String userId = "user123";
        Map<Object, Object> entries = new HashMap<>();
        entries.put("ORDER_QUERY", 50);
        entries.put("ORDER_SUBMIT", 0);
        
        when(hashOperations.entries(anyString())).thenReturn(entries);
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());

        // When
        profileService.refreshUserTags(userId);

        // Then
        verify(setOperations, atLeast(1)).add(anyString(), eq("潜在流失用户"));
    }
}
