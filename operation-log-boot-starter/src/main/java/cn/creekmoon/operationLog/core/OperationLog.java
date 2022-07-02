package cn.creekmoon.operationLog.core;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    //操作描述
    String value() default "未描述的接口";


    //操作失败时,不进行日志记录
    boolean handleOnFail() default false;
}
