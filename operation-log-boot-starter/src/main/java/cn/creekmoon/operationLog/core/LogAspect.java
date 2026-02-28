package cn.creekmoon.operationLog.core;

import cn.creekmoon.operationLog.config.OperationLogProperties;
import cn.creekmoon.operationLog.heatmap.HeatmapCollector;
import cn.creekmoon.operationLog.profile.OperationTypeInference;
import cn.creekmoon.operationLog.profile.ProfileCollector;
import cn.creekmoon.operationLog.profile.ProfileProperties;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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


import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 日志切片 如果存在@WtxLog注解 则进行处理
 */
@Aspect
@Component
@Slf4j
public class LogAspect implements ApplicationContextAware, Ordered {

    private volatile OperationLogRecordInitializer logDetailProvider;
    private volatile OperationLogProperties operationLogProperties;

    /**
     * 上下文对象实例
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取全局配置
     */
    private OperationLogProperties getOperationLogProperties() {
        if (operationLogProperties == null) {
            synchronized (this) {
                if (operationLogProperties == null) {
                    try {
                        operationLogProperties = applicationContext.getBean(OperationLogProperties.class);
                    } catch (Exception e) {
                        operationLogProperties = new OperationLogProperties();
                    }
                }
            }
        }
        return operationLogProperties;
    }

    @Pointcut("@annotation(cn.creekmoon.operationLog.core.OperationLog)")
    private void pointcut() {
    }


    @Around("pointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        /*如果已经有了一个日志对象, 说明外层方法已经启用过一次注解了, 直接跳过*/
        LogRecord currentLogRecord = OperationLogContext.getCurrentLogRecord();
        if (currentLogRecord != null) {
            return pjp.proceed();
        }

        /*尝试开启新的日志对象*/
        LogRecord logRecord = initOperationLog();
        /*尝试获取外部请求体信息*/
        ServletRequestAttributes servletAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletAttributes != null) {
            HttpServletRequest request = servletAttributes.getRequest();
            OperationLogContext.currentServletRequest.set(request);
        }
        /*获取注解所在的方法*/
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Operation swaggerApi = signature.getMethod().getAnnotation(Operation.class);
        OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);
        logRecord.setMethodName(Optional.ofNullable(pjp.getSignature()).map(Signature::getName).orElse("unknownMethod"));
        logRecord.setClassFullName(pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());

        /**
         * 赋值优先级 从上到下
         * 1.使用OperationLog注解(如果已经填写)
         * 2.使用Swagger注解(如果已经填写)
         * 3.使用当前方法类名
         *
         * */
        if (!OperationLog.OPERATION_SUMMARY_DEFAULT.equals(annotation.value())) {
            logRecord.setOperationName(annotation.value());
        } else if (swaggerApi != null && swaggerApi.summary() != null) {
            logRecord.setOperationName(swaggerApi.summary());
        } else {
            logRecord.setOperationName(logRecord.classFullName);
        }


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
            log.error("[operation-log]获取方法参数出错！可能入参含有无法转换为JSON的值! 本次参数保存空值！", e);
            logRecord.setRequestParams(new JSONArray());
        }
        try {
            /*执行真正的方法*/
            Object returnValue = pjp.proceed();
            /*执行工厂类afterReturn方法*/
            getLogDetailFactory().functionPostProcess(logRecord, returnValue);
            return returnValue;
        } catch (Exception e) {
            log.debug("[operation-log]原生方法执行异常!", e);
            logRecord.setRequestResult(Boolean.FALSE);
            /*如果配置了handleOnFail(注解或全局), 将异常消息添加到remarks中*/
            boolean handleOnFail = annotation.handleOnFail() || getOperationLogProperties().isRecordOnFailGlobalEnabled();
            if (handleOnFail) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                logRecord.getRemarks().add("异常: " + errorMsg);
            }
            throw e;
        } finally {
            try {
                /*操作结果正确 或者 操作结果失败且配置了失败记录 才会进行日志记录*/
                boolean handleOnFail = annotation.handleOnFail() || getOperationLogProperties().isRecordOnFailGlobalEnabled();
                boolean isNeedRecord = logRecord.getRequestResult() || (!logRecord.getRequestResult() && handleOnFail);
                /* 跟踪结果变化*/
                if (isNeedRecord) {
                    if (OperationLogContext.metadataSupplier.get() != null) {
                        try {
                            //序列化成JSON格式
                            JSONObject parse = JSONObject.parseObject(JSONObject.toJSONString(OperationLogContext.metadataSupplier.get().call()));
                            logRecord.setAfterValue(parse);
                        } catch (Exception e) {
                            log.debug("[operation-log]跟踪日志对象时报错! 发生位置setAfterValue!", e);
                        }
                    }
                    /*保存日志结果*/
                    LogThreadPool.runTask(() -> {
                        for (OperationLogHandler operationLogHandler : applicationContext.getBeansOfType(OperationLogHandler.class).values()) {
                            try {
                                operationLogHandler.handle(logRecord);
                            } catch (Exception e) {
                                log.error("[operation-log]日志处理器执行异常!", e);
                            }
                        }
                    });

                    /*收集热力图数据 - 支持全局配置*/
                    boolean heatmapEnabled = annotation.heatmap() || getOperationLogProperties().isHeatmapGlobalEnabled();
                    if (heatmapEnabled) {
                        collectHeatmapData(logRecord);
                    }

                    /*收集用户画像数据 - 支持全局配置*/
                    boolean profileEnabled = annotation.profile() || getOperationLogProperties().isProfileGlobalEnabled();
                    if (profileEnabled) {
                        collectProfileData(logRecord, annotation);
                    }
                }
                /*不进行日志记录*/
                if (!isNeedRecord) {
                    log.debug("[operation-log]用户操作没有成功,不会进行日志记录");
                }
            } finally {
                /*确保ThreadLocal上下文始终被清理，防止内存泄漏*/
                OperationLogContext.clean();
            }
        }
    }

    /**
     * 收集热力图数据
     */
    private void collectHeatmapData(LogRecord logRecord) {
        try {
            Map<String, HeatmapCollector> collectors = applicationContext.getBeansOfType(HeatmapCollector.class);
            if (!collectors.isEmpty()) {
                HeatmapCollector collector = collectors.values().iterator().next();
                collector.collect(logRecord);
            }
        } catch (Exception e) {
            log.debug("[operation-log]热力图数据收集异常: {}", e.getMessage());
        }
    }

    /**
     * 收集用户画像数据
     */
    private void collectProfileData(LogRecord logRecord, OperationLog annotation) {
        try {
            Map<String, ProfileCollector> collectors = applicationContext.getBeansOfType(ProfileCollector.class);
            if (!collectors.isEmpty()) {
                ProfileCollector collector = collectors.values().iterator().next();
                
                /*确定操作类型优先级:
                 * 1. 注解显式指定的 type (非 DEFAULT)
                 * 2. 自动推断的类型 (如果启用)
                 * 3. 注解的 type 默认值
                 */
                String operationType = annotation.type();
                if ("DEFAULT".equals(operationType) || operationType == null || operationType.isEmpty()) {
                    // 尝试从 ProfileProperties 获取配置
                    try {
                        ProfileProperties profileProperties = applicationContext.getBean(ProfileProperties.class);
                        if (profileProperties.isAutoInferType()) {
                            OperationTypeInference inference = applicationContext.getBean(OperationTypeInference.class);
                            operationType = inference.inferType(logRecord.getOperationName());
                        }
                    } catch (Exception ex) {
                        // 如果获取 bean 失败，使用默认值
                        operationType = "DEFAULT";
                    }
                }
                
                collector.collect(logRecord, operationType);
            }
        } catch (Exception e) {
            log.debug("[operation-log]用户画像数据收集异常: {}", e.getMessage());
        }
    }


    /**
     * 创建一个日志记录对象
     *
     * @return
     */
    private LogRecord initOperationLog() {
        LogRecord logRecord = new LogRecord();
        logRecord = getLogDetailFactory().init(logRecord);
        String recordId = UUID.fastUUID().toString();
        OperationLogContext.currentRecordId.set(recordId);
        OperationLogContext.recordId2Logs.put(recordId, logRecord);
        return logRecord;
    }


    private OperationLogRecordInitializer getLogDetailFactory() {
        if (this.logDetailProvider == null) {
            synchronized (this) {
                if (this.logDetailProvider == null) {
                    this.logDetailProvider = applicationContext.getBean(OperationLogRecordInitializer.class);
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
