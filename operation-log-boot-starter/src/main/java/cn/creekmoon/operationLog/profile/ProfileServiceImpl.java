package cn.creekmoon.operationLog.profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 用户行为画像服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final StringRedisTemplate redisTemplate;
    private final ProfileProperties properties;

    // 降级模式下的本地缓存
    private final Map<String, Map<String, Long>> fallbackCache = new ConcurrentHashMap<>();
    private final AtomicBoolean fallbackActive = new AtomicBoolean(false);
    private final AtomicBoolean redisErrorLogged = new AtomicBoolean(false);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @PostConstruct
    public void init() {
        log.info("[operation-log] ProfileService initialized, enabled={}", properties.isEnabled());
    }

    @PreDestroy
    public void destroy() {
        log.info("[operation-log] ProfileService destroyed");
    }

    @Override
    public void recordOperation(String userId, String operationType, LocalDateTime timestamp) {
        if (!properties.isEnabled()) {
            return;
        }

        if (userId == null || userId.isEmpty() || operationType == null || operationType.isEmpty()) {
            return;
        }

        try {
            String dateKey = timestamp != null ? timestamp.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);
            String key = buildKey(userId, dateKey);

            // 使用Redis Hash存储操作计数
            redisTemplate.opsForHash().increment(key, operationType, 1);

            // 设置过期时间
            redisTemplate.expire(key, java.time.Duration.ofDays(properties.getOperationCountRetentionDays()));

        } catch (Exception e) {
            handleRedisError("recordOperation", e);
            // 降级到本地缓存
            if (properties.isFallbackEnabled()) {
                recordOperationFallback(userId, operationType);
            }
        }
    }

    @Override
    public void recordOperation(String userId, String operationType) {
        recordOperation(userId, operationType, LocalDateTime.now());
    }

    /**
     * 降级模式下的操作记录
     */
    private void recordOperationFallback(String userId, String operationType) {
        fallbackCache.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .merge(operationType, 1L, Long::sum);
    }

    @Override
    public Map<String, Long> getUserOperationStats(String userId) {
        return getUserOperationStats(userId, properties.getDefaultStatsDays());
    }

    @Override
    public Map<String, Long> getUserOperationStats(String userId, int days) {
        if (!properties.isEnabled()) {
            return Collections.emptyMap();
        }

        Map<String, Long> result = new HashMap<>();

        try {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < days; i++) {
                LocalDate date = today.minusDays(i);
                String key = buildKey(userId, date.format(DATE_FORMATTER));

                Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
                entries.forEach((k, v) -> {
                    String operationType = k.toString();
                    Long count = v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
                    result.merge(operationType, count, Long::sum);
                });
            }

            // 合并降级缓存数据
            if (fallbackActive.get() && fallbackCache.containsKey(userId)) {
                Map<String, Long> fallbackData = fallbackCache.get(userId);
                fallbackData.forEach((k, v) -> result.merge(k, v, Long::sum));
            }

        } catch (Exception e) {
            handleRedisError("getUserOperationStats", e);
        }

        return result;
    }

    @Override
    public Set<String> getUserTags(String userId) {
        // 标签功能已移除，返回空集合
        return Collections.emptySet();
    }

    @Override
    public UserProfile getUserProfile(String userId) {
        return getUserProfile(userId, properties.getDefaultStatsDays());
    }

    @Override
    public UserProfile getUserProfile(String userId, int days) {
        if (!properties.isEnabled()) {
            return null;
        }

        Map<String, Long> operationStats = getUserOperationStats(userId, days);

        return new UserProfile(
                userId,
                Collections.emptySet(), // 标签功能已移除
                operationStats,
                Collections.emptyMap(),
                Collections.emptyMap(),
                null,
                LocalDateTime.now()
        );
    }

    @Override
    public List<String> getUsersByTag(String tag, int page, int size) {
        // 标签功能已移除，返回空列表
        return Collections.emptyList();
    }

    @Override
    public long getUserCountByTag(String tag) {
        // 标签功能已移除，返回0
        return 0;
    }

    @Override
    public void refreshUserTags(String userId) {
        // 标签功能已移除，无需操作
    }

    @Override
    public void refreshAllUserTags() {
        // 标签功能已移除，无需操作
    }

    @Override
    public void cleanupExpiredData() {
        if (!properties.isEnabled()) {
            return;
        }

        log.info("[operation-log] Starting profile data cleanup");

        try {
            // 清理过期的操作计数数据
            LocalDate cutoffDate = LocalDate.now().minusDays(properties.getOperationCountRetentionDays());
            String cutoffDateStr = cutoffDate.format(DATE_FORMATTER);

            String pattern = properties.getRedisKeyPrefix() + ":*:counts:*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null) {
                int deleted = 0;
                for (String key : keys) {
                    // 提取日期部分
                    int lastColon = key.lastIndexOf(':');
                    if (lastColon > 0) {
                        String dateStr = key.substring(lastColon + 1);
                        if (dateStr.compareTo(cutoffDateStr) < 0) {
                            redisTemplate.delete(key);
                            deleted++;
                        }
                    }
                }
                log.info("[operation-log] Deleted {} expired profile keys", deleted);
            }

            // 清理降级缓存
            fallbackCache.clear();
        } catch (Exception e) {
            log.error("[operation-log] Error during profile cleanup", e);
        }
    }

    @Override
    public ProfileStatus getStatus() {
        boolean redisConnected = checkRedisConnection();

        long totalUsers = 0;

        try {
            Set<String> userKeys = redisTemplate.keys(properties.getRedisKeyPrefix() + ":*:counts:*");
            if (userKeys != null) {
                totalUsers = userKeys.stream()
                        .map(this::extractUserIdFromKey)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();
            }
        } catch (Exception e) {
            log.warn("[operation-log] Failed to get status counts", e);
        }

        return new ProfileStatus(
                properties.isEnabled(),
                redisConnected,
                false, // tagEngineEnabled 已移除
                fallbackActive.get(),
                totalUsers,
                0 // totalTags 已移除
        );
    }

    /**
     * 检查Redis连接状态
     */
    private boolean checkRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            if (fallbackActive.get()) {
                fallbackActive.set(false);
                redisErrorLogged.set(false);
                log.info("[operation-log] Redis connection restored");
            }
            return true;
        } catch (Exception e) {
            fallbackActive.set(true);
            return false;
        }
    }

    /**
     * 处理Redis错误
     */
    private void handleRedisError(String operation, Exception e) {
        fallbackActive.set(true);
        if (!redisErrorLogged.getAndSet(true)) {
            log.warn("[operation-log] Redis error in {}: {}. Switching to fallback mode.",
                    operation, e.getMessage());
        }
    }

    /**
     * 构建Redis key
     */
    private String buildKey(String userId, String date) {
        return properties.getRedisKeyPrefix() + ":" + userId + ":counts:" + date;
    }

    /**
     * 从key中提取用户ID
     */
    private String extractUserIdFromKey(String key) {
        String prefix = properties.getRedisKeyPrefix() + ":";
        if (!key.startsWith(prefix)) {
            return null;
        }
        String remaining = key.substring(prefix.length());
        int firstColon = remaining.indexOf(':');
        if (firstColon > 0) {
            return remaining.substring(0, firstColon);
        }
        return null;
    }

    // ==================== CSV导出方法实现 ====================

    @Override
    public List<List<String>> exportUserProfileToCsv(String userId) {
        List<List<String>> rows = new ArrayList<>();

        UserProfile profile = getUserProfile(userId);
        if (profile == null) {
            rows.add(Arrays.asList("用户ID", "操作类型", "操作次数"));
            return rows;
        }

        // 表头
        rows.add(Arrays.asList("用户ID", "操作类型", "操作次数"));

        // 操作统计行
        if (profile.operationStats() != null && !profile.operationStats().isEmpty()) {
            for (Map.Entry<String, Long> entry : profile.operationStats().entrySet()) {
                rows.add(Arrays.asList(
                        profile.userId(),
                        entry.getKey(),
                        String.valueOf(entry.getValue())
                ));
            }
        } else {
            rows.add(Arrays.asList(profile.userId(), "", ""));
        }

        return rows;
    }

    @Override
    public List<List<String>> exportUsersByTagToCsv(String tag, int page, int size) {
        // 标签功能已移除，返回空表头
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("用户ID", "操作统计JSON"));
        return rows;
    }

    @Override
    public List<List<String>> exportAllUserStatsToCsv(int limit) {
        List<List<String>> rows = new ArrayList<>();

        // 表头
        rows.add(Arrays.asList("用户ID", "操作统计JSON"));

        try {
            // 获取所有用户
            String pattern = properties.getRedisKeyPrefix() + ":*:counts:*";
            Set<String> keys = redisTemplate.keys(pattern);

            if (keys != null) {
                Set<String> userIds = keys.stream()
                        .map(this::extractUserIdFromKey)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(limit)
                        .collect(Collectors.toSet());

                for (String userId : userIds) {
                    Map<String, Long> stats = getUserOperationStats(userId);
                    if (!stats.isEmpty()) {
                        String statsJson = stats.toString();
                        rows.add(Arrays.asList(userId, statsJson));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[operation-log] Error exporting all user stats", e);
        }

        return rows;
    }
}
