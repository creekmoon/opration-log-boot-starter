package cn.creekmoon.operationLog.core;

import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OperationLogContext 单元测试
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperationLogContextTest {

    @BeforeEach
    void setUp() {
        // 确保上下文在测试开始前是干净的
        OperationLogContext.disable = false;
        OperationLogContext.clean();
    }

    @AfterEach
    void tearDown() {
        // 测试结束后清理上下文
        OperationLogContext.clean();
        OperationLogContext.disable = false;
    }

    @Test
    @Order(1)
    @DisplayName("测试禁用状态下 follow 方法不执行")
    void testFollowWhenDisabled() {
        OperationLogContext.disable = true;
        
        @SuppressWarnings("unchecked")
        Callable<Object> mockMetadata = mock(Callable.class);
        
        // 不应抛出异常
        assertDoesNotThrow(() -> OperationLogContext.follow(mockMetadata));
        
        // verify 不应被调用
        try {
            verify(mockMetadata, never()).call();
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    @Order(2)
    @DisplayName("测试 markFail 方法 - 无当前记录时不应抛出异常")
    void testMarkFailWithoutRecord() {
        // 确保没有当前记录
        OperationLogContext.clean();
        
        assertDoesNotThrow(() -> OperationLogContext.markFail());
    }

    @Test
    @Order(3)
    @DisplayName("测试 getCurrentLogRecord 返回 null 当没有记录时")
    void testGetCurrentLogRecordWhenEmpty() {
        LogRecord record = OperationLogContext.getCurrentLogRecord();
        assertNull(record, "没有设置记录时应返回 null");
    }

    @Test
    @Order(4)
    @DisplayName("测试 addTags 方法 - 禁用状态下不执行")
    void testAddTagsWhenDisabled() {
        OperationLogContext.disable = true;
        
        // 不应抛出异常
        assertDoesNotThrow(() -> OperationLogContext.addTags("tag1", "tag2"));
    }

    @Test
    @Order(5)
    @DisplayName("测试 addTags 方法 - 空标签不添加")
    void testAddTagsWithEmptyTags() {
        OperationLogContext.disable = false;
        
        // 创建模拟记录
        String recordId = "test-record-1";
        LogRecord mockRecord = new LogRecord();
        OperationLogContext.recordId2Logs.put(recordId, mockRecord);
        OperationLogContext.currentRecordId.set(recordId);
        
        // 添加空标签
        OperationLogContext.addTags((String[]) null);
        OperationLogContext.addTags("", "  ");
        
        assertEquals(0, mockRecord.getTags().size(), "空标签不应被添加");
    }

    @Test
    @Order(6)
    @DisplayName("测试 addTags 方法 - 正常添加标签")
    void testAddTagsNormal() {
        OperationLogContext.disable = false;
        
        String recordId = "test-record-2";
        LogRecord mockRecord = new LogRecord();
        OperationLogContext.recordId2Logs.put(recordId, mockRecord);
        OperationLogContext.currentRecordId.set(recordId);
        
        OperationLogContext.addTags("important", "order", " test ");
        
        assertEquals(3, mockRecord.getTags().size());
        assertTrue(mockRecord.getTags().contains("important"));
        assertTrue(mockRecord.getTags().contains("order"));
        assertTrue(mockRecord.getTags().contains("test"), "标签应被 trim");
    }

    @Test
    @Order(7)
    @DisplayName("测试 addRemarks 方法 - 禁用状态下不执行")
    void testAddRemarksWhenDisabled() {
        OperationLogContext.disable = true;
        
        assertDoesNotThrow(() -> OperationLogContext.addRemarks("remark1"));
    }

    @Test
    @Order(8)
    @DisplayName("测试 addRemarks 方法 - 空备注不添加")
    void testAddRemarksWithEmptyRemarks() {
        OperationLogContext.disable = false;
        
        String recordId = "test-record-3";
        LogRecord mockRecord = new LogRecord();
        OperationLogContext.recordId2Logs.put(recordId, mockRecord);
        OperationLogContext.currentRecordId.set(recordId);
        
        // 添加空备注
        OperationLogContext.addRemarks((String[]) null);
        
        assertEquals(0, mockRecord.getRemarks().size());
    }

    @Test
    @Order(9)
    @DisplayName("测试 addRemarks 方法 - 正常添加备注")
    void testAddRemarksNormal() {
        OperationLogContext.disable = false;
        
        String recordId = "test-record-4";
        LogRecord mockRecord = new LogRecord();
        OperationLogContext.recordId2Logs.put(recordId, mockRecord);
        OperationLogContext.currentRecordId.set(recordId);
        
        OperationLogContext.addRemarks("用户操作", " 需要审核 ");
        
        assertEquals(2, mockRecord.getRemarks().size());
        assertTrue(mockRecord.getRemarks().contains("用户操作"));
        assertTrue(mockRecord.getRemarks().contains("需要审核"), "备注应被 trim");
    }

    @Test
    @Order(10)
    @DisplayName("测试 clean 方法清理所有 ThreadLocal")
    void testCleanRemovesAll() {
        String recordId = "test-record-5";
        LogRecord mockRecord = new LogRecord();
        OperationLogContext.recordId2Logs.put(recordId, mockRecord);
        OperationLogContext.currentRecordId.set(recordId);
        
        ServletRequest mockRequest = mock(ServletRequest.class);
        OperationLogContext.currentServletRequest.set(mockRequest);
        
        // 清理前验证存在
        assertNotNull(OperationLogContext.getCurrentLogRecord());
        
        // 执行清理
        OperationLogContext.clean();
        
        // 清理后验证
        assertNull(OperationLogContext.getCurrentLogRecord());
        assertNull(OperationLogContext.currentRecordId.get());
        assertNull(OperationLogContext.currentServletRequest.get());
    }

    @Test
    @Order(11)
    @DisplayName("测试并发环境下的 recordId2Logs 安全性")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int iterations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        String recordId = "thread-" + threadIndex + "-record-" + j;
                        LogRecord record = new LogRecord();
                        record.setUserId((long) threadIndex);
                        
                        OperationLogContext.recordId2Logs.put(recordId, record);
                        LogRecord retrieved = OperationLogContext.recordId2Logs.get(recordId);
                        
                        if (retrieved != null && retrieved.getUserId() == threadIndex) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount * iterations, successCount.get(), 
            "所有并发操作都应成功");
        
        // 清理
        OperationLogContext.recordId2Logs.clear();
    }

    @Test
    @Order(12)
    @DisplayName("测试 markFail 方法标记记录为失败")
    void testMarkFail() {
        String recordId = "test-record-fail";
        LogRecord mockRecord = new LogRecord();
        mockRecord.setRequestResult(true);
        
        OperationLogContext.recordId2Logs.put(recordId, mockRecord);
        OperationLogContext.currentRecordId.set(recordId);
        
        assertTrue(mockRecord.getRequestResult(), "初始状态应为成功");
        
        OperationLogContext.markFail();
        
        assertFalse(mockRecord.getRequestResult(), "标记后应为失败");
    }
}
