package cn.creekmoon.operationLog.core;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogRecord 单元测试
 */
class LogRecordTest {

    @Test
    @DisplayName("测试 LogRecord 默认构造函数和默认值")
    void testDefaultValues() {
        LogRecord record = new LogRecord();
        
        assertNotNull(record.getOperationTime(), "操作时间应自动设置为当前时间");
        assertNotNull(record.getTags(), "标签集合不应为null");
        assertNotNull(record.getRemarks(), "备注集合不应为null");
        assertTrue(record.getTags() instanceof LinkedHashSet, "标签应为LinkedHashSet类型");
        assertTrue(record.getRemarks() instanceof LinkedHashSet, "备注应为LinkedHashSet类型");
        assertEquals(Boolean.TRUE, record.getRequestResult(), "默认操作结果应为true");
    }

    @Test
    @DisplayName("测试 LogRecord setter 和 getter 方法")
    void testSettersAndGetters() {
        LogRecord record = new LogRecord();
        
        // 设置基本属性
        record.setUserId(1L);
        record.setUserName("张三");
        record.setUserAccountId(100L);
        record.setUserAccountName("zhangsan");
        record.setProjectName("测试项目");
        record.setOperationType("CREATE");
        record.setOperationName("创建订单");
        record.setMethodName("createOrder");
        record.setClassFullName("cn.creekmoon.service.OrderService");
        record.setRequestResult(false);
        
        // 验证
        assertEquals(1L, record.getUserId());
        assertEquals("张三", record.getUserName());
        assertEquals(100L, record.getUserAccountId());
        assertEquals("zhangsan", record.getUserAccountName());
        assertEquals("测试项目", record.getProjectName());
        assertEquals("CREATE", record.getOperationType());
        assertEquals("创建订单", record.getOperationName());
        assertEquals("createOrder", record.getMethodName());
        assertEquals("cn.creekmoon.service.OrderService", record.getClassFullName());
        assertEquals(Boolean.FALSE, record.getRequestResult());
    }

    @Test
    @DisplayName("测试 toFlatJson 方法 - 空对象转换")
    void testToFlatJsonEmptyRecord() {
        LogRecord record = new LogRecord();
        JSONObject json = record.toFlatJson();
        
        assertNotNull(json);
        assertTrue(json.containsKey("userId"));
        assertTrue(json.containsKey("userName"));
        assertTrue(json.containsKey("operationTime"));
        assertTrue(json.containsKey("tags"));
        assertTrue(json.containsKey("remarks"));
    }

    @Test
    @DisplayName("测试 toFlatJson 方法 - 完整对象转换")
    void testToFlatJsonWithValues() {
        LogRecord record = new LogRecord();
        record.setUserId(1L);
        record.setUserName("张三");
        record.setOperationType("UPDATE");
        record.setOperationName("更新用户");
        record.setRequestResult(true);
        
        JSONArray params = new JSONArray();
        params.add("param1");
        params.add("param2");
        record.setRequestParams(params);
        
        JSONObject json = record.toFlatJson();
        
        assertEquals(1L, json.getLong("userId"));
        assertEquals("张三", json.getString("userName"));
        assertEquals("UPDATE", json.getString("operationType"));
        assertEquals("UPDATE", json.getString("operationType"));
        assertTrue(json.getBoolean("requestResult"));
    }

    @Test
    @DisplayName("测试 tags 和 remarks 集合操作")
    void testTagsAndRemarks() {
        LogRecord record = new LogRecord();
        
        // 测试标签
        record.getTags().add("important");
        record.getTags().add("order");
        record.getTags().add("important"); // 重复添加
        
        assertEquals(2, record.getTags().size(), "LinkedHashSet 应去重");
        assertTrue(record.getTags().contains("important"));
        assertTrue(record.getTags().contains("order"));
        
        // 测试备注
        record.getRemarks().add("用户主动操作");
        record.getRemarks().add("需要审核");
        
        assertEquals(2, record.getRemarks().size());
        assertTrue(record.getRemarks().contains("用户主动操作"));
    }

    @Test
    @DisplayName("测试 toFlatJson 处理 LocalDateTime 格式")
    void testToFlatJsonDateTimeFormat() {
        LogRecord record = new LogRecord();
        LocalDateTime now = LocalDateTime.now();
        record.setOperationTime(now);
        
        JSONObject json = record.toFlatJson();
        
        assertNotNull(json.getString("operationTime"));
        // UTC 时间格式验证
        String timeStr = json.getString("operationTime");
        assertTrue(timeStr.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"), 
            "时间格式应为 ISO 格式");
    }

    @Test
    @DisplayName("测试 toFlatJson 处理 requestParams JSON 数组")
    void testToFlatJsonWithRequestParams() {
        LogRecord record = new LogRecord();
        
        JSONArray params = new JSONArray();
        params.add(JSONObject.of("id", 1, "name", "test"));
        params.add("stringParam");
        record.setRequestParams(params);
        
        JSONObject json = record.toFlatJson();
        
        assertNotNull(json.getString("requestParams"));
        String paramsStr = json.getString("requestParams");
        assertTrue(paramsStr.contains("id"));
        assertTrue(paramsStr.contains("test"));
    }

    @Test
    @DisplayName("测试 preValue 和 afterValue 设置")
    void testPreAndAfterValues() {
        LogRecord record = new LogRecord();
        
        JSONObject preValue = JSONObject.of("status", "pending", "amount", 100);
        JSONObject afterValue = JSONObject.of("status", "completed", "amount", 100);
        
        record.setPreValue(preValue);
        record.setAfterValue(afterValue);
        
        assertEquals(preValue, record.getPreValue());
        assertEquals(afterValue, record.getAfterValue());
        
        // 测试 JSON 转换
        JSONObject json = record.toFlatJson();
        assertNotNull(json.getString("preValue"));
        assertNotNull(json.getString("afterValue"));
    }
}
