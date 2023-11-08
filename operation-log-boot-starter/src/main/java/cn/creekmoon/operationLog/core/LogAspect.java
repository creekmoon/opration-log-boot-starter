package cn.creekmoon.operationLog.core;

import cn.creekmoon.operationLog.utils.ObjectUtils;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson.JSONArray;
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

    private volatile operationLogRecordInitializer logDetailProvider;
    /**
     * 上下文对象实例
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Pointcut("@annotation(cn.creekmoon.operationLog.core.OperationLog)")
    private void pointcut() {
    }


    @Around("pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        /*如果已经有了一个日志对象, 说明外层已经存在AOP了, 本次直接跳过*/
        LogRecord currentLogRecord = OperationLogContext.getCurrentLogRecord();
        if (currentLogRecord != null) {
            return pjp.proceed();
        }

        /*尝试开启新的日志对象*/
        LogRecord logRecord = initOperationLog();

        /*初始化请求体信息*/
        ServletRequestAttributes servletAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletAttributes != null) {
            HttpServletRequest request = servletAttributes.getRequest();
            OperationLogContext.currentServletRequest.set(request);
        }

        /*获取注解值*/
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        ApiOperation swaggerApi = signature.getMethod().getAnnotation(ApiOperation.class);
        OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);
        logRecord.setMethodName(Optional.ofNullable(pjp.getSignature()).map(Signature::getName).orElse("unknownMethod"));
        logRecord.setOperationName(
                OperationLog.DEFAULT_VALUE.equals(annotation.value()) && swaggerApi.value() != null
                        ? swaggerApi.value()
                        : annotation.value());

        /*处理注解所在的方法体参数*/
        try {
            List<Object> paramList = Arrays
                    .stream(Optional.ofNullable(pjp.getArgs()).orElse(new Object[]{}))
                    .map(currentParam -> {
                        if (currentParam == null) {
                            return "null";
                        }
                        /*对不能进行序列化的类进行额外处理*/
                        if (currentParam instanceof ServletRequest) {
                            return "ServletRequest";
                        } else if (currentParam instanceof ServletResponse) {
                            return "ServletResponse";
                        } else if (currentParam instanceof MultipartFile) {
                            return "MultipartFile";
                        } else if (currentParam instanceof MultipartFile[]) {
                            return "MultipartFile[]";
                        } else if (currentParam instanceof InputStreamSource) {
                            return "InputStreamSource";
                        }
                        /*如果是基本类型的参数，则将其转为JSON形式。 如果是对象类型参数，则不需要处理*/
                        if (currentParam.getClass().isPrimitive()
                                || currentParam instanceof Boolean
                                || currentParam instanceof Character
                                || currentParam instanceof Byte
                                || currentParam instanceof Short
                                || currentParam instanceof Integer
                                || currentParam instanceof Long
                                || currentParam instanceof Float
                                || currentParam instanceof Double
                                || currentParam instanceof BigDecimal
                                || currentParam instanceof String
                        ) {
                            return String.valueOf(currentParam);
                        }
                        /*如果是数组类型的参数，则将其转为JSON形式。*/
                        if (currentParam instanceof List || currentParam.getClass().isArray()
                        ) {
                            return ArrayUtil.toString(currentParam);
                        }
                        return currentParam;
                    })
                    .collect(Collectors.toList());
            logRecord.setRequestParams(new JSONArray(paramList));
        } catch (Exception e) {
            log.error("[日志推送]获取方法参数出错！可能入参含有无法转换为JSON的值! 本次参数保存空值！", e);
            logRecord.setRequestParams(new JSONArray());
        }
        try {
            /*执行真正的方法*/
            Object returnValue = pjp.proceed();
            /*执行工厂类afterReturn方法*/
            getLogDetailFactory().postProcess(logRecord, returnValue);
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
                        log.debug("[日志推送]跟踪日志对象时报错! 发生位置setAfterValue!", e);
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
            OperationLogContext.clean();
        }
    }


    /**
     * 创建一个日志记录对象
     *
     * @return
     */
    private LogRecord initOperationLog() {
        LogRecord logRecord = getLogDetailFactory().createNewLogRecord();
        String recordId = UUID.fastUUID().toString();
        OperationLogContext.currentRecordId.set(recordId);
        OperationLogContext.recordId2Logs.put(recordId, logRecord);
        return logRecord;
    }


    private operationLogRecordInitializer getLogDetailFactory() {
        if (this.logDetailProvider == null) {
            synchronized (this) {
                if (this.logDetailProvider == null) {
                    this.logDetailProvider = applicationContext.getBean(operationLogRecordInitializer.class);
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
