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

        // 异步处理
        LogThreadPool.runTask(() -> {
            try {
                String userId = logRecord.getUserId().toString();
                String operationType = logRecord.getOperationType();
                
                // 如果没有操作类型,使用默认值
                if (operationType == null || operationType.isEmpty()) {
                    operationType = "DEFAULT";
                }

                LocalDateTime timestamp = logRecord.getOperationTime() != null 
                        ? logRecord.getOperationTime() : LocalDateTime.now();

                profileService.recordOperation(userId, operationType, timestamp);
            } catch (Exception e) {
                log.debug("[operation-log] Profile collect error: {}", e.getMessage());
            }
        });
    }
}
