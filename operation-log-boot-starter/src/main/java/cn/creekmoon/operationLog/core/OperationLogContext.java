package cn.creekmoon.operationLog.core;


import cn.hutool.core.util.StrUtil;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.ServletRequest;
import lombok.extern.slf4j.Slf4j;


import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志上下文
 * 每一个 request都会独占一个线程.
 * ThreadLocal记录下当前请求的信息 使用过后需要及时清理
 *
 * @author JY
 */
@Slf4j
public class OperationLogContext {
    /*当前是否处于禁用状态*/
    public static boolean disable = true;
    /*当前记录实例识别号*/
    protected static ThreadLocal<String> currentRecordId = new ThreadLocal<>();
    /*当前请求*/
    protected static ThreadLocal<ServletRequest> currentServletRequest = new ThreadLocal<>();
    /*跟踪的元数据*/
    protected static ThreadLocal<Callable<Object>> metadataSupplier = new ThreadLocal<>();
    protected static ConcurrentHashMap<String, LogRecord> recordId2Logs = new ConcurrentHashMap(1024);

    /**
     * 传入一个获取数据的方式,会通过这个方式监控数据变化 体现在effectFields字段中
     *
     * @param metadata 元数据,传入需要监控的对象
     */
    public static void follow(Callable<Object> metadata) {
        if (disable) {
            return;
        }
        LogRecord record = OperationLogContext.getCurrentLogRecord();
        if (record == null) {
            log.error("[日志推送]获取日志上下文失败! 请检查是否添加了@OperationLog注解!", new RuntimeException("获取日志上下文失败!"));
            return;
        }
        try {
            if (metadata != null) {
                metadataSupplier.set(metadata);
                //序列化成JSON格式
                JSONObject parse = JSONObject.parseObject(JSONObject.toJSONString(metadata.call()));
                record.setPreValue(parse);
            }
        } catch (Exception e) {
            log.warn("[日志推送]跟踪日志对象时报错! 发生位置setPreValue");
        }
    }

    /**
     * 标记当前的日志操作为失败
     *
     * @return
     */
    public static void markFail() {
        LogRecord currentLogRecord = getCurrentLogRecord();
        if (currentLogRecord == null) {
            return;
        }
        currentLogRecord.setRequestResult(false);
    }

    /**
     * 获取当前的日志记录对象
     *
     * @return
     */
    public static LogRecord getCurrentLogRecord() {
        String recordId = currentRecordId.get();
        return recordId == null ? null : recordId2Logs.get(recordId);
    }


    /**
     * 增加标签 可以根据自己定义的标签,方便索引日志
     *
     * @param tags 标签
     */
    public static void addTags(String... tags) {
        if (disable || tags == null) {
            return;
        }
        LogRecord record = getCurrentLogRecord();
        if (record == null) {
            return;
        }
        for (String tag : tags) {
            if (StrUtil.isNotBlank(tag)) {
                record.getTags().add(tag.trim());
            }
        }
    }

    /**
     * 增加备注
     *
     * @param remarks 备注信息
     */
    public static void addRemarks(String... remarks) {
        if (disable || remarks == null) {
            return;
        }
        LogRecord record = getCurrentLogRecord();
        if (record == null) {
            log.error("[日志推送]获取日志上下文失败! 请检查是否添加了@OperationLog注解!", new RuntimeException("获取日志上下文失败!"));
            return;
        }
        for (String remark : remarks) {
            if (remark != null) {
                record.getRemarks().add(remark.trim());
            }
        }
    }


    /**
     * 清理当前的上下文信息
     */
    protected static void clean() {
        String recordId = OperationLogContext.currentRecordId.get();
        if (recordId == null) {
            return;
        }
        /*移除对象*/
        OperationLogContext.recordId2Logs.remove(recordId);   //及时移除对象
        OperationLogContext.currentServletRequest.remove(); //及时移除对象
        OperationLogContext.metadataSupplier.remove();//及时移除对象
        OperationLogContext.currentRecordId.remove();//及时移除对象
    }

}
