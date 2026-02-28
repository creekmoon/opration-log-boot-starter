package cn.creekmoon.operationLog.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户分析控制器
 * 提供单用户行为分析 API
 */
@RestController
@RequestMapping("/operation-log/dashboard/api/users")
@RequiredArgsConstructor
public class UserAnalysisController {

    private final UserAnalysisService userAnalysisService;

    /**
     * 获取用户列表
     */
    @GetMapping
    public List<UserSummary> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tag) {
        return userAnalysisService.listUsers(page, size, tag);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public UserDetail getUserDetail(@PathVariable String userId) {
        return userAnalysisService.getUserDetail(userId);
    }

    /**
     * 获取用户行为轨迹
     */
    @GetMapping("/{userId}/timeline")
    public List<UserTimelineEvent> getUserTimeline(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int days) {
        return userAnalysisService.getTimeline(userId, days);
    }

    /**
     * 获取用户统计数据
     */
    @GetMapping("/{userId}/stats")
    public UserStats getUserStats(@PathVariable String userId) {
        return userAnalysisService.getUserStats(userId);
    }

    /**
     * 获取用户常用接口
     */
    @GetMapping("/{userId}/top-endpoints")
    public List<EndpointUsage> getUserTopEndpoints(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        return userAnalysisService.getTopEndpoints(userId, limit);
    }

    /**
     * 获取用户活跃时间分布
     */
    @GetMapping("/{userId}/hourly-distribution")
    public Map<Integer, Long> getHourlyDistribution(@PathVariable String userId) {
        return userAnalysisService.getHourlyDistribution(userId);
    }

    // ==================== DTOs ====================

    public record UserSummary(
            String userId,
            String userName,
            long totalOperations,
            LocalDateTime lastActiveTime,
            Set<String> tags
    ) {}

    public record UserDetail(
            String userId,
            String userName,
            LocalDateTime firstActiveTime,
            LocalDateTime lastActiveTime,
            long totalOperations,
            Set<String> tags,
            Map<String, Long> topEndpoints,
            Map<Integer, Long> hourlyDistribution,
            long last7DaysCount,
            long last30DaysCount
    ) {}

    public record UserTimelineEvent(
            LocalDateTime timestamp,
            String operationType,
            String endpoint,
            String operationName,
            boolean success,
            Long responseTime,
            String errorMsg
    ) {}

    public record UserStats(
            long totalOperations,
            long successCount,
            long errorCount,
            double successRate,
            long avgResponseTime,
            String mostUsedEndpoint,
            String mostActiveHour
    ) {}

    public record EndpointUsage(
            String endpoint,
            String operationName,
            long count,
            double percentage
    ) {}
}
