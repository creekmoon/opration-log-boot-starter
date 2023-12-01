package cn.creekmoon.operationLog.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.format.FastDateFormat;
import cn.hutool.core.util.ReflectUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.TimeZone;

import static cn.hutool.core.date.DatePattern.UTC_MS_PATTERN;

@Data
@Slf4j
public class LogRecord {

    /*用户id */
    Long userId;
    /*用户账户名*/
    String userAccount;
    /*用户姓名*/
    String userName;
    /*当前项目名称*/
    String projectName;
    /*操作操作类型*/
    String operationType;
    /*本次操作本次操作名称 如果未在@operationLog中指定,则默认读取swagger注解*/
    String operationName;
    /*操作的JAVA方法名称*/
    String methodName;
    /*操作的JAVA方法全称*/
    String classFullName;
    /*操作之前的值*/
    Object preValue;
    /*操作之后的值*/
    Object afterValue;
    /*操作结果*/
    Boolean requestResult = Boolean.TRUE;
    /*操作参数*/
    JSONArray requestParams;
    /*操作时间 */
    LocalDateTime operationTime = LocalDateTime.now();
    /*记录标签 可以用标签进行索引查找 */
    LinkedHashSet<String> tags = new LinkedHashSet();
    /*备注 可以手动为此次操作添加备注*/
    LinkedHashSet<String> remarks = new LinkedHashSet();

    /**
     * 内置方法, 转换为打平的第一层JSON, 主要是为了方便存储到ES中
     * 这里输出的时间格式为UTC时间
     *
     * @return
     */
    public JSONObject toFlatJson() {
        JSONObject result = new JSONObject();
        Field[] fields = ReflectUtil.getFields(this.getClass());
        for (Field field : fields) {
            try {
                Object vaule = field.get(this);
                String fieldName = field.getName();
                if (vaule == null) {
                    result.put(fieldName, null);
                    continue;
                }
                switch (vaule) {
                    case LocalDateTime x -> result.put(fieldName, DateUtil.format(x.plusHours(-8), UTC_MS_PATTERN));
                    case Date x ->
                            result.put(fieldName, DateUtil.format(x, FastDateFormat.getInstance(UTC_MS_PATTERN, TimeZone.getTimeZone("GMT+:08:00"))));
                    case JSONArray x -> result.put(fieldName, x.toJSONString());
                    case String x -> result.put(fieldName, x);
                    default -> result.put(fieldName, JSONObject.toJSONString(vaule));
                }
            } catch (Exception e) {
                log.error("[operation-log]内置方法toFlatJson转换对象失败!", e);
            }
        }
        return result;
    }
}
