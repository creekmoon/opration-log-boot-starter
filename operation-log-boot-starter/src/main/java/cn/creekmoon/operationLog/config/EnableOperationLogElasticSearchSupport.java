package cn.creekmoon.operationLog.config;


import cn.creekmoon.operationLog.elasticSupport.OperationLogElasticClient;
import cn.creekmoon.operationLog.elasticSupport.Push2ElasticSearchOperationLogHandler;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@EnableOperationLog
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({OperationLogElasticClient.class, Push2ElasticSearchOperationLogHandler.class})
public @interface EnableOperationLogElasticSearchSupport {

}
