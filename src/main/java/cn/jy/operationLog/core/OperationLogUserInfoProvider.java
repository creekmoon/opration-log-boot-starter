package cn.jy.operationLog.core;

public interface OperationLogUserInfoProvider {

    String getUserName();

    Long getUserId();

    Long getOrgId();

    String getProjectName();
}
