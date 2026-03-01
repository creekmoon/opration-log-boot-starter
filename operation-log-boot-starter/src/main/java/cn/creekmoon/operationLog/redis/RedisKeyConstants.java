package cn.creekmoon.operationLog.redis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis Key 常量定义
 * 统一规范所有Redis Key的生成规则
 */
public final class RedisKeyConstants {
    
    private RedisKeyConstants() {
        // 工具类，禁止实例化
    }
    
    /** Key前缀 */
    public static final String KEY_PREFIX = "log:";
    
    /** 日期格式 */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_MINUTE_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    
    // ==================== 接口统计 (Hash) ====================
    
    /**
     * 接口统计Key
     * Format: log:stat:{date}:{endpoint}
     * Type: Hash
     * TTL: 7天
     */
    public static String statKey(String endpoint) {
        return KEY_PREFIX + "stat:" + LocalDate.now().format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint);
    }
    
    /**
     * 接口统计Key（指定日期）
     */
    public static String statKey(String endpoint, LocalDate date) {
        return KEY_PREFIX + "stat:" + date.format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint);
    }
    
    // ==================== 响应时间分布 (Sorted Set) ====================
    
    /**
     * 响应时间分布Key
     * Format: log:latency:{date}:{endpoint}
     * Type: Sorted Set
     * TTL: 1天
     */
    public static String latencyKey(String endpoint) {
        return KEY_PREFIX + "latency:" + LocalDate.now().format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint);
    }
    
    /**
     * 响应时间分布Key（指定日期）
     */
    public static String latencyKey(String endpoint, LocalDate date) {
        return KEY_PREFIX + "latency:" + date.format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint);
    }
    
    // ==================== 用户时间线 (Sorted Set) ====================
    
    /**
     * 用户时间线Key
     * Format: log:user:{userId}:timeline
     * Type: Sorted Set
     * TTL: 7天
     */
    public static String userTimelineKey(String userId) {
        return KEY_PREFIX + "user:" + userId + ":timeline";
    }
    
    // ==================== 全局UV (HyperLogLog) ====================
    
    /**
     * UV统计Key
     * Format: log:uv:{date}:{endpoint}
     * Type: HyperLogLog
     * TTL: 7天
     */
    public static String uvKey(String endpoint) {
        return KEY_PREFIX + "uv:" + LocalDate.now().format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint);
    }
    
    /**
     * UV统计Key（指定日期）
     */
    public static String uvKey(String endpoint, LocalDate date) {
        return KEY_PREFIX + "uv:" + date.format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint);
    }
    
    // ==================== 全局QPS (String) ====================
    
    /**
     * QPS统计Key（分钟级）
     * Format: log:qps:{date}:{endpoint}:{HHmm}
     * Type: String
     * TTL: 1分钟
     */
    public static String qpsKey(String endpoint) {
        LocalDate now = LocalDate.now();
        String timeStr = java.time.LocalTime.now().format(HOUR_MINUTE_FORMAT);
        return KEY_PREFIX + "qps:" + now.format(DATE_FORMAT) + ":" + sanitizeEndpoint(endpoint) + ":" + timeStr;
    }
    
    // ==================== 错误排行 (Sorted Set) ====================
    
    /**
     * 错误排行Key
     * Format: log:error:{date}:rank
     * Type: Sorted Set
     * TTL: 1天
     */
    public static String errorRankKey() {
        return KEY_PREFIX + "error:" + LocalDate.now().format(DATE_FORMAT) + ":rank";
    }
    
    /**
     * 错误排行Key（指定日期）
     */
    public static String errorRankKey(LocalDate date) {
        return KEY_PREFIX + "error:" + date.format(DATE_FORMAT) + ":rank";
    }
    
    // ==================== 全局P99 (Sorted Set) ====================
    
    /**
     * 全局P99响应时间Key
     * Format: log:p99:{date}:global
     * Type: Sorted Set
     * TTL: 1天
     */
    public static String p99GlobalKey() {
        return KEY_PREFIX + "p99:" + LocalDate.now().format(DATE_FORMAT) + ":global";
    }
    
    /**
     * 全局P99响应时间Key（指定日期）
     */
    public static String p99GlobalKey(LocalDate date) {
        return KEY_PREFIX + "p99:" + date.format(DATE_FORMAT) + ":global";
    }
    
    // ==================== 实例心跳 (Hash) ====================
    
    /**
     * 实例心跳Key
     * Format: log:instances:heartbeat
     * Type: Hash
     */
    public static String instanceHeartbeatKey() {
        return KEY_PREFIX + "instances:heartbeat";
    }
    
    /**
     * 实例采样率Key
     * Format: log:instances:samplerate
     * Type: Hash
     */
    public static String instanceSampleRateKey() {
        return KEY_PREFIX + "instances:samplerate";
    }
    
    // ==================== 降级队列 (List) ====================
    
    /**
     * 降级数据队列Key
     * Format: log:queue:{instanceId}
     * Type: List
     * TTL: 1天
     */
    public static String fallbackQueueKey(String instanceId) {
        return KEY_PREFIX + "queue:" + instanceId;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 清理端点名称中的特殊字符
     */
    private static String sanitizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return "unknown";
        }
        // 替换冒号和斜杠，避免Key冲突
        return endpoint.replace(":", "_").replace("/", "_").replace(" ", "_");
    }
    
    // ==================== TTL常量（秒） ====================
    
    public static final long TTL_STAT = 7 * 24 * 60 * 60;      // 7天
    public static final long TTL_LATENCY = 24 * 60 * 60;        // 1天
    public static final long TTL_USER_TIMELINE = 7 * 24 * 60 * 60; // 7天
    public static final long TTL_UV = 7 * 24 * 60 * 60;        // 7天
    public static final long TTL_QPS = 60;                      // 1分钟
    public static final long TTL_ERROR_RANK = 24 * 60 * 60;     // 1天
    public static final long TTL_P99 = 24 * 60 * 60;            // 1天
    public static final long TTL_FALLBACK_QUEUE = 24 * 60 * 60; // 1天
}
