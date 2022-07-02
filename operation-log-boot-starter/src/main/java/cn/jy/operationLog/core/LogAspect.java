package cn.jy.operationLog.core;

import cn.jy.operationLog.utils.ObjectUtils;
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
import org.springframework.core.Ordered;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 日志切片 如果存在@WtxLog注解 则进行处理
 */
@Aspect
@Component
@Slf4j
public class LogAspect implements ApplicationContextAware, Ordered {

    private volatile OperationLogRecordFactory logDetailProvider;
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
        LogRecord logRecord = getLogDetailFactory().createNewLogRecord();
        logRecord.setMethodName(Optional.ofNullable(pjp.getSignature()).map(Signature::getName).orElse("unknownMethod"));
        logRecord.setOperationName(swaggerApi == null ? annotation.value() : swaggerApi.value());

        try {
            /*处理传递过来的参数*/
            List<Object> argList = Arrays
                    .stream(Optional.ofNullable(pjp.getArgs()).orElse(new Object[]{}))
                    .map(currentParams -> {
                        /*对不能进行序列化的类进行额外处理*/
                        if (currentParams instanceof ServletRequest) {
                            JSONObject jsonObject = new JSONObject(1);
                            jsonObject.put("servletRequest", "无法序列化");
                            return jsonObject;
                        } else if (currentParams instanceof ServletResponse) {
                            JSONObject jsonObject = new JSONObject(1);
                            jsonObject.put("servletResponse", "无法序列化");
                            return jsonObject;
                        } else if (currentParams instanceof MultipartFile) {
                            JSONObject jsonObject = new JSONObject(1);
                            jsonObject.put("multipartFile", "无法序列化");
                            return jsonObject;
                        } else if (currentParams instanceof InputStreamSource) {
                            JSONObject jsonObject = new JSONObject(1);
                            jsonObject.put("inputStreamSource", "无法序列化");
                            return jsonObject;
                        }
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
                            jsonObject.put("arg", String.valueOf(currentParams));
                            return jsonObject;
                        }
                        return currentParams;
                    })
                    .collect(Collectors.toList());
            logRecord.setRequestParams(new JSONArray(argList));
        } catch (Exception e) {
            log.error("[日志推送]获取方法参数出错！可能入参含有无法转换为JSON的值! 本次参数保存空值！", e);
            logRecord.setRequestParams(new JSONArray());
        }
        /*初始化上下文*/
        HttpServletRequest request = servletAttributes.getRequest();
        try {
            /*初始化数据*/
            OperationLogContext.currentServletRequest.set(request);
            OperationLogContext.request2Logs.put(request, logRecord);
            /*执行真正的方法*/
            Object returnValue = pjp.proceed();
            /*执行工厂类afterReturn方法*/
            getLogDetailFactory().afterReturn(logRecord, returnValue);
            return returnValue;
        } catch (Exception e) {
            log.debug("[日志推送]原生方法执行异常!", e);
            logRecord.setRequestResult(Boolean.FALSE);
            throw e;
        } finally {
            /*操作结果正确 或者 操作结果失败且配置了失败记录 才会进行日志记录*/
            boolean isNeedRecord = logRecord.getRequestResult() || (!logRecord.getRequestResult() && annotation.handleOnFail());
            /*进行日志记录*/
            if (isNeedRecord) {
                if (OperationLogContext.metadataSupplier.get() != null) {
                    try {
                        /*跟踪结果变化*/
                        logRecord.setAfterValue(OperationLogContext.metadataSupplier.get().call());
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.debug("[日志推送]跟踪日志对象时报错! 发生位置setAfterValue!");
                    }
                }
                /*保存日志结果*/
                LogThreadPool.runTask(() -> {
                    ObjectUtils.findDifferent(logRecord);
                    for (OperationLogHandler operationLogHandler : applicationContext.getBeansOfType(OperationLogHandler.class).values()) {
                        try {
                            operationLogHandler.handle(logRecord);
                        } catch (Exception e) {
                            log.error("[日志推送]日志处理器执行异常!", e);
                        }
                    }
                });
            }
            /*不进行日志记录*/
            if (!isNeedRecord) {
                log.debug("[日志推送]用户操作没有成功,不会进行日志记录");
            }
            /*最后移除对象*/
            OperationLogContext.request2Logs.remove(request);   //及时移除对象
            OperationLogContext.currentServletRequest.remove(); //及时移除对象
            OperationLogContext.metadataSupplier.remove();//及时移除对象
        }
    }


    private OperationLogRecordFactory getLogDetailFactory() {
        if (this.logDetailProvider == null) {
            synchronized (this) {
                if (this.logDetailProvider == null) {
                    this.logDetailProvider = applicationContext.getBean(OperationLogRecordFactory.class);
                }
            }
        }
        return this.logDetailProvider;

    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
