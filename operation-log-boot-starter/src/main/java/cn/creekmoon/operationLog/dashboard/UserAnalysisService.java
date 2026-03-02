package cn.creekmoon.operationLog.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户分析服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAnalysisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String USER_TIMELINE_KEY = "operation-log:user-timeline:";
    private static final String USER_STATS_KEY = "operation-log:user-stats:";
    private static final String USER_TAGS_KEY = "operation-log:user-tags:";

    /**
     * 获取用户列表
     */
    public List<UserAnalysisController.UserSummary> listUsers(int page, int size, String tag) {
        /* 从 Redis 获取用户列表 */
        Set<String> userIds = redisTemplate.opsForSet().members("operation-log:all-users");
        if (userIds == null) {
            return Collections.emptyList();
        }
        /* 过滤标签并分页，最后映射用户摘要 */
        return userIds.stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::getUserSummary)
                .filter(Objects::nonNull)
                .filter(user -> tag == null || user.tags().contains(tag))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户摘要
     */
    private UserAnalysisController.UserSummary getUserSummary(String userId) {
        try {
            Map<Object, Object> stats = redisTemplate.opsForHash().entries(USER_STATS_KEY + userId);
            if (stats.isEmpty()) {
                return null;
            }

            Set<String> tags = redisTemplate.opsForSet().members(USER_TAGS_KEY + userId);

            return new UserAnalysisController.UserSummary(
                    userId,
                    (String) stats.getOrDefault("userName", userId),
                    Long.parseLong(stats.getOrDefault("totalOps", "0").toString()),
                    parseTime((String) stats.get("lastActiveTime")),
                    tags != null ? tags : Collections.emptySet()
            );
        } catch (Exception e) {
            log.warn("获取用户摘要失败: {}", userId, e);
            return null;
        }
    }

    /**
     * 获取用户详情
     */
    public UserAnalysisController.UserDetail getUserDetail(String userId) {
        /* 查询用户统计与标签 */
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(USER_STATS_KEY + userId);
        Set<String> tags = redisTemplate.opsForSet().members(USER_TAGS_KEY + userId);
        /* 获取 top endpoints */
        Map<String, Long> topEndpoints = getTopEndpointsFromRedis(userId, 5);
        /* 获取 hourly distribution */
        Map<Integer, Long> hourlyDistribution = getHourlyDistributionFromRedis(userId);

        return new UserAnalysisController.UserDetail(
                userId,
                (String) stats.getOrDefault("userName", userId),
                parseTime((String) stats.get("firstActiveTime")),
                parseTime((String) stats.get("lastActiveTime")),
                Long.parseLong(stats.getOrDefault("totalOps", "0").toString()),
                tags != null ? tags : Collections.emptySet(),
                topEndpoints,
                hourlyDistribution,
                getCountLastDays(userId, 7),
                getCountLastDays(userId, 30)
        );
    }

    /**
     * 获取用户行为轨迹
     */
    public List<UserAnalysisController.UserTimelineEvent> getTimeline(String userId, int days) {
        String key = USER_TIMELINE_KEY + userId;
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24 * 60 * 60 * 1000L);

        Set<ZSetOperations.TypedTuple<String>> events = redisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, startTime, endTime);

        if (events == null) {
            return Collections.emptyList();
        }

        return events.stream()
                .map(tuple -> parseTimelineEvent(tuple.getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户统计
     */
    public UserAnalysisController.UserStats getUserStats(String userId) {
        Map<Object, Object> stats = redisTemplate.opsForHash().entries(USER_STATS_KEY + userId);

        long total = Long.parseLong(stats.getOrDefault("totalOps", "0").toString());
        long success = Long.parseLong(stats.getOrDefault("successCount", "0").toString());
        long errors = Long.parseLong(stats.getOrDefault("errorCount", "0").toString());

        return new UserAnalysisController.UserStats(
                total,
                success,
                errors,
                total > 0 ? (double) success / total * 100 : 0,
                Long.parseLong(stats.getOrDefault("avgResponseTime", "0").toString()),
                (String) stats.get("mostUsedEndpoint"),
                (String) stats.get("mostActiveHour")
        );
    }

    /**
     * 获取用户常用接口
     */
    public List<UserAnalysisController.EndpointUsage> getTopEndpoints(String userId, int limit) {
        Map<String, Long> endpoints = getTopEndpointsFromRedis(userId, limit);
        long total = endpoints.values().stream().mapToLong(Long::longValue).sum();

        return endpoints.entrySet().stream()
                .map(entry -> new UserAnalysisController.EndpointUsage(
                        entry.getKey(),
                        entry.getKey(), // TODO: get operation name
                        entry.getValue(),
                        total > 0 ? (double) entry.getValue() / total * 100 : 0
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取用户活跃时间分布
     */
    public Map<Integer, Long> getHourlyDistribution(String userId) {
        return getHourlyDistributionFromRedis(userId);
    }

    // ==================== Helper Methods ====================

    private Map<String, Long> getTopEndpointsFromRedis(String userId, int limit) {
        String key = "operation-log:user-endpoints:" + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        return entries.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> Long.parseLong(e.getValue().toString()),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    private Map<Integer, Long> getHourlyDistributionFromRedis(String userId) {
        String key = "operation-log:user-hourly:" + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        Map<Integer, Long> result = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            result.put(i, 0L);
        }

        entries.forEach((k, v) -> {
            try {
                int hour = Integer.parseInt(k.toString());
                long count = Long.parseLong(v.toString());
                result.put(hour, count);
            } catch (Exception ignored) {}
        });

        return result;
    }

    private long getCountLastDays(String userId, int days) {
        /* TODO: implement */
        return 0;
    }

    private LocalDateTime parseTime(String timeStr) {
        if (timeStr == null) return null;
        try {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private UserAnalysisController.UserTimelineEvent parseTimelineEvent(String json) {
        /* TODO: parse JSON */
        return null;
    }
}
