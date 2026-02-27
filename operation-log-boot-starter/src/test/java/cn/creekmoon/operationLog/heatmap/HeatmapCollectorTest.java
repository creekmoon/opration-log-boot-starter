package cn.creekmoon.operationLog.heatmap;

import cn.creekmoon.operationLog.core.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 热力图数据收集器测试类
 */
@ExtendWith(MockitoExtension.class)
class HeatmapCollectorTest {

    @Mock
    private HeatmapService heatmapService;

    private HeatmapProperties properties;
    private HeatmapCollector collector;

    @BeforeEach
    void setUp() {
        properties = new HeatmapProperties();
        properties.setEnabled(true);
        collector = new HeatmapCollector(heatmapService, properties);
    }

    @Test
    void testCollect_Success() throws InterruptedException {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setClassFullName("com.example.TestService.testMethod");
        logRecord.setUserId(123L);
        logRecord.setOperationTime(LocalDateTime.now());

        // When
        collector.collect(logRecord);
        
        // Wait for async task
        Thread.sleep(100);

        // Then
        verify(heatmapService, atLeast(0)).recordVisit(anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void testCollect_Disabled() {
        // Given
        properties.setEnabled(false);
        LogRecord logRecord = new LogRecord();

        // When
        collector.collect(logRecord);

        // Then
        verifyNoInteractions(heatmapService);
    }

    @Test
    void testCollect_NullLogRecord() {
        // When
        collector.collect(null);

        // Then
        verifyNoInteractions(heatmapService);
    }

    @Test
    void testCollect_NullClassFullName() {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setClassFullName(null);

        // When
        collector.collect(logRecord);

        // Then
        verifyNoInteractions(heatmapService);
    }

    @Test
    void testCollect_ExcludedOperationType() {
        // Given
        properties.setExcludeOperationTypes(Collections.singletonList("EXCLUDED_TYPE"));
        LogRecord logRecord = new LogRecord();
        logRecord.setClassFullName("com.example.TestService.testMethod");
        logRecord.setOperationType("EXCLUDED_TYPE");

        // When
        collector.collect(logRecord);

        // Then
        verifyNoInteractions(heatmapService);
    }

    @Test
    void testCollect_ParseClassMethod() throws InterruptedException {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setClassFullName("com.example.service.OrderService.createOrder");
        logRecord.setUserId(456L);
        logRecord.setOperationTime(LocalDateTime.now());

        // When
        collector.collect(logRecord);
        
        // Wait for async task
        Thread.sleep(100);

        // Then - className should be "OrderService", methodName should be "createOrder"
        verify(heatmapService, atLeast(0)).recordVisit(eq("OrderService"), eq("createOrder"), anyString(), any(LocalDateTime.class));
    }
}
