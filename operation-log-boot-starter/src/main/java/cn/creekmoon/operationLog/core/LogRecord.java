package cn.creekmoon.operationLog.core;

import cn.creekmoon.operationLog.hutoolCore589.core.date.DateUtil;
import cn.creekmoon.operationLog.hutoolCore589.core.date.format.FastDateFormat;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;

import static cn.creekmoon.operationLog.hutoolCore589.core.date.DatePattern.UTC_MS_PATTERN;

@Data
public class LogRecord {
    /*用户id */
    Long userId;
    /*用户机构id*/
    Long orgId;
    /*用户姓名*/
    String userName;
    /*当前项目名称*/
    String projectName;
    /*本次操作本次操作名称 如果未在@operationLog中指定,则默认读取swagger注解*/
    String operationName;
    /*操作的JAVA方法名称*/
    String methodName;
    /*可能改变的值*/
    Set<String> effectFields;
    /*改变的值之前*/
    JSONObject effectFieldsBefore;
    /*改变的值之后*/
    JSONObject effectFieldsAfter;
    /*操作之前的值*/
    Object preValue;
    /*操作之后的值*/
    Object afterValue;
    /*操作结果*/
    Boolean requestResult = Boolean.TRUE;
    /*操作参数*/
    JSONArray requestParams;
    /*操作时间 */
    Date operationTime = new Date();
    /*记录标签 可以用标签进行索引查找 */
    LinkedHashSet<String> tags = new LinkedHashSet();
    /*备注 可以手动为此次操作添加备注*/
    LinkedHashSet<String> remarks = new LinkedHashSet();


    /**
     * 内置方法, 转换为打平的JSON对象(即所有第一级属性都转为简单的String或String[]类型)
     *
     * @return
     */
    public JSONObject toFlatJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userId != null ? String.valueOf(userId) : null);
        jsonObject.put("orgId", orgId != null ? String.valueOf(orgId) : null);
        jsonObject.put("userName", userName);
        jsonObject.put("projectName", projectName);
        jsonObject.put("operationName", operationName);
        jsonObject.put("methodName", methodName);
        jsonObject.put("effectFields", effectFields);
        jsonObject.put("preValue", preValue != null ? JSONObject.toJSONString(preValue) : null);
        jsonObject.put("afterValue", afterValue != null ? JSONObject.toJSONString(afterValue) : null);
        jsonObject.put("effectFieldsBefore", effectFieldsBefore != null ? JSONObject.toJSONString(effectFieldsBefore) : null);
        jsonObject.put("effectFieldsAfter", effectFieldsAfter != null ? JSONObject.toJSONString(effectFieldsAfter) : null);
        jsonObject.put("requestResult", requestResult);
        jsonObject.put("requestParams", requestParams != null ? requestParams.toJSONString() : null);
        jsonObject.put("operationTime", operationTime != null ? DateUtil.format(operationTime, FastDateFormat.getInstance(UTC_MS_PATTERN, TimeZone.getTimeZone("GMT+:08:00"))) : null);
        jsonObject.put("tags", tags);
        jsonObject.put("remarks", remarks);
        return jsonObject;
    }

}
