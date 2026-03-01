package cn.creekmoon.operationLog.redis;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis故障降级管理器
 * 
 * 核心功能：
 * 1. Redis连接健康检查
 * 2. 每个副本独立降级，但尽可能保持运行
 * 3. 降级时回退到本地固定采样率
 * 4. 数据暂存本地队列，恢复后批量上报
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFailoverManager {

    private final StringRedisTemplate redisTemplate;
    
    // 实例ID
    @Getter
    private final String instanceId = generateInstanceId();
    
    // 降级状态
    @Getter
    private final AtomicBoolean fallbackMode = new AtomicBoolean(false);
    
    // 降级采样率
    private static final double FALLBACK_SAMPLE_RATE = 0.1;
    
    // 正常采样率（可配置）
    private volatile double normalSampleRate = 1.0;
    
    // 健康检查配置
    private static final int HEALTH_CHECK_INTERVAL_MS = 5000;
    private static final int HEALTH_CHECK_TIMEOUT_MS = 3000;
    private static final int FAILURE_THRESHOLD = 3;
    
    // 本地队列配置
    private static final int MAX_QUEUE_SIZE = 10000;
    private static final int BATCH_FLUSH_SIZE = 500;
    private static final long QUEUE_TTL_MS = 24 * 60 * 60 * 1000; // 24小时
    
    // 失败计数器
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long lastSuccessTime = System.currentTimeMillis();
    
    // 本地队列（降级时暂存数据）
    private final BlockingQueue<QueuedData> fallbackQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    
    // 健康检查调度器
    private ScheduledExecutorService healthCheckScheduler;
    
    // 恢复上报调度器
    private ScheduledExecutorService flushScheduler;

    /**
     * 队列数据
     */
    public record QueuedData(
            DataType type,
            String endpoint,
            Long responseTime,
            Boolean success,
            String userId,
            Long timestamp,
            String payload
    ) {
        public enum DataType {
            METRIC, USER_ACTION, HEARTBEAT
        }
    }

    @PostConstruct
    public void init() {
        log.info("RedisFailoverManager 初始化完成，实例ID: {}", instanceId);
        
        // 启动健康检查
        startHealthCheck();
        
        // 启动恢复上报
        startFlushScheduler();
        
        // 注册实例心跳
        registerHeartbeat();
    }

    @PreDestroy
    public void destroy() {
        log.info("RedisFailoverManager 正在关闭...");
        
        if (healthCheckScheduler != null) {
            healthCheckScheduler.shutdown();
        }
        
        if (flushScheduler != null) {
            flushScheduler.shutdown();
        }
        
        // 尝试最后刷新一次队列
        if (!fallbackQueue.isEmpty()) {
            flushQueuedData();
        }
        
        // 注销实例
        unregisterHeartbeat();
    }

    /**
     * 启动健康检查
     */
    private void startHealthCheck() {
        healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "redis-health-check");
            t.setDaemon(true);
            return t;
        });
        
        healthCheckScheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean healthy = checkRedisHealth();
                
                if (healthy) {
                    failureCount.set(0);
                    lastSuccessTime = System.currentTimeMillis();
                    
                    // 如果处于降级模式，检查是否恢复
                    if (fallbackMode.get()) {
                        exitFallbackMode();
                    }
                } else {
                    int failures = failureCount.incrementAndGet();
                    
                    // 连续失败超过阈值，进入降级模式
                    if (failures >= FAILURE_THRESHOLD && !fallbackMode.get()) {
                        enterFallbackMode();
                    }
                }
            } catch (Exception e) {
                log.warn("健康检查异常: {}", e.getMessage());
            }
        }, HEALTH_CHECK_INTERVAL_MS, HEALTH_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 启动恢复上报调度器
     */
    private void startFlushScheduler() {
        flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "redis-flush-scheduler");
            t.setDaemon(true);
            return t;
        });
        
        flushScheduler.scheduleWithFixedDelay(() -> {
            try {
                // 如果Redis健康且队列有数据，尝试恢复
                if (!fallbackMode.get() && !fallbackQueue.isEmpty()) {
                    flushQueuedData();
                }
            } catch (Exception e) {
                log.warn("恢复上报异常: {}", e.getMessage());
            }
        }, 10000, 10000, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查Redis健康状态
     */
    public boolean isRedisHealthy() {
        try {
            return checkRedisHealth();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查Redis健康状态
     */
    private boolean checkRedisHealth() {
        try {
            // 使用超时控制
            Future<Boolean> future = Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    String pong = redisTemplate.execute((connection) -> {
                        return connection.ping();
                    });
                    return "PONG".equalsIgnoreCase(pong);
                } catch (Exception e) {
                    return false;
                }
            });
            
            return future.get(HEALTH_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            
        } catch (TimeoutException e) {
            log.warn("Redis健康检查超时");
            return false;
        } catch (Exception e) {
            log.warn("Redis健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 进入降级模式
     */
    public void enterFallbackMode() {
        if (fallbackMode.compareAndSet(false, true)) {
            log.warn("Redis不可用，进入降级模式。实例ID: {}", instanceId);
            
            // 通知其他组件
            onFallbackModeEntered();
        }
    }

    /**
     * 退出降级模式
     */
    public void exitFallbackMode() {
        if (fallbackMode.compareAndSet(true, false)) {
            log.info("Redis已恢复，退出降级模式。实例ID: {}", instanceId);
            
            // 立即尝试恢复队列数据
            flushQueuedData();
            
            // 通知其他组件
            onFallbackModeExited();
        }
    }

    /**
     * 降级模式进入回调
     */
    protected void onFallbackModeEntered() {
        // 子类可覆盖
    }

    /**
     * 降级模式退出回调
     */
    protected void onFallbackModeExited() {
        // 子类可覆盖
    }

    /**
     * 获取当前采样率
     */
    public double getCurrentSampleRate() {
        return fallbackMode.get() ? FALLBACK_SAMPLE_RATE : normalSampleRate;
    }

    /**
     * 设置正常采样率
     */
    public void setNormalSampleRate(double rate) {
        this.normalSampleRate = Math.max(0.01, Math.min(1.0, rate));
    }

    /**
     * 数据暂存本地队列
     */
    public boolean queueForLater(QueuedData data) {
        if (data == null) {
            return false;
        }
        
        // 如果队列已满，丢弃最旧的数据
        if (fallbackQueue.size() >= MAX_QUEUE_SIZE) {
            QueuedData discarded = fallbackQueue.poll();
            if (discarded != null) {
                log.debug("队列已满，丢弃旧数据: {}", discarded.type());
            }
        }
        
        boolean offered = fallbackQueue.offer(data);
        if (offered) {
            log.debug("数据已暂存队列，当前大小: {}", fallbackQueue.size());
        }
        return offered;
    }

    /**
     * 数据暂存本地队列（简化版）
     */
    public boolean queueMetric(String endpoint, long responseTime, boolean success, String userId) {
        QueuedData data = new QueuedData(
                QueuedData.DataType.METRIC,
                endpoint,
                responseTime,
                success,
                userId,
                System.currentTimeMillis(),
                null
        );
        return queueForLater(data);
    }

    /**
     * 恢复后批量上报
     */
    public void flushQueuedData() {
        if (fallbackQueue.isEmpty()) {
            return;
        }
        
        if (fallbackMode.get()) {
            log.debug("仍处于降级模式，无法上报队列数据");
            return;
        }
        
        List<QueuedData> batch = new ArrayList<>();
        int count = 0;
        
        // 取出批量数据
        while (count < BATCH_FLUSH_SIZE && !fallbackQueue.isEmpty()) {
            QueuedData data = fallbackQueue.poll();
            if (data == null) {
                break;
            }
            
            // 检查数据是否过期
            if (System.currentTimeMillis() - data.timestamp() > QUEUE_TTL_MS) {
                continue; // 跳过过期数据
            }
            
            batch.add(data);
            count++;
        }
        
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            // 批量上报（实际实现依赖具体的Collector）
            log.info("正在上报队列数据: {} 条", batch.size());
            
            // 上报成功后，继续处理下一批
            if (!fallbackQueue.isEmpty()) {
                // 延迟执行下一批，避免阻塞
                flushScheduler.schedule(this::flushQueuedData, 1, TimeUnit.SECONDS);
            }
            
        } catch (Exception e) {
            log.warn("批量上报失败: {}", e.getMessage());
            // 重新放入队列
            batch.forEach(this::queueForLater);
        }
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return fallbackQueue.size();
    }

    /**
     * 清空队列
     */
    public void clearQueue() {
        fallbackQueue.clear();
        log.info("已清空降级队列");
    }

    /**
     * 注册实例心跳
     */
    private void registerHeartbeat() {
        try {
            String heartbeatKey = RedisKeyConstants.instanceHeartbeatKey();
            redisTemplate.opsForHash().put(heartbeatKey, instanceId, 
                    String.valueOf(System.currentTimeMillis()));
            redisTemplate.expire(heartbeatKey, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.debug("注册心跳失败: {}", e.getMessage());
        }
    }

    /**
     * 注销实例
     */
    private void unregisterHeartbeat() {
        try {
            String heartbeatKey = RedisKeyConstants.instanceHeartbeatKey();
            redisTemplate.opsForHash().delete(heartbeatKey, instanceId);
        } catch (Exception e) {
            log.debug("注销心跳失败: {}", e.getMessage());
        }
    }

    /**
     * 生成实例ID
     */
    private static String generateInstanceId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return host + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (UnknownHostException e) {
            return "unknown-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }

    /**
     * 安全执行Redis操作（自动降级）
     */
    public <T> T executeWithFallback(RedisOperation<T> operation, T fallbackValue) {
        if (fallbackMode.get()) {
            return fallbackValue;
        }
        
        try {
            T result = operation.execute();
            failureCount.set(0);
            return result;
        } catch (Exception e) {
            failureCount.incrementAndGet();
            
            // 检查是否需要进入降级模式
            if (failureCount.get() >= FAILURE_THRESHOLD) {
                enterFallbackMode();
            }
            
            return fallbackValue;
        }
    }

    /**
     * Redis操作接口
     */
    @FunctionalInterface
    public interface RedisOperation<T> {
        T execute() throws Exception;
    }
}
