# operation-log-boot-starter
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.yinjiangyue/operation-log-boot-starter/badge.svg)](https://mvnrepository.com/artifact/io.github.yinjiangyue/operation-log-boot-starter)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

## 能做什么?  
简易的业务操作日志AOP实现类, 用于记录业务中的Controller的操作日志,能记录用户什么时候修改了哪些字段

#### maven引用方式

```xml

<dependency>
    <groupId>io.github.yinjiangyue</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>1.1.2</version>
</dependency>
```

#### 踩雷警告! 使用前注意!!

个人水平非常非常有限,这个工具性能比较差,而且应该有很多BUG  
这个工具需要额外依赖

* fastjson
* hutool-core
* lombok

## 快速开始

首先在**SpringBoot启动类**加上**@EnableOperationLog**注解

```java

@EnableOperationLog //在启动类加上注解
@ComponentScan(basePackages = {"com.vdp", "com.wtx.mgt"})
public class VdpWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(VdpWebApplication.class, args);
    }
} 
```

在**controller方法**上加入注解 **@OperationLog**

```java

@RequestMapping("web/test")
public class TTransportController {
    
    @OperationLog //在此加上注解
    @PostMapping(value = "/update")
    public ReturnValue update(TTransport tTransport) {
       /*业务代码*/
       return SUCCESS; 
    }
}

```

## 查看效果

如果能够成功启动.那么会将每次操作Controller的情况打印到控制台.

```text
operation-log:LogRecord(userId=1, orgId=1, userName=unknown  ..........省略
```

#### 实体类说明
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

## 更进一步使用

### 跟踪值的变化

关键代码
```java
OperationLogContext.follow(Supplier supplier);
```
 
你需要提供**回调函数**(Supplier),告诉组件该如何去获取对象.只有这样才能够正确地跟踪到变化的值.    
它将在方法**开始时**和方法**结束时**分别调用一次,最后会对比两者的变化.  
你可以在**LogRecord.effectFields**看到受影响的字段. 

示例: 这是一个修改接口,日志会跟踪变化而得知用户修改了哪一些值
```java
import cn.jy.operationLog.core.OperationLogContext;

public class TTransportController {

    @PostMapping(value = "/update")
    @OperationLog
    public ReturnValue update(TTransport tTransport) {
        //设置一个追踪的目标
        OperationLogContext.follow(()->{return getDeatil(tTransport.getId());});
        //业务代码
        return ReturnValue.success();
    }
}
```

### 定义日志该如何处理

**实现OperationLogHandler接口并加上@Component注解**

这里需要定义做一件事情:
* 如何处理logRecord对象 (例如存储到Elastic或者Mysql)

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


### 完善更多的日志详情



#### 实现OperationLogDetailProvider接口并加上@Component注解

 
* getUserName 用户名 
* getUserId 用户id 
* getOrgId 用户机构id 
* getProjectName 当前的项目名称
* requestIsFail 本次操作是否失败(详见下方说明) 默认值 false

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


### 定义日志是否应该进行记录

#### 避免垃圾日志
默认只要不抛出异常,都会进行日志记录.

日志的记录存在一个缺陷: **不能够意识到执行结果所代表的业务意义**.

举个例子: update接口执行成功, 但返回的信息提示"车辆运行中,无法修改数据".

**我们不想产生过多无意义的垃圾日志**,所以针对这种**业务层面的失败**,我们期望它不要进行日志记录.

所以,我们需要**自定义requestIsFail方法**, 告诉日志组件,如果有错误信息,就应该抛弃本次日志.

* 结果为true 不进行日志处理 即不会执行handle()
* 结果为false 进行日志处理 即不会执行handle()
```java
    default Boolean requestIsFail(LogRecord logRecord,Object returnValue) {
        if(returnValue instanceof ReturnVaule){
           return  ((ReturnVaule)returnValue).getErrorMsg != null;
        }
        return false;
    }
```

#### 例外
如果你的OperationLog注解上标记有 **handleOnFail = true** 则会忽略上面的requestIsFail结果,无论如何都会生成日志 即一定会执行handle()
```java
@OperationLog(handleOnFail = true)
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


