package cn.jy.operationLog.core;

import cn.hutool.core.bean.BeanUtil;
import cn.jy.operationLog.utils.ObjectUtils;

import java.util.*;

/**
 * 实现这个接口,需要保证线程安全!
 */
public interface OperationLogHandler {


    /**
     * 定义http本次请求是否成功  如果成功则记录 不成功则不记录
     *
     * @param functionResult 本次执行的结果
     * @return
     */
    boolean requestIsSuccess(Object functionResult);


    /**
     * 定义如何保存这个日志
     *
     * @param logRecord
     */
    void save(LogRecord logRecord);


    static void findDifferent(LogRecord record) {
        /*找出改变的字段*/
        Map<String, Object> oldMap = Optional.ofNullable(record.preValue).map(BeanUtil::beanToMap).orElse(null);
        Map<String, Object> newMap = Optional.ofNullable(record.afterValue).map(BeanUtil::beanToMap).orElse(null);
        Set<String> different = ObjectUtils.findDifferent4Map(
                oldMap,
                newMap,
                Collections.EMPTY_SET);
        record.setEffectFields(different);
        /*找出改变的字段对应的值*/
        record.setEffectFieldsBefore(oldMap);
        record.setEffectFieldsAfter(newMap);
        /*设置操作时间*/
        record.setOpsTime(new Date());
    }
}
