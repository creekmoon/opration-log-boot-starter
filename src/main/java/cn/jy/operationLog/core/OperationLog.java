package cn.jy.operationLog.core;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    //操作描述
    String value() default "未描述的操作";


    //日志记录条件: 执行成功才会记录()
    boolean onlySuccess() default true;
}
