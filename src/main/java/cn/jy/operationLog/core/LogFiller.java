package cn.jy.operationLog.core;

import cn.hutool.core.bean.BeanUtil;

import cn.jy.operationLog.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


/**
 * 日志填装器
 */
@Slf4j
public class LogFiller {

    /*后期处理日志(查找不同点) 并输出内容*/
    public static void fill(LogRecord record) {
        LogThreadPool.runTask(Optional.ofNullable(record.getUserId()).orElse(1L), () -> {
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
            record.setOpsTime(new Date(System.currentTimeMillis()));

        });
    }
}
