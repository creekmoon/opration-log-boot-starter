package cn.jy.operationLog.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        ignoreKeys.forEach(changedFields::remove);
        return changedFields;
    }
}
