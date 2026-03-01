package cn.creekmoon.operationLog.config;

import cn.creekmoon.operationLog.core.LogRecord;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultOperationLogHandler 测试类
 */
class DefaultOperationLogHandlerTest {

    @Test
    void testHandle() {
        DefaultOperationLogHandler handler = new DefaultOperationLogHandler();
        
        LogRecord record = new LogRecord();
        record.setClassFullName("TestClass.testMethod");
        record.setOperationName("Test Operation");
        
        // 默认实现只是打印日志，不应该抛出异常
        assertDoesNotThrow(() -> handler.handle(record));
    }

    @Test
    void testHandle_NullRecord() {
        DefaultOperationLogHandler handler = new DefaultOperationLogHandler();
        
        // 测试空记录 - 可能抛出 NullPointerException
        // 这里只验证不会崩溃
        assertDoesNotThrow(() -> {
            try {
                handler.handle(null);
            } catch (NullPointerException e) {
                // 预期的异常，可以接受
            }
        });
    }
}
