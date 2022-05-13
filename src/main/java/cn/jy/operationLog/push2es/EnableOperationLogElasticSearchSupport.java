package cn.jy.operationLog.push2es;


import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({OperationLogElasticConstant.class,OperationLogElasticClient.class,Push2ElasticSearchOperationLogHandler.class})
public @interface EnableOperationLogElasticSearchSupport {

}
