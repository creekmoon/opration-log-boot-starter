package cn.creekmoon.operationLog.redis;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis-based 全局关联分析引擎
 * 
 * 核心功能：
 * 1. 跨副本用户行为路径（Redis Sorted Set）
 * 2. 全局异常模式检测（聚合所有副本错误日志）
 * 3. 实时数据写入（每个副本调用）
 * 4. 多维度行为分析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisInsightEngine {

    private final StringRedisTemplate redisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    // 用户行为记录最大数量（每个用户）
    private static final long MAX_USER_ACTIONS = 1000;
    
    // 异常模式检测窗口（分钟）
    private static final int ERROR_PATTERN_WINDOW_MINUTES = 5;
    
    // 异常模式阈值（相同错误次数）
    private static final int ERROR_PATTERN_THRESHOLD = 10;

    /**
     * 用户行为记录
     */
    public record UserAction(
            String userId,
            String endpoint,
            String action,
            long timestamp,
            long responseTime,
            boolean success,
            Map<String, Object> metadata
    ) {
        public UserAction {
            metadata = metadata != null ? metadata : new HashMap<>();
        }
    }

    /**
     * 错误模式
     */
    public record ErrorPattern(
            String endpoint,
            String errorType,
            int occurrenceCount,
            long firstOccurrence,
            long lastOccurrence,
            Set<String> affectedUsers
    ) {}

    /**
     * 用户行为路径
     */
    public record UserPath(
            String userId,
            List<UserAction> actions,
            long startTime,
            long endTime,
            int totalActions,
            Set<String> uniqueEndpoints
    ) {}

    /**
     * 记录用户行为到Redis时间线
     * 异步写入，不阻塞业务逻辑
     */
    @Async("logThreadPool")
    public void recordUserAction(String userId, UserAction action) {
        if (userId == null || userId.isEmpty()) {
            return;
        }
        
        try {
            String timelineKey = RedisKeyConstants.userTimelineKey(userId);
            
            // 序列化行为记录
            String actionJson = JSON.toJSONString(action);
            
            // 写入Sorted Set（按时间戳排序）
            redisTemplate.opsForZSet().add(timelineKey, actionJson, action.timestamp());
            
            // 限制每个用户的行为记录数量（保留最新的MAX_USER_ACTIONS条）
            Long currentSize = redisTemplate.opsForZSet().zCard(timelineKey);
            if (currentSize != null && currentSize > MAX_USER_ACTIONS) {
                // 删除最旧的记录
                redisTemplate.opsForZSet().removeRange(timelineKey, 0, currentSize - MAX_USER_ACTIONS - 1);
            }
            
            // 设置TTL
            redisTemplate.expire(timelineKey, Duration.ofSeconds(RedisKeyConstants.TTL_USER_TIMELINE));
            
        } catch (Exception e) {
            log.warn("记录用户行为失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 记录用户行为（简化版）
     */
    public void recordUserAction(String userId, String endpoint, String action, 
                                  long responseTime, boolean success) {
        UserAction userAction = new UserAction(
                userId, endpoint, action, System.currentTimeMillis(), 
                responseTime, success, null
        );
        recordUserAction(userId, userAction);
    }

    /**
     * 获取跨副本用户行为路径
     * 
     * @param userId 用户ID
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 用户行为列表
     */
    public List<UserAction> getUserTimeline(String userId, long startTime, long endTime) {
        try {
            String timelineKey = RedisKeyConstants.userTimelineKey(userId);
            
            // 从Sorted Set获取时间范围内的行为记录
            Set<?> actions = redisTemplate.opsForZSet().rangeByScore(
                    timelineKey, startTime, endTime);
            
            if (actions == null || actions.isEmpty()) {
                return List.of();
            }
            
            // 反序列化
            return actions.stream()
                    .map(obj -> {
                        try {
                            return JSON.parseObject(obj.toString(), UserAction.class);
                        } catch (Exception e) {
                            log.warn("解析用户行为失败: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(UserAction::timestamp))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.warn("获取用户时间线失败: userId={}, error={}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取用户完整行为路径
     * 
     * @param userId 用户ID
     * @param limit 最大返回数量
     * @return 用户行为路径
     */
    public UserPath getUserPath(String userId, int limit) {
        try {
            String timelineKey = RedisKeyConstants.userTimelineKey(userId);
            
            // 获取最新的N条记录
            Set<?> actions = redisTemplate.opsForZSet().reverseRange(timelineKey, 0, limit - 1);
            
            if (actions == null || actions.isEmpty()) {
                return new UserPath(userId, List.of(), 0, 0, 0, Set.of());
            }
            
            // 反序列化并排序（正序）
            List<UserAction> actionList = actions.stream()
                    .map(obj -> {
                        try {
                            return JSON.parseObject(obj.toString(), UserAction.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingLong(UserAction::timestamp))
                    .collect(Collectors.toList());
            
            if (actionList.isEmpty()) {
                return new UserPath(userId, List.of(), 0, 0, 0, Set.of());
            }
            
            long startTime = actionList.get(0).timestamp();
            long endTime = actionList.get(actionList.size() - 1).timestamp();
            Set<String> uniqueEndpoints = actionList.stream()
                    .map(UserAction::endpoint)
                    .collect(Collectors.toSet());
            
            return new UserPath(userId, actionList, startTime, endTime, actionList.size(), uniqueEndpoints);
            
        } catch (Exception e) {
            log.warn("获取用户路径失败: userId={}, error={}", userId, e.getMessage());
            return new UserPath(userId, List.of(), 0, 0, 0, Set.of());
        }
    }

    /**
     * 获取用户最近的行为记录
     * 
     * @param userId 用户ID
     * @param minutes 最近N分钟
     * @return 用户行为列表
     */
    public List<UserAction> getRecentUserActions(String userId, int minutes) {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (minutes * 60 * 1000L);
        return getUserTimeline(userId, startTime, endTime);
    }

    /**
     * 全局异常模式检测
     * 聚合所有副本的错误日志，检测高频错误模式
     * 
     * @return 检测到的错误模式列表
     */
    public List<ErrorPattern> detectErrorPatterns() {
        List<ErrorPattern> patterns = new ArrayList<>();
        
        try {
            // 从错误排行获取高频错误接口
            String errorRankKey = RedisKeyConstants.errorRankKey();
            Set<?> topErrors = redisTemplate.opsForZSet().reverseRange(errorRankKey, 0, 20);
            
            if (topErrors == null || topErrors.isEmpty()) {
                return patterns;
            }
            
            for (Object endpointObj : topErrors) {
                String endpoint = endpointObj.toString();
                
                // 获取错误次数
                Double errorCount = redisTemplate.opsForZSet().score(errorRankKey, endpoint);
                if (errorCount == null || errorCount < ERROR_PATTERN_THRESHOLD) {
                    continue;
                }
                
                // 构建错误模式
                ErrorPattern pattern = new ErrorPattern(
                        endpoint,
                        "HTTP_ERROR",
                        errorCount.intValue(),
                        System.currentTimeMillis() - (ERROR_PATTERN_WINDOW_MINUTES * 60 * 1000L),
                        System.currentTimeMillis(),
                        Set.of() // 可扩展：从日志中提取受影响用户
                );
                
                patterns.add(pattern);
            }
            
        } catch (Exception e) {
            log.warn("检测错误模式失败: {}", e.getMessage());
        }
        
        return patterns;
    }

    /**
     * 检测特定用户的异常行为
     * 
     * @param userId 用户ID
     * @return 是否检测到异常
     */
    public boolean detectUserAnomaly(String userId) {
        try {
            // 获取用户最近的行为
            List<UserAction> recentActions = getRecentUserActions(userId, 5);
            
            if (recentActions.isEmpty()) {
                return false;
            }
            
            // 计算错误率
            long errorCount = recentActions.stream().filter(a -> !a.success()).count();
            double errorRate = (double) errorCount / recentActions.size();
            
            // 检测高频访问（疑似爬虫）
            long timeSpan = recentActions.get(recentActions.size() - 1).timestamp() - 
                           recentActions.get(0).timestamp();
            double qps = timeSpan > 0 ? (double) recentActions.size() / (timeSpan / 1000.0) : 0;
            
            // 异常判定：错误率 > 50% 或 QPS > 10
            return errorRate > 0.5 || qps > 10;
            
        } catch (Exception e) {
            log.warn("检测用户异常失败: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取活跃用户信息
     * 
     * @param minutes 最近N分钟
     * @return 活跃用户ID列表
     */
    public List<String> getActiveUsers(int minutes) {
        // 扫描所有用户时间线Key
        String pattern = RedisKeyConstants.KEY_PREFIX + "user:*:timeline";
        
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return List.of();
            }
            
            long cutoffTime = System.currentTimeMillis() - (minutes * 60 * 1000L);
            List<String> activeUsers = new ArrayList<>();
            
            for (String key : keys) {
                // 提取userId
                String userId = key.replace(RedisKeyConstants.KEY_PREFIX + "user:", "")
                                   .replace(":timeline", "");
                
                // 检查是否有最近的活动
                Set<?> recent = redisTemplate.opsForZSet().rangeByScore(key, cutoffTime, Double.MAX_VALUE);
                if (recent != null && !recent.isEmpty()) {
                    activeUsers.add(userId);
                }
            }
            
            return activeUsers;
            
        } catch (Exception e) {
            log.warn("获取活跃用户失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 批量记录用户行为（用于批量处理）
     */
    public void batchRecordUserActions(List<UserAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        
        try {
            // 按userId分组
            Map<String, List<UserAction>> grouped = actions.stream()
                    .filter(a -> a.userId() != null && !a.userId().isEmpty())
                    .collect(Collectors.groupingBy(UserAction::userId));
            
            // 批量写入Redis Pipeline
            redisTemplate.executePipelined(new org.springframework.data.redis.core.RedisCallback<Object>() {
                @Override
                public Object doInRedis(org.springframework.data.redis.connection.RedisConnection connection) {
                    for (Map.Entry<String, List<UserAction>> entry : grouped.entrySet()) {
                        String userId = entry.getKey();
                        String timelineKey = RedisKeyConstants.userTimelineKey(userId);
                        
                        for (UserAction action : entry.getValue()) {
                            String actionJson = JSON.toJSONString(action);
                            connection.zSetCommands().zAdd(
                                    timelineKey.getBytes(), 
                                    action.timestamp(), 
                                    actionJson.getBytes()
                            );
                        }
                    }
                    return null;
                }
            });
            
            // 设置TTL
            for (String userId : grouped.keySet()) {
                String timelineKey = RedisKeyConstants.userTimelineKey(userId);
                redisTemplate.expire(timelineKey, Duration.ofSeconds(RedisKeyConstants.TTL_USER_TIMELINE));
            }
            
        } catch (Exception e) {
            log.warn("批量记录用户行为失败: {}", e.getMessage());
        }
    }

    /**
     * 清理过期数据
     * Redis TTL会自动清理，此方法用于手动清理（如测试场景）
     */
    public void cleanupExpiredData() {
        // Redis会自动处理TTL过期，无需手动清理
        log.debug("Redis会自动清理过期数据，无需手动干预");
    }
}
