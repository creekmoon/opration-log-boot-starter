package cn.creekmoon.operationLog.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogRecord 单元测试
 * 测试日志记录对象的创建、数据设置和转换功能
 */
class LogRecordTest {

    @Test
    void testDefaultValues() {
        LogRecord logRecord = new LogRecord();
        
        // 验证默认值
        assertTrue(logRecord.getRequestResult(), "默认请求结果应为 true");
        assertNotNull(logRecord.getOperationTime(), "默认操作时间不应为空");
        assertNotNull(logRecord.getTags(), "默认标签集合不应为空");
        assertNotNull(logRecord.getRemarks(), "默认备注集合不应为空");
        assertTrue(logRecord.getTags() instanceof LinkedHashSet, "标签应为 LinkedHashSet");
        assertTrue(logRecord.getRemarks() instanceof LinkedHashSet, "备注应为 LinkedHashSet");
    }

    @Test
    void testSetAndGetUserId() {
        LogRecord logRecord = new LogRecord();
        Long userId = 12345L;
        
        logRecord.setUserId(userId);
        
        assertEquals(userId, logRecord.getUserId());
    }

    @Test
    void testSetAndGetUserName() {
        LogRecord logRecord = new LogRecord();
        String userName = "测试用户";
        
        logRecord.setUserName(userName);
        
        assertEquals(userName, logRecord.getUserName());
    }

    @Test
    void testSetAndGetUserAccountId() {
        LogRecord logRecord = new LogRecord();
        Long accountId = 99999L;
        
        logRecord.setUserAccountId(accountId);
        
        assertEquals(accountId, logRecord.getUserAccountId());
    }

    @Test
    void testSetAndGetUserAccountName() {
        LogRecord logRecord = new LogRecord();
        String accountName = "test_account";
        
        logRecord.setUserAccountName(accountName);
        
        assertEquals(accountName, logRecord.getUserAccountName());
    }

    @Test
    void testSetAndGetProjectName() {
        LogRecord logRecord = new LogRecord();
        String projectName = "测试项目";
        
        logRecord.setProjectName(projectName);
        
        assertEquals(projectName, logRecord.getProjectName());
    }

    @Test
    void testSetAndGetOperationType() {
        LogRecord logRecord = new LogRecord();
        String operationType = "CREATE";
        
        logRecord.setOperationType(operationType);
        
        assertEquals(operationType, logRecord.getOperationType());
    }

    @Test
    void testSetAndGetOperationName() {
        LogRecord logRecord = new LogRecord();
        String operationName = "创建用户";
        
        logRecord.setOperationName(operationName);
        
        assertEquals(operationName, logRecord.getOperationName());
    }

    @Test
    void testSetAndGetMethodName() {
        LogRecord logRecord = new LogRecord();
        String methodName = "createUser";
        
        logRecord.setMethodName(methodName);
        
        assertEquals(methodName, logRecord.getMethodName());
    }

    @Test
    void testSetAndGetClassFullName() {
        LogRecord logRecord = new LogRecord();
        String classFullName = "com.example.UserService.createUser";
        
        logRecord.setClassFullName(classFullName);
        
        assertEquals(classFullName, logRecord.getClassFullName());
    }

    @Test
    void testSetAndGetRequestResult() {
        LogRecord logRecord = new LogRecord();
        
        logRecord.setRequestResult(false);
        assertFalse(logRecord.getRequestResult());
        
        logRecord.setRequestResult(true);
        assertTrue(logRecord.getRequestResult());
    }

    @Test
    void testSetAndGetRequestParams() {
        LogRecord logRecord = new LogRecord();
        JSONArray params = new JSONArray();
        params.add("param1");
        params.add(123);
        
        logRecord.setRequestParams(params);
        
        assertEquals(params, logRecord.getRequestParams());
        assertEquals(2, logRecord.getRequestParams().size());
    }

    @Test
    void testSetAndGetPreValue() {
        LogRecord logRecord = new LogRecord();
        JSONObject preValue = new JSONObject();
        preValue.put("id", 1);
        preValue.put("name", "before");
        
        logRecord.setPreValue(preValue);
        
        assertEquals(preValue, logRecord.getPreValue());
    }

    @Test
    void testSetAndGetAfterValue() {
        LogRecord logRecord = new LogRecord();
        JSONObject afterValue = new JSONObject();
        afterValue.put("id", 1);
        afterValue.put("name", "after");
        
        logRecord.setAfterValue(afterValue);
        
        assertEquals(afterValue, logRecord.getAfterValue());
    }

    @Test
    void testSetAndGetOperationTime() {
        LogRecord logRecord = new LogRecord();
        LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        
        logRecord.setOperationTime(now);
        
        assertEquals(now, logRecord.getOperationTime());
    }

    @Test
    void testTagsInitialization() {
        LogRecord logRecord = new LogRecord();
        
        // 验证可以添加标签
        logRecord.getTags().add("tag1");
        logRecord.getTags().add("tag2");
        
        assertEquals(2, logRecord.getTags().size());
        assertTrue(logRecord.getTags().contains("tag1"));
        assertTrue(logRecord.getTags().contains("tag2"));
    }

    @Test
    void testRemarksInitialization() {
        LogRecord logRecord = new LogRecord();
        
        // 验证可以添加备注
        logRecord.getRemarks().add("remark1");
        logRecord.getRemarks().add("remark2");
        
        assertEquals(2, logRecord.getRemarks().size());
        assertTrue(logRecord.getRemarks().contains("remark1"));
        assertTrue(logRecord.getRemarks().contains("remark2"));
    }

    @Test
    void testToFlatJson() {
        LogRecord logRecord = new LogRecord();
        logRecord.setUserId(12345L);
        logRecord.setUserName("测试用户");
        logRecord.setOperationName("创建订单");
        logRecord.setOperationType("CREATE");
        logRecord.setMethodName("createOrder");
        logRecord.setClassFullName("com.example.OrderService.createOrder");
        logRecord.setRequestResult(true);
        
        JSONArray params = new JSONArray();
        params.add("param1");
        logRecord.setRequestParams(params);
        
        JSONObject flatJson = logRecord.toFlatJson();
        
        assertNotNull(flatJson);
        assertEquals(12345L, flatJson.getLong("userId"));
        assertEquals("测试用户", flatJson.getString("userName"));
        assertEquals("创建订单", flatJson.getString("operationName"));
        assertEquals("CREATE", flatJson.getString("operationType"));
        assertEquals("createOrder", flatJson.getString("methodName"));
        assertEquals(true, flatJson.getBoolean("requestResult"));
    }

    @Test
    void testToFlatJsonWithNullValues() {
        LogRecord logRecord = new LogRecord();
        // 保持所有值为默认（null）
        
        JSONObject flatJson = logRecord.toFlatJson();
        
        assertNotNull(flatJson);
        // 验证null值被正确处理
        assertTrue(flatJson.containsKey("userId"));
        assertTrue(flatJson.containsKey("userName"));
    }

    @Test
    void testToFlatJsonWithLocalDateTime() {
        LogRecord logRecord = new LogRecord();
        LocalDateTime specificTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        logRecord.setOperationTime(specificTime);
        
        JSONObject flatJson = logRecord.toFlatJson();
        
        assertNotNull(flatJson);
        // LocalDateTime 会被格式化为 UTC 时间字符串
        assertNotNull(flatJson.get("operationTime"));
    }

    @Test
    void testToFlatJsonWithComplexPreValue() {
        LogRecord logRecord = new LogRecord();
        JSONObject preValue = new JSONObject();
        preValue.put("id", 1);
        preValue.put("items", new JSONArray() {{ add("item1"); add("item2"); }});
        logRecord.setPreValue(preValue);
        
        JSONObject flatJson = logRecord.toFlatJson();
        
        assertNotNull(flatJson);
        assertNotNull(flatJson.get("preValue"));
    }

    @Test
    void testCompleteLogRecordCreation() {
        LogRecord logRecord = new LogRecord();
        
        // 设置所有字段
        logRecord.setUserId(1L);
        logRecord.setUserName("张三");
        logRecord.setUserAccountId(100L);
        logRecord.setUserAccountName("zhangsan");
        logRecord.setProjectName("电商系统");
        logRecord.setOperationType("ORDER_CREATE");
        logRecord.setOperationName("创建订单");
        logRecord.setMethodName("createOrder");
        logRecord.setClassFullName("com.ecommerce.OrderService.createOrder");
        logRecord.setRequestResult(true);
        
        JSONArray params = new JSONArray();
        params.add("productId: 123");
        params.add("quantity: 2");
        logRecord.setRequestParams(params);
        
        JSONObject preValue = new JSONObject();
        preValue.put("stock", 100);
        logRecord.setPreValue(preValue);
        
        JSONObject afterValue = new JSONObject();
        afterValue.put("stock", 98);
        afterValue.put("orderId", "ORD202401150001");
        logRecord.setAfterValue(afterValue);
        
        logRecord.getTags().add("订单");
        logRecord.getTags().add("创建");
        logRecord.getRemarks().add("库存扣减成功");
        
        // 验证
        assertEquals(1L, logRecord.getUserId());
        assertEquals("张三", logRecord.getUserName());
        assertEquals(100L, logRecord.getUserAccountId());
        assertEquals("zhangsan", logRecord.getUserAccountName());
        assertEquals("电商系统", logRecord.getProjectName());
        assertEquals("ORDER_CREATE", logRecord.getOperationType());
        assertEquals("创建订单", logRecord.getOperationName());
        assertEquals("createOrder", logRecord.getMethodName());
        assertEquals("com.ecommerce.OrderService.createOrder", logRecord.getClassFullName());
        assertTrue(logRecord.getRequestResult());
        assertEquals(2, logRecord.getRequestParams().size());
        assertEquals(2, logRecord.getTags().size());
        assertEquals(1, logRecord.getRemarks().size());
        
        // 验证 toFlatJson
        JSONObject flatJson = logRecord.toFlatJson();
        assertNotNull(flatJson);
        assertEquals("张三", flatJson.getString("userName"));
    }
}
