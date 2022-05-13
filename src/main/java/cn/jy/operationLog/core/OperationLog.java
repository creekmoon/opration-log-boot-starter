package cn.jy.operationLog.core;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    //操作描述
    String value() default "未描述的操作";


    //操作失败时,不进行日志记录
    boolean saveOnFail() default false;
}
