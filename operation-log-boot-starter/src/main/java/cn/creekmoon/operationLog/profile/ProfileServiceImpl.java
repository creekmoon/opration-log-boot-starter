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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final Map<String, Map<String, AtomicBoolean>> fallbackCache = new ConcurrentHashMap<>();
    private final AtomicBoolean fallbackActive = new AtomicBoolean(false);
    private final AtomicBoolean redisErrorLogged = new AtomicBoolean(false);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 规则解析正则
    private static final Pattern RULE_PATTERN = Pattern.compile(
            "(\\w+)\\s*(=|!=|>|<|>=|<=)\\s*(\\d+)");

    @PostConstruct
    public void init() {
        log.info("[operation-log] ProfileService initialized, enabled={}, tagEngineEnabled={}",
                properties.isEnabled(), properties.isTagEngineEnabled());
    }

    @PreDestroy
    public void destroy() {
        log.info("[operation-log] ProfileService destroyed");
    }

    @Override
    public void recordOperation(String userId, String operationType, LocalDateTime timestamp) {
        if (!properties.isEnabled() || userId == null || userId.isEmpty()) {
            return;
        }

        if (operationType == null || operationType.isEmpty()) {
            operationType = "UNKNOWN";
        }

        try {
            String dateStr = timestamp.format(DATE_FORMATTER);
            
            // 用户操作计数Hash (按日期)
            String countKey = properties.getRedisKeyPrefix() + ":" + userId + ":counts:" + dateStr;
            redisTemplate.opsForHash().increment(countKey, operationType, 1);
            redisTemplate.expire(countKey, 
                    java.time.Duration.ofDays(properties.getOperationCountRetentionDays()));

            // 用户最后活跃时间
            String lastActiveKey = properties.getRedisKeyPrefix() + ":" + userId + ":meta";
            redisTemplate.opsForHash().put(lastActiveKey, "lastActiveTime", timestamp.toString());
            redisTemplate.expire(lastActiveKey, 
                    java.time.Duration.ofDays(properties.getOperationCountRetentionDays()));

            // 异步更新标签(可以优化为批量更新)
            if (properties.isTagEngineEnabled()) {
                refreshUserTags(userId);
            }
        } catch (Exception e) {
            handleRedisError("recordOperation", e);
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
        String key = userId + ":" + operationType;
        fallbackCache
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .computeIfAbsent("count", k -> new AtomicBoolean(false));
    }

    @Override
    public Map<String, Long> getUserOperationStats(String userId) {
        return getUserOperationStats(userId, properties.getDefaultStatsDays());
    }

    @Override
    public Map<String, Long> getUserOperationStats(String userId, int days) {
        if (!properties.isEnabled() || userId == null || userId.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Long> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        try {
            for (int i = 0; i < days; i++) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.format(DATE_FORMATTER);
                String countKey = properties.getRedisKeyPrefix() + ":" + userId + ":counts:" + dateStr;

                Map<Object, Object> entries = redisTemplate.opsForHash().entries(countKey);
                for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                    String operationType = entry.getKey().toString();
                    long count = entry.getValue() instanceof Number 
                            ? ((Number) entry.getValue()).longValue() 
                            : Long.parseLong(entry.getValue().toString());
                    result.merge(operationType, count, Long::sum);
                }
            }
        } catch (Exception e) {
            handleRedisError("getUserOperationStats", e);
        }

        return result;
    }

    @Override
    public Set<String> getUserTags(String userId) {
        if (!properties.isEnabled() || userId == null || userId.isEmpty()) {
            return Collections.emptySet();
        }

        try {
            String tagsKey = properties.getRedisKeyPrefix() + ":" + userId + ":tags";
            Set<String> tags = redisTemplate.opsForSet().members(tagsKey);
            return tags != null ? tags : Collections.emptySet();
        } catch (Exception e) {
            handleRedisError("getUserTags", e);
            return Collections.emptySet();
        }
    }

    @Override
    public UserProfile getUserProfile(String userId) {
        return getUserProfile(userId, properties.getDefaultStatsDays());
    }

    @Override
    public UserProfile getUserProfile(String userId, int days) {
        if (!properties.isEnabled() || userId == null || userId.isEmpty()) {
            return null;
        }

        Map<String, Long> allStats = getUserOperationStats(userId, days);
        Map<String, Long> last7DaysStats = getUserOperationStats(userId, 7);
        Map<String, Long> last30DaysStats = getUserOperationStats(userId, 30);
        Set<String> tags = getUserTags(userId);

        LocalDateTime lastActiveTime = null;
        try {
            String lastActiveKey = properties.getRedisKeyPrefix() + ":" + userId + ":meta";
            Object lastActive = redisTemplate.opsForHash().get(lastActiveKey, "lastActiveTime");
            if (lastActive != null) {
                lastActiveTime = LocalDateTime.parse(lastActive.toString());
            }
        } catch (Exception e) {
            log.warn("[operation-log] Failed to get last active time for user: {}", userId);
        }

        return new UserProfile(
                userId,
                tags,
                allStats,
                last7DaysStats,
                last30DaysStats,
                lastActiveTime,
                LocalDateTime.now()
        );
    }

    @Override
    public List<String> getUsersByTag(String tag, int page, int size) {
        if (!properties.isEnabled() || tag == null || tag.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String tagIndexKey = properties.getRedisKeyPrefix() + ":tag-index:" + tag;
            long start = (long) page * size;
            long end = start + size - 1;
            
            Set<String> users = redisTemplate.opsForSet().members(tagIndexKey);
            if (users == null) {
                return Collections.emptyList();
            }
            
            return users.stream()
                    .skip(start)
                    .limit(size)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            handleRedisError("getUsersByTag", e);
            return Collections.emptyList();
        }
    }

    @Override
    public long getUserCountByTag(String tag) {
        if (!properties.isEnabled() || tag == null || tag.isEmpty()) {
            return 0;
        }

        try {
            String tagIndexKey = properties.getRedisKeyPrefix() + ":tag-index:" + tag;
            Long size = redisTemplate.opsForSet().size(tagIndexKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            handleRedisError("getUserCountByTag", e);
            return 0;
        }
    }

    @Override
    public void refreshUserTags(String userId) {
        if (!properties.isEnabled() || !properties.isTagEngineEnabled() 
                || userId == null || userId.isEmpty()) {
            return;
        }

        try {
            Map<String, Long> stats = getUserOperationStats(userId, properties.getDefaultStatsDays());
            Set<String> newTags = evaluateTagRules(stats);
            
            // 获取旧标签
            String tagsKey = properties.getRedisKeyPrefix() + ":" + userId + ":tags";
            Set<String> oldTags = redisTemplate.opsForSet().members(tagsKey);
            
            // 删除不再满足条件的标签
            if (oldTags != null) {
                for (String oldTag : oldTags) {
                    if (!newTags.contains(oldTag)) {
                        redisTemplate.opsForSet().remove(tagsKey, oldTag);
                        String tagIndexKey = properties.getRedisKeyPrefix() + ":tag-index:" + oldTag;
                        redisTemplate.opsForSet().remove(tagIndexKey, userId);
                    }
                }
            }
            
            // 添加新标签
            for (String newTag : newTags) {
                redisTemplate.opsForSet().add(tagsKey, newTag);
                String tagIndexKey = properties.getRedisKeyPrefix() + ":tag-index:" + newTag;
                redisTemplate.opsForSet().add(tagIndexKey, userId);
            }
            
            // 设置过期时间
            redisTemplate.expire(tagsKey, 
                    java.time.Duration.ofDays(properties.getUserTagsRetentionDays()));
            
        } catch (Exception e) {
            handleRedisError("refreshUserTags", e);
        }
    }

    @Override
    public void refreshAllUserTags() {
        if (!properties.isEnabled() || !properties.isTagEngineEnabled()) {
            return;
        }

        log.info("[operation-log] Starting batch tag refresh for all users");
        
        try {
            // 获取所有用户(通过扫描keys)
            String pattern = properties.getRedisKeyPrefix() + ":*:counts:*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null) {
                Set<String> userIds = keys.stream()
                        .map(this::extractUserIdFromKey)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                
                for (String userId : userIds) {
                    try {
                        refreshUserTags(userId);
                    } catch (Exception e) {
                        log.warn("[operation-log] Failed to refresh tags for user: {}", userId, e);
                    }
                }
                
                log.info("[operation-log] Batch tag refresh completed for {} users", userIds.size());
            }
        } catch (Exception e) {
            handleRedisError("refreshAllUserTags", e);
        }
    }

    /**
     * 从key中提取用户ID
     */
    private String extractUserIdFromKey(String key) {
        // key格式: operation-log:user-profile:{userId}:counts:{date}
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

    /**
     * 评估标签规则
     */
    private Set<String> evaluateTagRules(Map<String, Long> stats) {
        Set<String> tags = new HashSet<>();
        
        List<ProfileProperties.TagRule> sortedRules = properties.getTagRules().stream()
                .sorted(Comparator.comparingInt(ProfileProperties.TagRule::getPriority))
                .collect(Collectors.toList());
        
        for (ProfileProperties.TagRule rule : sortedRules) {
            if (evaluateRuleCondition(rule.getCondition(), stats)) {
                tags.add(rule.getName());
            }
        }
        
        return tags;
    }

    /**
     * 评估单个规则条件
     * 支持格式: OPERATION_TYPE > count [AND|OR] OPERATION_TYPE2 < count2
     */
    private boolean evaluateRuleCondition(String condition, Map<String, Long> stats) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }

        // 处理AND条件
        if (condition.contains(" AND ")) {
            String[] parts = condition.split(" AND ");
            for (String part : parts) {
                if (!evaluateSingleCondition(part.trim(), stats)) {
                    return false;
                }
            }
            return true;
        }

        // 处理OR条件
        if (condition.contains(" OR ")) {
            String[] parts = condition.split(" OR ");
            for (String part : parts) {
                if (evaluateSingleCondition(part.trim(), stats)) {
                    return true;
                }
            }
            return false;
        }

        // 单个条件
        return evaluateSingleCondition(condition, stats);
    }

    /**
     * 评估单个条件
     */
    private boolean evaluateSingleCondition(String condition, Map<String, Long> stats) {
        Matcher matcher = RULE_PATTERN.matcher(condition);
        if (!matcher.matches()) {
            return false;
        }

        String operationType = matcher.group(1);
        String operator = matcher.group(2);
        long threshold = Long.parseLong(matcher.group(3));
        
        long actualValue = stats.getOrDefault(operationType, 0L);

        return switch (operator) {
            case "=" -> actualValue == threshold;
            case "!=" -> actualValue != threshold;
            case ">" -> actualValue > threshold;
            case "<" -> actualValue < threshold;
            case ">=" -> actualValue >= threshold;
            case "<=" -> actualValue <= threshold;
            default -> false;
        };
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
        long totalTags = 0;
        
        try {
            Set<String> userKeys = redisTemplate.keys(properties.getRedisKeyPrefix() + ":*:counts:*");
            if (userKeys != null) {
                totalUsers = userKeys.stream()
                        .map(this::extractUserIdFromKey)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();
            }
            
            Set<String> tagKeys = redisTemplate.keys(properties.getRedisKeyPrefix() + ":tag-index:*");
            if (tagKeys != null) {
                totalTags = tagKeys.size();
            }
        } catch (Exception e) {
            log.warn("[operation-log] Failed to get status counts", e);
        }

        return new ProfileStatus(
                properties.isEnabled(),
                redisConnected,
                properties.isTagEngineEnabled(),
                fallbackActive.get(),
                totalUsers,
                totalTags
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

    // ==================== CSV导出方法实现 ====================

    @Override
    public List<List<String>> exportUserProfileToCsv(String userId) {
        List<List<String>> rows = new ArrayList<>();
        
        UserProfile profile = getUserProfile(userId);
        if (profile == null) {
            rows.add(Arrays.asList("用户ID", "标签", "操作类型", "操作次数"));
            return rows;
        }
        
        // 表头
        rows.add(Arrays.asList("用户ID", "标签", "操作类型", "操作次数"));
        
        // 用户基本信息行
        String tags = profile.tags() != null ? String.join(";", profile.tags()) : "";
        
        // 操作统计行
        if (profile.operationStats() != null && !profile.operationStats().isEmpty()) {
            for (Map.Entry<String, Long> entry : profile.operationStats().entrySet()) {
                rows.add(Arrays.asList(
                        profile.userId(),
                        tags,
                        entry.getKey(),
                        String.valueOf(entry.getValue())
                ));
            }
        } else {
            rows.add(Arrays.asList(profile.userId(), tags, "", ""));
        }
        
        return rows;
    }

    @Override
    public List<List<String>> exportUsersByTagToCsv(String tag, int page, int size) {
        List<List<String>> rows = new ArrayList<>();
        
        // 表头
        rows.add(Arrays.asList("用户ID", "标签", "ORDER_QUERY次数", "ORDER_SUBMIT次数", "ORDER_REFUND次数"));
        
        // 获取用户列表
        List<String> userIds = getUsersByTag(tag, page, size);
        
        for (String userId : userIds) {
            Map<String, Long> stats = getUserOperationStats(userId);
            
            rows.add(Arrays.asList(
                    userId,
                    tag,
                    String.valueOf(stats.getOrDefault("ORDER_QUERY", 0L)),
                    String.valueOf(stats.getOrDefault("ORDER_SUBMIT", 0L)),
                    String.valueOf(stats.getOrDefault("ORDER_REFUND", 0L))
            ));
        }
        
        return rows;
    }

    @Override
    public List<List<String>> exportAllUserStatsToCsv(int limit) {
        List<List<String>> rows = new ArrayList<>();
        
        // 表头
        rows.add(Arrays.asList("用户ID", "标签列表", "操作统计JSON"));
        
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
                    UserProfile profile = getUserProfile(userId);
                    if (profile != null) {
                        String tags = profile.tags() != null ? String.join(";", profile.tags()) : "";
                        String statsJson = profile.operationStats() != null ? 
                                profile.operationStats().toString() : "{}";
                        
                        rows.add(Arrays.asList(userId, tags, statsJson));
                    }
                }
            }
        } catch (Exception e) {
            log.error("[operation-log] Error exporting all user stats", e);
        }
        
        return rows;
    }
}
