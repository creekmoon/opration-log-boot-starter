package cn.creekmoon.operationLog.core;


import java.lang.annotation.*;

/**
 * @author creekmoon
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    public static final String OPERATION_TYPE_DEFAULT = "DEFAULT";


    public static final String OPERATION_SUMMARY_DEFAULT = "未描述的接口";

    /**
     * 操作描述
     * @return
     */
    String value() default OPERATION_SUMMARY_DEFAULT;

    /**
     * 操作类型
     *
     * @return
     */
    String type() default OPERATION_TYPE_DEFAULT;

    //操作失败时,不进行日志记录
    boolean handleOnFail() default false;

    /**
     * 是否启用热力图统计
     * 默认关闭,需要显式开启
     */
    boolean heatmap() default false;

    /**
     * 是否启用用户行为画像统计
     * 默认关闭,需要显式开启
     */
    boolean profile() default false;
}
