# operation-log-starter
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.yinjiangyue/operation-log-boot-starter/badge.svg)](https://mvnrepository.com/artifact/io.github.yinjiangyue/operation-log-boot-starter)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
简易的业务操作日志AOP实现类, 用于记录业务中的Controller的操作日志,能记录用户什么时候修改了哪些字段

#### maven引用方式

```xml

<dependency>
    <groupId>io.github.yinjiangyue</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>1.1.2</version>
</dependency>
```

## 踩雷警告! 使用前注意!!

个人水平非常非常有限,这个工具性能比较差,而且应该有很多BUG  
这个工具需要额外依赖

* fastjson
* hutool-core
* lombok

### 快速开始

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
public class TTransportController {

    @PostMapping(value = "/update")
    @OperationLog
    public ReturnValue update(TTransport tTransport) {

    }
}

```

### 查看效果

如果能够成功启动.那么会将每次操作的情况打印到控制台.

```text
operation-log:LogRecord(userId=1, orgId=1, userName=unknown  ..........省略
```

### 实体类说明
LogRecord是记录当前的操作信息实体类
* 当前操作的用户userId/userName 
* 当前操作的机构orgId
* 当前的项目名称projectName
* 传入的参数值 requestParams
* 本次修改前/修改后的值 preValue/afterValue
* 受到本次影响的字段 effectFields/effectFieldsBefore/effectFieldsAfter
* 标记点 markPoints 用于辅助ES查询的字段

```JSON
{
  "userId": "1",  
  "orgId": "1",   
  "userName": "unknown",
  "projectName": "unknown",
  "requestParams": {
    "id": 5,
    "carrierId": 5,
    "driverId": 13,
    "vehicleId": 58
  },
  "effectFields": [
    "updateTime",
    "carrierId"
  ],
  "effectFieldsAfter": {
    "updateTime": "2022-05-13 17:15:01",
    "carrierId": 5
  },
  "effectFieldsBefore": {
    "updateTime": "2022-05-13 17:17:17",
    "carrierId": 1
  },
  "preValue": {
    "updateTime": "2022-05-13 17:17:17",
    "carrierId": 1,
    "driverId": 13,
    "vehicleId": 58
  },
  "afterValue": {
    "updateTime": "2022-05-13 17:17:17",
    "carrierId": 1,
    "driverId": 13,
    "vehicleId": 58
  },
  "markPoints": []
}
```

如上文所示,本次请求传入了四个参数, 最终修改了carrierId和updateTime两个属性

### 进行自定义

**跟踪值的变化**

关键代码  
`OperationLogContext.follow(Producer)`  
其接收一个Supplier,它将在方法**开始时**和方法**结束时**分别调用两次,最后会对比两者的变化.  
你可以在**LogRecord.effectFields**看到受影响的字段. 

示例: 这是一个修改接口,日志会跟踪变化而得知用户修改了哪一些值
```java
import cn.jy.operationLog.core.OperationLogContext;

public class TTransportController {

    @PostMapping(value = "/update")
    @OperationLog
    public ReturnValue update(TTransport tTransport) {
        OperationLogContext.follow(()->{return getDeatil(tTransport.getId());});
        //业务代码
        return ReturnValue.success();
    }
}

```

**实现OperationLogDetailProvider接口并加上@Component注解**

这里定义
* 用户的信息来源
* 未抛出异常的情况下,本次操作是否失败(默认值是false 等价于只要不抛出异常,都认为接口执行成功)


```java

public interface OperationLogDetailProvider {

    String getUserName();

    Long getUserId();

    Long getOrgId();

    String getProjectName();

    default Boolean requestIsFail(LogRecord logRecord,Object returnValue) {
        return false;
    }
}
```

**实现OperationLogHandler接口并加上@Component注解**  

这里需要定义做一件事情:
* 如何处理logRecord (例如存储到Elastic或者Mysql)

```java

public interface OperationLogHandler {
    
    /**
     * 定义如何处理这个日志(这个方法会作为异步执行)
     *
     * @param logRecord
     */
    void handle(LogRecord logRecord);


}
```

## 对ElasticSearch的支持

### 使用步骤
* 在SpringBoot的Main方法上, 使用注解 **@EnableOperationLogElasticSearchSupport**
* 在application.yaml配置ES的连接参数

### 配置举例
```java
@EnableOperationLogElasticSearchSupport
@EnableOperationLog
@ComponentScan(basePackages = {"com.vdp", "com.wtx.mgt"})
public class VdpWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(VdpWebApplication.class, args);
    }

} 
```

```yaml
operation-log:
  host-name: 192.168.1.109
  port: 9200
  index-name: web-logs
  user-name: elastic
  password: elastic
```


