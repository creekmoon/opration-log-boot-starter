# 模块-AOP切面与采集

## 0. 模块目标

拦截标注 `@OperationLog` 的方法，采集调用上下文（方法信息/入参/结果/异常/操作名）并决定是否异步记录。

## 1. 边界

- **包含**：AOP 切点、日志对象初始化、操作名决策、入参序列化、异常处理、是否记录的判定、异步触发处理器
- **不包含**：字段差异计算（当前只写入 `preValue/afterValue`，不计算 diff）

## 2. 可检索关键词

LogAspect.around / @Around / @Pointcut / OperationLog.handleOnFail / RequestContextHolder / swagger @Operation summary / JSONArray / MultipartFile / CallerRunsPolicy

## 3. 已实现能力（DONE）

- **切点定义**：仅拦截 `@annotation(cn.creekmoon.operationLog.core.OperationLog)`（锚点：`LogAspect#pointcut`）
- **嵌套拦截保护**：同线程内若已有 `LogRecord`，内层 `@OperationLog` 直接跳过记录（锚点：`LogAspect#around` 开头对 `OperationLogContext.getCurrentLogRecord()` 的判断）
- **日志对象初始化**：创建 `LogRecord` 后调用 `OperationLogRecordInitializer#init()`，并在 `OperationLogContext` 中注册 recordId → logRecord（锚点：`LogAspect#initOperationLog`）
- **操作名优先级**：
  - `@OperationLog.value` 非默认值时使用该值
  - 否则使用 swagger `@Operation.summary`
  - 否则回退到 `classFullName`
  （锚点：`LogAspect#around` 中 `OperationLog.OPERATION_SUMMARY_DEFAULT` 判断 + `io.swagger.v3.oas.annotations.Operation` 读取）
- **入参采集与兜底**：把方法参数映射为 `JSONArray`，对 `ServletRequest/ServletResponse/MultipartFile/InputStreamSource` 等不可序列化类型写入占位字符串；异常时置空数组（锚点：`LogAspect#around` 中构建 `paramList` 与 `catch` 分支）
- **异常处理与是否记录**：捕获 `Exception` 时置 `requestResult=false`；仅当成功或 `handleOnFail=true` 时进入记录分支（锚点：`LogAspect#around` 的 `catch` 与 `finally` 中 `isNeedRecord` 判定）
- **失败备注**：当 `handleOnFail=true` 且发生异常时，将异常信息写入 `remarks`（锚点：`LogAspect#around` 的 `catch` 内 `logRecord.getRemarks().add("异常: " + errorMsg)`）
- **后置扩展点**：方法成功返回后调用 `OperationLogRecordInitializer#functionPostProcess(logRecord, returnValue)`（锚点：`LogAspect#around` 成功分支）
- **异步触发处理器**：通过 `LogThreadPool.runTask` 异步遍历所有 `OperationLogHandler` bean 并调用 `handle()`，单个 handler 异常不会影响其他 handler（锚点：`LogAspect#around` finally 内异步任务）
- **上下文清理**：无论成功/异常都会 `OperationLogContext.clean()`（锚点：`LogAspect#around` finally 末尾）

## 4. 主要入口与关键类

- `cn.creekmoon.operationLog.core.LogAspect#around`
- `cn.creekmoon.operationLog.core.OperationLog`
- `cn.creekmoon.operationLog.core.OperationLogContext`
- `cn.creekmoon.operationLog.core.LogThreadPool`
- `cn.creekmoon.operationLog.core.OperationLogHandler`
- `cn.creekmoon.operationLog.core.OperationLogRecordInitializer`

## 5. 核心流程（高层）

1. 检查是否嵌套调用（已存在 `LogRecord` 则跳过）
2. 初始化 `LogRecord` 并写入上下文
3. 决策 `operationName`，采集 `methodName/classFullName`
4. 采集入参到 `requestParams`
5. `proceed()` 执行原方法
6. 成功：调用 `functionPostProcess`
7. 失败：置 `requestResult=false`，必要时写 `remarks`
8. 判定是否需要记录 → 异步触发 handlers → 清理上下文

## 6. 外部依赖

- AspectJ（`@Aspect/@Around/@Pointcut`）
- Spring Web（`RequestContextHolder` 获取 `HttpServletRequest`）
- Fastjson2（`JSONArray/JSONObject`）
- Hutool（`UUID`、`ArrayUtil`）

## 7. 模块协作点

- 需要一个且仅一个 `OperationLogRecordInitializer` bean（`LogAspect#getLogDetailFactory()` 使用 `applicationContext.getBean(...)` 获取单例）
- 可以存在多个 `OperationLogHandler` bean（`LogAspect` 遍历 `getBeansOfType(OperationLogHandler.class).values()`）

## 8. 常见问题（优先看哪里）

- **为什么 operationName 不是我想要的**：看 `LogAspect#around` 的优先级逻辑（`@OperationLog.value` → swagger summary → classFullName）
- **为什么入参没被序列化/变成占位字符串**：看 `LogAspect#around` 中对 Servlet/MultipartFile/InputStreamSource 的特殊处理与异常兜底
- **为什么异常时没记录日志**：看 `OperationLog#handleOnFail` 与 `isNeedRecord` 判定

