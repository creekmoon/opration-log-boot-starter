package cn.creekmoon.operationLog.profile;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.LogThreadPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 用户行为画像数据收集器
 * 负责收集操作日志并转发到ProfileService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileCollector {

    private final ProfileService profileService;
    private final ProfileProperties properties;

    /**
     * 收集操作日志数据
     *
     * @param logRecord 日志记录
     */
    public void collect(LogRecord logRecord) {
        collect(logRecord, logRecord != null ? logRecord.getOperationType() : null);
    }

    /**
     * 收集操作日志数据(指定操作类型)
     *
     * @param logRecord 日志记录
     * @param operationType 操作类型
     */
    public void collect(LogRecord logRecord, String operationType) {
        if (!properties.isEnabled()) {
            return;
        }

        if (logRecord == null) {
            return;
        }

        // 必须有用户ID
        if (logRecord.getUserId() == null) {
            return;
        }

        // 使用final变量供lambda使用
        final String finalOperationType = operationType != null && !operationType.isEmpty() 
                ? operationType : "DEFAULT";
        final String userId = logRecord.getUserId().toString();
        final LocalDateTime timestamp = logRecord.getOperationTime() != null 
                ? logRecord.getOperationTime() : LocalDateTime.now();

        // 异步处理
        LogThreadPool.runTask(() -> {
            try {
                profileService.recordOperation(userId, finalOperationType, timestamp);
            } catch (Exception e) {
                log.debug("[operation-log] Profile collect error: {}", e.getMessage());
            }
        });
    }
}
