package cn.creekmoon.operationLog.actuator;

import cn.creekmoon.operationLog.export.CsvExportService;
import cn.creekmoon.operationLog.profile.ProfileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
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
    private final CsvExportService csvExportService;

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

    // ==================== CSV导出端点 ====================

    /**
     * 导出用户画像CSV
     */
    @ReadOperation(operation = "export/user/{userId}")
    public void exportUserProfile(@Selector String userId, HttpServletResponse response) throws IOException {
        List<List<String>> data = profileService.exportUserProfileToCsv(userId);
        String fileName = csvExportService.generateFileName("profile-user-" + userId);
        
        response.setContentType(csvExportService.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出标签用户列表CSV
     */
    @ReadOperation(operation = "export/tag/{tag}")
    public void exportUsersByTag(@Selector String tag, HttpServletResponse response) throws IOException {
        List<List<String>> data = profileService.exportUsersByTagToCsv(tag, 0, 100);
        String fileName = csvExportService.generateFileName("profile-tag-" + tag);
        
        response.setContentType(csvExportService.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }

    /**
     * 导出所有用户统计CSV
     */
    @ReadOperation(operation = "export/all")
    public void exportAllUsers(HttpServletResponse response) throws IOException {
        List<List<String>> data = profileService.exportAllUserStatsToCsv(1000);
        String fileName = csvExportService.generateFileName("profile-all-users");
        
        response.setContentType(csvExportService.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        
        try (OutputStream out = response.getOutputStream()) {
            csvExportService.exportCsvWithBom(data.get(0), data.subList(1, data.size()), 
                    row -> row, out);
        }
    }
}
