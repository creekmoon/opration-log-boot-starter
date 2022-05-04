package cn.jy.operationLog.core;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 日志切片 如果存在@WtxLog注解 则进行处理
 */
@Aspect
@Component
@Slf4j
public class LogAspect implements ApplicationContextAware {

    private volatile OperationLogUserInfoProvider logUserInfoProvider;
    /**
     * 上下文对象实例
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Pointcut("@annotation(cn.jy.operationLog.core.OperationLog)")
    private void pointcut() {
    }


    @Around("pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {

        //获取请求的上下文
        ServletRequestAttributes servletAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletAttributes == null) {
            return pjp.proceed();
        }
        /*获取注解值*/
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        ApiOperation swaggerApi = signature.getMethod().getAnnotation(ApiOperation.class);
        OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);
        /*初始化日志对象*/
        LogRecord logRecord = new LogRecord();
        OperationLogUserInfoProvider OperationLogUserInfoProvider = getLogUserInfoProvider();
        if (OperationLogUserInfoProvider != null) {
            try {
                logRecord.setUserId(OperationLogUserInfoProvider.getUserId());
                logRecord.setUserName(OperationLogUserInfoProvider.getUserName());
                logRecord.setOrgId(OperationLogUserInfoProvider.getOrgId());
                logRecord.setProjectName(OperationLogUserInfoProvider.getProjectName());
            } catch (Exception e) {
                e.printStackTrace();
                log.debug("[日志推送]获取当前用户信息发生异常!");
            }
        }
        logRecord.setMethodName(Optional.ofNullable(pjp.getSignature()).map(Signature::getName).orElse("unknownMethod"));
        logRecord.setRemark(swaggerApi == null ? annotation.value() : swaggerApi.value());

        try {
            /*处理传递过来的参数*/
            List<Object> argList = Arrays
                    .stream(Optional.ofNullable(pjp.getArgs()).orElse(new Object[]{}))
                    .map(currentParams -> {
                        /*如果是基本类型的参数，则将其转为JSON形式。 如果是对象类型参数，则不需要处理*/
                        if (currentParams.getClass().isPrimitive()
                                || currentParams instanceof Boolean
                                || currentParams instanceof Character
                                || currentParams instanceof Byte
                                || currentParams instanceof Short
                                || currentParams instanceof Integer
                                || currentParams instanceof Long
                                || currentParams instanceof Float
                                || currentParams instanceof Double
                                || currentParams instanceof BigDecimal
                                || currentParams instanceof String
                        ) {
                            JSONObject jsonObject = new JSONObject(1);
                            jsonObject.put("arg", currentParams);
                            return jsonObject;
                        }
                        return currentParams;
                    })
                    .collect(Collectors.toList());
            logRecord.setRequestParams(new JSONArray(argList));
        } catch (Exception e) {
            log.debug("[日志推送]获取方法参数出错！本次参数保存空值！", e);
            logRecord.setRequestParams(new JSONArray());
        }
        /*初始化上下文*/
        HttpServletRequest request = servletAttributes.getRequest();
        try {
            LogContext.currentServletRequest.set(request);
            LogContext.request2Logs.put(request, logRecord);
            /*判断当前方法是否执行成功*/
            Object functionResult = pjp.proceed();
            if (!applicationContext.getBean(OperationLogHandler.class).requestIsSuccess(functionResult)
            ) {
                logRecord.setRequestResult(false);
                if (annotation.onlySuccess()) {
                    log.debug("[日志推送]用户操作没有成功,将不进行日志记录");
                    return functionResult;
                }
            } else {
                logRecord.setRequestResult(true);
            }
            /*如果方法执行成功,拿取方法执行后的结果*/
            try {
                if (LogContext.metadataSupplier.get() != null) {
                    logRecord.setAfterValue(LogContext.metadataSupplier.get().call());
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.debug("[日志推送]跟踪日志对象时报错! 发生位置setAfterValue");
            }
            /*保存日志结果*/
            LogThreadPool.runTask(logRecord.getUserId(), () -> {
                LogFiller.fill(logRecord);
                applicationContext.getBean(OperationLogHandler.class).save(logRecord);
            });
            return functionResult;
        } finally {
            LogContext.request2Logs.remove(request);   //及时移除对象
            LogContext.currentServletRequest.remove(); //及时移除对象
            LogContext.metadataSupplier.remove();//及时移除对象
        }
    }

    private OperationLogUserInfoProvider getLogUserInfoProvider() {
        if (this.logUserInfoProvider == null) {
            synchronized (this) {
                if (this.logUserInfoProvider == null) {
                    this.logUserInfoProvider = applicationContext.getBean(OperationLogUserInfoProvider.class);
                }
            }
        }
        return this.logUserInfoProvider;

    }
}
