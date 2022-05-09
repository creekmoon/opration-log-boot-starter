package cn.jy.operationLog.config;


import cn.jy.operationLog.core.LogAspect;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({OptLogAutoConfiguration.class})
public @interface EnableOperationLog {

}