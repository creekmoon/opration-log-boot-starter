# operation-log-starter

简易的业务操作日志AOP实现类, 用于记录业务中的Controller的操作日志,能记录用户什么时候修改了哪些字段

#### maven引用方式

```xml

<dependency>
    <groupId>io.github.yinjiangyue</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 踩雷警告! 使用前注意!!

个人水平非常非常有限,这个工具性能比较差,而且应该有很多BUG  
这个工具需要额外依赖

* fastjson
* hutool-all
* lombok

### 开始使用

首先在SpringBoot启动类加上注解@EnableOperationLog

```java

@EnableOperationLog
@ComponentScan(basePackages = {"com.vdp", "com.wtx.mgt"})
public class VdpWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(VdpWebApplication.class, args);
    }

} 
```

在需要记录操作的controller方法上加入注解 @OperationLog

```java
@RequestMapping("web/{version}/tTransport")
public class TTransportController {

    @PostMapping(value = "/update")
    @OperationLog
    public ReturnValue update(TTransport tTransport) {

    }
}

```

### 查看效果

如果能够成功启动.那么会将操作的情况打印到控制台.

### 进行自定义

**实现OperationLogHandler接口**  
实现类需要加入@Component注解  
这里需要做两件事情:

* 如何定义请求成功状态(操作失败时可以不进行记录)
* 如何处理数据 (自己存储到Elastic或者Mysql)

```java

@Component
public class MyOperationLogHandler implements OperationLogHandler {
    @Override
    public boolean requestIsSuccess(Object methodResult) {
        return methodResult instanceof ReturnValue && ((ReturnValue) methodResult).isSuccess();
    }

    /*这个save方法是异步的*/
    @Override
    public void save(LogRecord logRecord) {

        System.out.println(logRecord.toString());
    }
}

```

**实现OperationLogUserInfoProvider接口**  
实现类需要加入@Component注解  
这里定义如何获取当前用户的信息


```java

public interface OperationLogUserInfoProvider {

    String getUserName();

    Long getUserId();

    Long getOrgId();

    String getProjectName();
}

