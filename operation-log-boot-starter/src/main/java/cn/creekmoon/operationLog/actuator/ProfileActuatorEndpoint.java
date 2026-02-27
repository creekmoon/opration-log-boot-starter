package cn.creekmoon.operationLog.actuator;

import cn.creekmoon.operationLog.profile.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户画像Actuator端点
 * 访问路径: /actuator/operation-log-profile
 */
@Component
@Endpoint(id = "operation-log-profile")
@RequiredArgsConstructor
public class ProfileActuatorEndpoint {

    private final ProfileService profileService;

    /**
     * 获取用户画像服务状态
     */
    @ReadOperation
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        ProfileService.ProfileStatus status = profileService.getStatus();
        
        result.put("enabled", status.enabled());
        result.put("redisConnected", status.redisConnected());
        result.put("tagEngineEnabled", status.tagEngineEnabled());
        result.put("fallbackActive", status.fallbackActive());
        result.put("totalUsers", status.totalUsers());
        result.put("totalTags", status.totalTags());
        
        return result;
    }

    /**
     * 获取指定用户的画像
     */
    @ReadOperation(operation = "user/{userId}")
    public ProfileService.UserProfile userProfile(@Selector String userId) {
        return profileService.getUserProfile(userId);
    }

    /**
     * 获取指定用户的标签
     */
    @ReadOperation(operation = "user/{userId}/tags")
    public Set<String> userTags(@Selector String userId) {
        return profileService.getUserTags(userId);
    }

    /**
     * 获取指定用户的操作统计
     */
    @ReadOperation(operation = "user/{userId}/stats")
    public Map<String, Long> userStats(@Selector String userId) {
        return profileService.getUserOperationStats(userId);
    }

    /**
     * 根据标签查询用户
     */
    @ReadOperation(operation = "tag/{tag}")
    public Map<String, Object> usersByTag(@Selector String tag) {
        Map<String, Object> result = new HashMap<>();
        
        long count = profileService.getUserCountByTag(tag);
        List<String> users = profileService.getUsersByTag(tag, 0, 20);
        
        result.put("tag", tag);
        result.put("userCount", count);
        result.put("users", users);
        
        return result;
    }
}
