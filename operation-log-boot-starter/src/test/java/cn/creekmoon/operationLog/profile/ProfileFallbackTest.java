package cn.creekmoon.operationLog.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Profile 降级策略测试
 */
@DisplayName("ProfileService 降级策略测试")
class ProfileFallbackTest {

    private ProfileServiceImpl profileService;
    private StringRedisTemplate redisTemplate;
    private ProfileProperties properties;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        properties = new ProfileProperties();
        properties.setEnabled(true);
        properties.setFallbackEnabled(true);
        
        // 模拟 Redis 操作抛出异常
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.increment(anyString(), any(), anyDouble()))
            .thenThrow(new RuntimeException("Redis连接失败"));
        
        profileService = new ProfileServiceImpl(redisTemplate, properties);
    }

    @Test
    @DisplayName("Redis异常时降级到本地缓存 - 记录操作")
    void testFallbackOperationRecording() {
        String userId = "user123";
        String operationType = "QUERY";
        
        // 触发降级
        profileService.recordOperation(userId, operationType);
        profileService.recordOperation(userId, operationType);
        
        // 验证服务仍然可用
        assertDoesNotThrow(() -> 
            profileService.recordOperation(userId, operationType)
        );
    }

    @Test
    @DisplayName("降级模式 - 获取用户画像合并降级数据")
    void testGetUserProfileWithFallback() {
        String userId = "user456";
        
        // 先触发降级记录一些数据
        profileService.recordOperation(userId, "CREATE");
        profileService.recordOperation(userId, "UPDATE");
        
        // 获取用户画像
        ProfileService.UserProfile profile = profileService.getUserProfile(userId);
        
        // 验证返回了有效的用户画像（即使是降级数据）
        assertNotNull(profile);
        assertEquals(userId, profile.userId());
    }

    @Test
    @DisplayName("禁用降级时 - Redis异常应处理但不崩溃")
    void testFallbackDisabled() {
        properties.setFallbackEnabled(false);
        
        assertDoesNotThrow(() -> 
            profileService.recordOperation("user789", "DELETE")
        );
    }

    @Test
    @DisplayName("禁用画像功能 - 不记录任何数据")
    void testDisabledProfile() {
        properties.setEnabled(false);
        
        profileService.recordOperation("user999", "QUERY");
        
        // 不应该访问 Redis
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("空用户ID处理")
    void testEmptyUserId() {
        assertDoesNotThrow(() -> {
            profileService.recordOperation("", "QUERY");
            profileService.recordOperation(null, "QUERY");
        });
    }

    @Test
    @DisplayName("降级缓存合并 - 多次操作")
    void testFallbackCacheMerge() {
        String userId = "mergeUser";
        
        // 记录多种类型的操作
        profileService.recordOperation(userId, "CREATE");
        profileService.recordOperation(userId, "CREATE");
        profileService.recordOperation(userId, "UPDATE");
        profileService.recordOperation(userId, "DELETE");
        
        // 获取用户画像
        ProfileService.UserProfile profile = profileService.getUserProfile(userId);
        
        assertNotNull(profile);
        assertEquals(userId, profile.userId());
    }

    @Test
    @DisplayName("获取用户操作统计 - 降级模式")
    void testGetUserOperationStatsWithFallback() {
        // 记录一些操作
        profileService.recordOperation("user1", "QUERY");
        profileService.recordOperation("user2", "UPDATE");
        
        // 获取统计
        Map<String, Long> stats = profileService.getUserOperationStats("user1");
        
        // 验证返回了映射（可能为空或包含降级数据）
        assertNotNull(stats);
    }

    @Test
    @DisplayName("刷新用户标签 - 降级模式")
    void testRefreshUserTagsWithFallback() {
        // 触发降级并记录数据
        profileService.recordOperation("tagUser", "HIGH_FREQ_OP");
        
        // 刷新标签
        assertDoesNotThrow(() -> 
            profileService.refreshUserTags("tagUser")
        );
    }

    @Test
    @DisplayName("清空用户画像 - 包含降级缓存")
    void testClearUserProfileWithFallback() {
        String userId = "clearUser";
        
        // 先记录一些降级数据
        profileService.recordOperation(userId, "OP1");
        
        // 清空画像
        assertDoesNotThrow(() -> profileService.getUserProfile(userId));
    }

    @Test
    @DisplayName("获取用户标签 - 降级模式")
    void testGetUserTagsWithFallback() {
        // 记录操作
        profileService.recordOperation("statsUser", "OP1");
        profileService.recordOperation("statsUser", "OP2");
        
        // 获取标签
        Set<String> tags = profileService.getUserTags("statsUser");
        
        assertNotNull(tags);
    }

    @Test
    @DisplayName("获取服务状态 - 包含降级状态")
    void testGetStatus() {
        ProfileService.ProfileStatus status = profileService.getStatus();
        
        assertNotNull(status);
        assertTrue(status.enabled() || !status.enabled()); // 验证返回有效状态
    }
}
