package cn.creekmoon.operationLog.utils;


import cn.creekmoon.operationLog.core.LogRecord;
import cn.creekmoon.operationLog.hutool589.core.bean.BeanUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.*;

public class ObjectUtils {

    /**
     * 找出两个MAP中的不同
     *
     * @param oldMap     旧值
     * @param newMap     新值
     * @param ignoreKeys 需要忽略的KEY
     * @return
     */
    public static Set<String> findDifferent4Map(Map<String, Object> oldMap, Map<String, Object> newMap, Set<String> ignoreKeys) {
        /*如果为null 赋空值*/
        final Map<String, Object> finalOldMap = oldMap == null ? Collections.emptyMap() : oldMap;
        final Map<String, Object> finalNewMap = newMap == null ? Collections.emptyMap() : newMap;
        /*已经确定有改变的值*/
        HashSet<String> changedFields = new HashSet<>();
        /*未参与比较的值*/
        HashSet<String> noComparedKeys = new HashSet<>(finalOldMap.keySet());

        finalNewMap.forEach((field, newFieldValue) -> {
            noComparedKeys.remove(field);
            if (ignoreKeys != null && ignoreKeys.size() > 0 && ignoreKeys.contains(field)) {
                /*如果需要忽略*/
                return;
            }
            Object oldFieldValue;
            /*如果出现NULL值*/
            if ((oldFieldValue = finalOldMap.get(field)) == null || newFieldValue == null) {
                if (oldFieldValue != null || newFieldValue != null) {
                    /*一个是NULL 一个非NULL  则认为改变了值*/
                    changedFields.add(field);
                }
                return;
            }
            if (!newFieldValue.equals(oldFieldValue)) {
                changedFields.add(field);
            }
        });
        changedFields.addAll(noComparedKeys);
        if (ignoreKeys != null) {
            ignoreKeys.forEach(changedFields::remove);
        }
        return changedFields;
    }


    public static void findDifferent(LogRecord record) {
        /*找出改变的字段*/
        Map<String, Object> oldMap = Optional.ofNullable(record.getPreValue()).map(BeanUtil::beanToMap).orElse(null);
        Map<String, Object> newMap = Optional.ofNullable(record.getAfterValue()).map(BeanUtil::beanToMap).orElse(null);
        Set<String> different = ObjectUtils.findDifferent4Map(
                oldMap,
                newMap,
                Collections.EMPTY_SET);
        record.setEffectFields(different);
        /*找出改变的字段对应的值*/
        JSONObject effectFieldsBefore = new JSONObject();
        JSONObject effectFieldsAfter = new JSONObject();
        for (String field : different) {
            effectFieldsBefore.put(field, oldMap == null ? null : oldMap.get(field));
            effectFieldsAfter.put(field, newMap == null ? null : newMap.get(field));
        }
        record.setEffectFieldsBefore(effectFieldsBefore);
        record.setEffectFieldsAfter(effectFieldsAfter);
        /*设置操作时间*/
        record.setOperationTime(new Date());
    }
}
