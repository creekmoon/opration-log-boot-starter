package cn.jy.operationLog.core;

import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.*;

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
    /*本次操作说明*/
    String remark;
    /*操作方法*/
    String methodName;
    /*可能改变的值*/
    Set<String> effectFields;
    /*改变的值之前*/
    Map<String, Object> effectFieldsBefore;
    /*改变的值之后*/
    Map<String, Object> effectFieldsAfter;
    /*操作之前的值*/
    Object preValue;
    /*操作之后的值*/
    Object afterValue;
    /*操作结果*/
    Boolean requestResult;
    /*操作参数*/
    JSONArray requestParams;
    /*操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Date operationTime;
    /*标记点*/
    List<String> markPoints = new ArrayList<>(4);
}
