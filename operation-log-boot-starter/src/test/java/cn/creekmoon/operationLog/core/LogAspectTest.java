package cn.creekmoon.operationLog.core;

import cn.creekmoon.operationLog.config.DefaultOperationLogRecordInitializer;
import cn.creekmoon.operationLog.config.OperationLogProperties;
import cn.creekmoon.operationLog.heatmap.HeatmapCollector;
import cn.creekmoon.operationLog.profile.OperationTypeInference;
import cn.creekmoon.operationLog.profile.ProfileCollector;
import cn.creekmoon.operationLog.profile.ProfileProperties;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

/**
 * LogAspect 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LogAspectTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private DefaultOperationLogRecordInitializer logRecordInitializer;

    @Mock
    private OperationLogHandler operationLogHandler;

    @Mock
    private HeatmapCollector heatmapCollector;

    @Mock
    private ProfileCollector profileCollector;

    @Mock
    private ProfileProperties profileProperties;

    @Mock
    private OperationTypeInference operationTypeInference;

    private LogAspect logAspect;

    @BeforeEach
    void setUp() {
        logAspect = new LogAspect();
        logAspect.setApplicationContext(applicationContext);

        // 清理上下文
        OperationLogContext.clean();
        OperationLogContext.disable = false;
    }

    @AfterEach
    void tearDown() {
        OperationLogContext.clean();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 测试带有 @OperationLog 注解的方法
     */
    @Test
    void testAround_WithOperationLogAnnotation() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", true, true);
        when(proceedingJoinPoint.proceed()).thenReturn("success result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        handlerMap.put("testHandler", operationLogHandler);
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        Object result = logAspect.around(proceedingJoinPoint);

        // Then
        assertEquals("success result", result);
        verify(proceedingJoinPoint).proceed();
        verify(logRecordInitializer).init(any(LogRecord.class));
        verify(logRecordInitializer).functionPostProcess(any(LogRecord.class), eq("success result"));
    }

    /**
     * 测试嵌套调用（外层已有日志记录时直接跳过）
     */
    @Test
    void testAround_WithNestedCall() throws Throwable {
        // Given - 模拟已有日志记录
        LogRecord existingRecord = new LogRecord();
        String recordId = "existing-record-id";
        OperationLogContext.recordId2Logs.put(recordId, existingRecord);
        OperationLogContext.currentRecordId.set(recordId);

        when(proceedingJoinPoint.proceed()).thenReturn("result");

        // When
        Object result = logAspect.around(proceedingJoinPoint);

        // Then - 应直接执行原方法，不进行日志处理
        assertEquals("result", result);
        verify(proceedingJoinPoint).proceed();
        verifyNoInteractions(applicationContext);
    }

    /**
     * 测试方法执行异常时的处理
     */
    @Test
    void testAround_WithException() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", true, "DEFAULT", false, false); // handleOnFail = true
        RuntimeException exception = new RuntimeException("Test exception");
        when(proceedingJoinPoint.proceed()).thenThrow(exception);
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        handlerMap.put("testHandler", operationLogHandler);
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);

        OperationLogProperties properties = new OperationLogProperties();
        properties.setHandleOnFailGlobalEnabled(true);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(properties);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            logAspect.around(proceedingJoinPoint);
        });
        assertEquals("Test exception", thrown.getMessage());
    }

    /**
     * 测试参数处理 - ServletRequest
     */
    @Test
    void testAround_WithServletRequestParam() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, false);

        jakarta.servlet.ServletRequest servletRequest = mock(jakarta.servlet.ServletRequest.class);
        Object[] args = new Object[]{servletRequest, "param2"};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        logAspect.around(proceedingJoinPoint);

        // Then - 验证参数被正确处理
        verify(proceedingJoinPoint).proceed();
    }

    /**
     * 测试参数处理 - MultipartFile
     */
    @Test
    void testAround_WithMultipartFileParam() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, false);

        MultipartFile multipartFile = mock(MultipartFile.class);
        Object[] args = new Object[]{multipartFile};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
    }

    /**
     * 测试参数处理 - 基本类型
     */
    @Test
    void testAround_WithPrimitiveParams() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, false);

        Object[] args = new Object[]{123, "string", true, 3.14, new BigDecimal("99.99"), 'A'};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
    }

    /**
     * 测试参数处理 - 数组类型
     */
    @Test
    void testAround_WithArrayParam() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, false);

        Object[] args = new Object[]{new String[]{"item1", "item2", "item3"}};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
    }

    /**
     * 测试参数处理 - null 参数
     */
    @Test
    void testAround_WithNullParam() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, false);

        Object[] args = new Object[]{null, "valid"};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
    }

    /**
     * 测试参数序列化异常处理
     */
    @Test
    void testAround_WithSerializationError() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, false);

        // 使用一个不能正确序列化的对象
        Object circular = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("Serialization error");
            }
        };
        Object[] args = new Object[]{circular};
        when(proceedingJoinPoint.getArgs()).thenReturn(args);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When - 不应抛出异常
        assertDoesNotThrow(() -> logAspect.around(proceedingJoinPoint));
    }

    /**
     * 测试使用 Swagger 注解获取操作名称
     */
    @Test
    void testAround_WithSwaggerAnnotation() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation(OperationLog.OPERATION_SUMMARY_DEFAULT, false, "DEFAULT", false, false);

        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(proceedingJoinPoint).proceed();
    }

    /**
     * 测试热力图数据收集
     */
    @Test
    void testAround_WithHeatmapCollection() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", true, false);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        Map<String, HeatmapCollector> heatmapMap = new HashMap<>();
        heatmapMap.put("heatmapCollector", heatmapCollector);
        when(applicationContext.getBeansOfType(HeatmapCollector.class)).thenReturn(heatmapMap);

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(heatmapCollector).collect(any(LogRecord.class));
    }

    /**
     * 测试用户画像数据收集
     */
    @Test
    void testAround_WithProfileCollection() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "ORDER_QUERY", false, true);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        Map<String, ProfileCollector> profileMap = new HashMap<>();
        profileMap.put("profileCollector", profileCollector);
        when(applicationContext.getBeansOfType(ProfileCollector.class)).thenReturn(profileMap);

        // When
        logAspect.around(proceedingJoinPoint);

        // Then
        verify(profileCollector).collect(any(LogRecord.class), eq("ORDER_QUERY"));
    }

    /**
     * 测试用户画像数据收集 - 自动推断操作类型
     */
    @Test
    void testAround_WithAutoInferOperationType() throws Throwable {
        // Given
        setupBasicMocks();
        setupOperationLogAnnotation("测试操作", false, "DEFAULT", false, true);
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(logRecordInitializer.init(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(logRecordInitializer).functionPostProcess(any(), any());

        Map<String, OperationLogHandler> handlerMap = new HashMap<>();
        when(applicationContext.getBeansOfType(OperationLogHandler.class)).thenReturn(handlerMap);
        when(applicationContext.getBean(OperationLogRecordInitializer.class)).thenReturn(logRecordInitializer);
        when(applicationContext.getBean(OperationLogProperties.class)).thenReturn(new OperationLogProperties());

        Map<String, ProfileCollector> profileMap = new HashMap<>();
        profileMap.put("profileCollector", profileCollector);
        when(applicationContext.getBeansOfType(ProfileCollector.class)).thenReturn(profileMap);
        when(applicationContext.getBean(ProfileProperties.class)).thenReturn(profileProperties);
        when(applicationContext.getBean(OperationTypeInference.class)).thenReturn(operationTypeInference);
        when(profileProperties.isAutoInferType()).thenReturn(true);
        lenient().when(operationTypeInference.inferType(anyString())).thenReturn("INFERRED_TYPE");

        // When
        logAspect.around(proceedingJoinPoint);

        // Then - 验证 collect 被调用，操作类型可能是 INFERRED_TYPE 或 DEFAULT
        verify(profileCollector).collect(any(LogRecord.class), anyString());
    }

    /**
     * 测试 Order 接口实现
     */
    @Test
    void testGetOrder() {
        // When
        int order = logAspect.getOrder();

        // Then
        assertEquals(Integer.MAX_VALUE, order);
    }

    // Helper methods

    private void setupBasicMocks() {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn("testMethod");
        when(methodSignature.getDeclaringTypeName()).thenReturn("cn.creekmoon.TestClass");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
    }

    @OperationLog(value = "默认测试操作", handleOnFail = false, type = "DEFAULT", heatmap = true, profile = true)
    public void defaultAnnotatedTestMethod() {
        // 用于测试的带注解方法
    }

    @OperationLog(value = OperationLog.OPERATION_SUMMARY_DEFAULT, handleOnFail = false, type = "DEFAULT", heatmap = false, profile = false)
    @Operation(summary = "Swagger操作名称")
    public void swaggerAnnotatedTestMethod() {
        // 用于 Swagger 测试的方法
    }

    @OperationLog(value = "热力图测试", type = "ORDER_QUERY", heatmap = true, profile = false)
    public void heatmapAnnotatedTestMethod() {
        // 用于热力图测试的方法
    }

    @OperationLog(value = "画像测试", type = "DEFAULT", heatmap = false, profile = true)
    public void profileAnnotatedTestMethod() {
        // 用于画像测试的方法
    }

    @OperationLog(value = "画像测试带类型", type = "ORDER_QUERY", heatmap = false, profile = true)
    public void profileWithTypeAnnotatedTestMethod() {
        // 用于带类型的画像测试方法
    }

    private void setupOperationLogAnnotation(String value, boolean handleOnFail, String type,
                                              boolean heatmap, boolean profile) throws NoSuchMethodException {
        // 根据参数选择合适的方法
        Method realMethod;
        if (OperationLog.OPERATION_SUMMARY_DEFAULT.equals(value)) {
            realMethod = getClass().getMethod("swaggerAnnotatedTestMethod");
        } else if ("ORDER_QUERY".equals(type) && profile) {
            realMethod = getClass().getMethod("profileWithTypeAnnotatedTestMethod");
        } else if ("ORDER_QUERY".equals(type)) {
            realMethod = getClass().getMethod("heatmapAnnotatedTestMethod");
        } else if (profile && !heatmap) {
            realMethod = getClass().getMethod("profileAnnotatedTestMethod");
        } else {
            realMethod = getClass().getMethod("defaultAnnotatedTestMethod");
        }
        when(methodSignature.getMethod()).thenReturn(realMethod);
    }
}
