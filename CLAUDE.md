# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3 starter library for operation logging. It provides an AOP-based solution to automatically log controller method invocations, including request parameters, user information, and field change tracking.

**Published Artifact**: `io.github.creekmoon:operation-log-boot-starter:2.1.2` on Maven Central

## Build Commands

```bash
# Compile the project
mvn clean compile

# Install to local repository
mvn clean install

# Deploy to Maven Central (requires GPG signing)
mvn clean deploy
```

**Note**: The build may show javadoc warnings/errors due to comment formatting. These are expected and handled by `<doclint>none</doclint>` in the POM configuration.

## Project Structure

Multi-module Maven project:
- **Parent** (`operation-log-parent`): Aggregator POM with dependency management
- **Module** (`operation-log-boot-starter/`): The actual starter library

Source code is in `operation-log-boot-starter/src/main/java/cn/creekmoon/operationLog/`:
- `config/` - Configuration classes and enablement annotations
- `core/` - Core logging functionality (annotations, aspects, handlers)

## Architecture

### Core Components

1. **`@OperationLog`** - Annotation placed on controller methods to trigger logging
2. **`LogAspect`** - AOP aspect that intercepts annotated methods, captures parameters, and delegates to handlers
3. **`LogRecord`** - Data object containing all log information (user, operation, parameters, before/after values)
4. **`OperationLogContext`** - Thread-local storage for request-scoped logging state. Must be cleaned after each request (handled by aspect)
5. **`LogThreadPool`** - Dedicated thread pool for asynchronous log processing

### Extension Points

Users implement these interfaces to customize behavior:

- **`OperationLogHandler`** - Defines how logs are processed (e.g., console output, Elasticsearch). Multiple handlers can coexist; all beans implementing this interface are invoked.
- **`OperationLogRecordInitializer`** - Initializes the `LogRecord` with user context (userId, userName, etc.). Only one bean should implement this.

Default implementations (`DefaultOperationLogHandler`, `DefaultOperationLogRecordInitializer`) are auto-configured via `@ConditionalOnMissingBean`.

### Data Flow

1. Method annotated with `@OperationLog` is called
2. `LogAspect.around()` intercepts the call
3. `LogRecord` is initialized via `OperationLogRecordInitializer`
4. Method parameters are serialized and stored
5. Original method executes
6. If `OperationLogContext.follow()` was called, the "after" value is captured
7. Log is processed asynchronously via `LogThreadPool` through all registered `OperationLogHandler` beans
8. Context is cleaned via `OperationLogContext.clean()`

### Thread Safety

- `OperationLogContext` uses `ThreadLocal` for per-request isolation
- `ConcurrentHashMap` stores log records keyed by UUID
- The aspect checks for nested `@OperationLog` annotations and skips if already processing

## Usage Pattern

```java
// 1. Enable in main class
@EnableOperationLog
@SpringBootApplication
public class Application { ... }

// 2. Annotate controller methods
@OperationLog("Update User")
@PostMapping("/user/update")
public Result update(User user) { ... }

// 3. Track field changes (optional)
@OperationLog("Update Order")
@PostMapping("/order/update")
public Result updateOrder(Long orderId, Order newOrder) {
    OperationLogContext.follow(() -> orderService.getById(orderId));
    orderService.update(newOrder);
    return Result.ok();
}
```

## Dependencies

- Spring Boot 3.0.12+
- JDK 21
- AspectJ for AOP
- Fastjson2 for JSON serialization
- Hutool for utilities

## Maven Release Notes

- GPG signing is required for deployment (Gpg4win on Windows)
- Source and Javadoc jars are auto-generated
- Distribution goes to Sonatype OSS (s01.oss.sonatype.org)
