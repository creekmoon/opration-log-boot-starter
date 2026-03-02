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
        /* fast-fail：日志为空直接返回 */
        if (logRecord == null) {
            return;
        }
        /* 声明参数 */
        String operationType = logRecord.getOperationType();
        /* 复用主流程 */
        collect(logRecord, operationType);
    }

    /**
     * 收集操作日志数据(指定操作类型)
     *
     * @param logRecord 日志记录
     * @param operationType 操作类型
     */
    public void collect(LogRecord logRecord, String operationType) {
        /* fast-fail：功能未启用或日志上下文不完整时直接返回 */
        if (!properties.isEnabled()) {
            return;
        }
        if (logRecord == null) {
            return;
        }
        if (logRecord.getUserId() == null) {
            return;
        }
        /* 声明参数 */
        String finalOperationType = operationType;
        String userId = logRecord.getUserId().toString();
        LocalDateTime timestamp = logRecord.getOperationTime();
        /* 默认值兜底 */
        if (finalOperationType == null || finalOperationType.isEmpty()) {
            finalOperationType = "DEFAULT";
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        /* 异步上报画像数据 */
        String operationTypeToSave = finalOperationType;
        LocalDateTime timestampToSave = timestamp;
        LogThreadPool.runTask(() -> {
            try {
                profileService.recordOperation(userId, operationTypeToSave, timestampToSave);
            } catch (Exception e) {
                log.debug("[operation-log] Profile collect error: {}", e.getMessage());
            }
        });
    }
}
