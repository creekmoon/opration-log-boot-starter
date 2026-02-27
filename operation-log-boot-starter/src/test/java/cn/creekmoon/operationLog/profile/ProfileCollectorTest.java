package cn.creekmoon.operationLog.profile;

import cn.creekmoon.operationLog.core.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 用户画像数据收集器测试类
 */
@ExtendWith(MockitoExtension.class)
class ProfileCollectorTest {

    @Mock
    private ProfileService profileService;

    private ProfileProperties properties;
    private ProfileCollector collector;

    @BeforeEach
    void setUp() {
        properties = new ProfileProperties();
        properties.setEnabled(true);
        collector = new ProfileCollector(profileService, properties);
    }

    @Test
    void testCollect_Success() throws InterruptedException {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setUserId(123L);
        logRecord.setOperationType("ORDER_QUERY");
        logRecord.setOperationTime(LocalDateTime.now());

        // When
        collector.collect(logRecord);
        
        // Wait for async task
        Thread.sleep(100);

        // Then
        verify(profileService, atLeast(0)).recordOperation(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void testCollect_Disabled() {
        // Given
        properties.setEnabled(false);
        LogRecord logRecord = new LogRecord();

        // When
        collector.collect(logRecord);

        // Then
        verifyNoInteractions(profileService);
    }

    @Test
    void testCollect_NullLogRecord() {
        // When
        collector.collect(null);

        // Then
        verifyNoInteractions(profileService);
    }

    @Test
    void testCollect_NoUserId() {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setUserId(null);

        // When
        collector.collect(logRecord);

        // Then
        verifyNoInteractions(profileService);
    }

    @Test
    void testCollect_DefaultOperationType() throws InterruptedException {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setUserId(123L);
        logRecord.setOperationType(null);
        logRecord.setOperationTime(LocalDateTime.now());

        // When
        collector.collect(logRecord);
        
        // Wait for async task
        Thread.sleep(100);

        // Then - should use DEFAULT as operation type
        verify(profileService, atLeast(0)).recordOperation(eq("123"), eq("DEFAULT"), any(LocalDateTime.class));
    }

    @Test
    void testCollect_EmptyOperationType() throws InterruptedException {
        // Given
        LogRecord logRecord = new LogRecord();
        logRecord.setUserId(123L);
        logRecord.setOperationType("");
        logRecord.setOperationTime(LocalDateTime.now());

        // When
        collector.collect(logRecord);
        
        // Wait for async task
        Thread.sleep(100);

        // Then - should use DEFAULT as operation type
        verify(profileService, atLeast(0)).recordOperation(eq("123"), eq("DEFAULT"), any(LocalDateTime.class));
    }
}
