package cn.creekmoon.operationLog.heatmap;

import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.core.LogThreadPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 热力图数据收集器
 * 负责收集操作日志并转发到HeatmapService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatmapCollector {

    private final HeatmapService heatmapService;
    private final HeatmapProperties properties;

    /**
     * 收集操作日志数据
     *
     * @param logRecord 日志记录
     */
    public void collect(LogRecord logRecord) {
        if (!properties.isEnabled()) {
            return;
        }

        if (logRecord == null || logRecord.getClassFullName() == null) {
            return;
        }

        // 检查排除列表
        if (logRecord.getOperationType() != null 
                && properties.getExcludeOperationTypes().contains(logRecord.getOperationType())) {
            return;
        }

        // 异步处理
        LogThreadPool.runTask(() -> {
            try {
                String classFullName = logRecord.getClassFullName();
                String[] parts = parseClassMethod(classFullName);
                if (parts == null) {
                    return;
                }

                String className = parts[0];
                String methodName = parts[1];
                String userId = logRecord.getUserId() != null ? logRecord.getUserId().toString() : null;
                LocalDateTime timestamp = logRecord.getOperationTime() != null 
                        ? logRecord.getOperationTime() : LocalDateTime.now();

                heatmapService.recordVisit(className, methodName, userId, timestamp);
            } catch (Exception e) {
                log.debug("[operation-log] Heatmap collect error: {}", e.getMessage());
            }
        });
    }

    /**
     * 解析类名和方法名
     *
     * @param classFullName 完整类方法名 (如: com.example.Service.methodName)
     * @return [className, methodName]
     */
    private String[] parseClassMethod(String classFullName) {
        if (classFullName == null || classFullName.isEmpty()) {
            return null;
        }

        int lastDot = classFullName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == classFullName.length() - 1) {
            return null;
        }

        String className = classFullName.substring(0, lastDot);
        String methodName = classFullName.substring(lastDot + 1);

        // 简化类名(只保留最后一部分)
        int lastClassDot = className.lastIndexOf('.');
        if (lastClassDot > 0) {
            className = className.substring(lastClassDot + 1);
        }

        return new String[]{className, methodName};
    }
}
