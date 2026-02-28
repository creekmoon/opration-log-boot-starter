package cn.creekmoon.operationLog.profile;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 操作类型推断器
 * 从操作名称中智能推断操作类型
 */
@Component
public class OperationTypeInference {

    /**
     * 动词到操作类型的映射
     */
    private static final Map<String, String> VERB_TYPE_MAP = Map.ofEntries(
        // 查询类
        Map.entry("查询", "QUERY"),
        Map.entry("获取", "QUERY"),
        Map.entry("列表", "QUERY"),
        Map.entry("查看", "QUERY"),
        Map.entry("搜索", "QUERY"),
        Map.entry("查找", "QUERY"),
        
        // 创建类
        Map.entry("创建", "CREATE"),
        Map.entry("新增", "CREATE"),
        Map.entry("添加", "CREATE"),
        Map.entry("插入", "CREATE"),
        
        // 提交类
        Map.entry("提交", "SUBMIT"),
        Map.entry("保存", "SAVE"),
        Map.entry("确认", "CONFIRM"),
        
        // 更新类
        Map.entry("更新", "UPDATE"),
        Map.entry("修改", "UPDATE"),
        Map.entry("编辑", "UPDATE"),
        Map.entry("变更", "UPDATE"),
        
        // 删除类
        Map.entry("删除", "DELETE"),
        Map.entry("移除", "DELETE"),
        Map.entry("取消", "CANCEL"),
        
        // 导入导出
        Map.entry("导出", "EXPORT"),
        Map.entry("导入", "IMPORT"),
        Map.entry("下载", "DOWNLOAD"),
        Map.entry("上传", "UPLOAD"),
        
        // 认证
        Map.entry("登录", "LOGIN"),
        Map.entry("登出", "LOGOUT"),
        Map.entry("注册", "REGISTER"),
        Map.entry("认证", "AUTH"),
        
        // 审核
        Map.entry("审核", "AUDIT"),
        Map.entry("审批", "APPROVE"),
        Map.entry("驳回", "REJECT")
    );

    /**
     * 从操作名称推断操作类型
     *
     * @param operationName 操作名称，如 "查询订单"、"创建用户"
     * @return 操作类型，如 "QUERY"、"CREATE"
     */
    public String inferType(String operationName) {
        if (operationName == null || operationName.trim().isEmpty()) {
            return "DEFAULT";
        }

        String trimmed = operationName.trim();

        // 1. 尝试前缀匹配（最常见的情况）
        for (Map.Entry<String, String> entry : VERB_TYPE_MAP.entrySet()) {
            if (trimmed.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 2. 尝试包含匹配
        for (Map.Entry<String, String> entry : VERB_TYPE_MAP.entrySet()) {
            if (trimmed.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 3. 默认类型
        return "DEFAULT";
    }

    /**
     * 批量推断操作类型
     */
    public Map<String, String> inferTypes(Map<String, String> operationNames) {
        Map<String, String> result = new java.util.HashMap<>();
        operationNames.forEach((key, value) -> 
            result.put(key, inferType(value))
        );
        return result;
    }
}
