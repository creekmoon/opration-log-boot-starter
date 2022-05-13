package cn.jy.operationLog.core;

/**
 * 日志细节提供器
 */
public interface OperationLogDetailProvider {

    String getUserName();

    Long getUserId();

    Long getOrgId();

    String getProjectName();

    /**
     * 定义http本次请求是否失败 (默认 false)
     *
     * @param returnValue 方法请求的返回值
     * @return
     */
    default Boolean requestIsFail(Object returnValue) {
        return false;
    }
}
