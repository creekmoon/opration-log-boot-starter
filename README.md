# operation-log-boot-starter
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.creekmoon/operation-log-boot-starter/badge.svg)](https://mvnrepository.com/artifact/io.github.creekmoon/operation-log-boot-starter)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

## 能做什么?  
简易的业务操作日志AOP实现类, 用于记录业务中的Controller的操作日志,能记录用户什么时候修改了哪些字段

#### maven引用方式

```xml

<dependency>
    <groupId>io.github.creekmoon</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>1.3.0</version>
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
* 标签 tags 用于辅助ES查询的字段

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
  "tags": []
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
import cn.creekmoon.operationLog.core.OperationLogContext;

public class TTransportController {

    @PostMapping(value = "/update")
    @OperationLog
    public ReturnValue update(TTransport tTransport) {
        //设置一个追踪的目标
        OperationLogContext.follow(() -> {
            return getDeatil(tTransport.getId());
        });
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

### 定义自己的日志对象

**实现OperationLogRecordFactory接口并加上@Component注解**

这里需要定义做一件事情:

* 如何初始化LogRecord对象 (例如设置一些默认值,当然你也可以创建一个继承于它的类,以便拓展它的属性)

```java

@Component
public class DefaultOperationLogRecordFactory implements OperationLogRecordFactory {
    @Override
    public LogRecord createNewLogRecord() {
        /*初始化LogRecord*/
        LogRecord logRecord = new LogRecord();
        logRecord.setOrgId(999L);
        logRecord.setUserId(999L);
        logRecord.setUserName("lalala");
        logRecord.setProjectName("web-project");
        return logRecord;
    }
}

```

### 定义日志是否应该进行记录

默认**不会记录失败**的日志.除非手动指定 handleOnFail = true 因为我不希望产生过多的垃圾日志,因为如果操作失败,通常都会回滚数据,失败的操作实际上是没有意义的

在组件中,有两种方法标记日志失败:

- 自动标记失败,只要抛出异常,就会标记日志操作失败

```java
//如果你的OperationLog注解上标记有 **handleOnFail = true** 则会忽略任何错误,也会执行handle()
@OperationLog(handleOnFail = true)
```

- 手动标记失败,如下

```java
//代码任意一个地方,执行    
OperationLogContext.fail();
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
  elastic:
    user-name: elastic
    port: 9200
    index-name: test-opt-220702
    password: qwe45678
    host: 121.5.52.88
```
## 常见错误解决
#### 依赖问题
```xml
        <dependencies>
        <!--如果引用时报NoSuchMethod转换错误，可以引用这个包-->
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
           <artifactId>httpclient</artifactId>
           <version>4.5.13</version>
        </dependency>
    <!--如果引用时报ES的客户端错误，可以引用这个包-->
    <dependency>
        <groupId>jakarta.json</groupId>
        <artifactId>jakarta.json-api</artifactId>
        <version>2.1.0</version>
    </dependency>
</dependencies>
```

#### Elastic索引问题
如果提示索引上限达到1000个 需要为ES的索引进行配置 (直接去Kibana可视化配置就好,不需要重启)
```yaml
"index.mapping.total_fields.limit": "5000",
```

清理Elastic索引数据

```json
POST walmart-operation-log/_delete_by_query
{
  "query": {
    "range": {
      "operationTime": {
        "lt": "2022-09-05T02:20:05.231Z"
      }
    }
  }
}



```

定义了Elastic索引声明周期, 但是删除阶段没有奏效 可以看这个人的文章

[这个人的文章]: https://blog.csdn.net/m0_60696455/article/details/119736496



> 这貌似是源于kibana的一个BUG,使用kibana创建索引声明周期时, actions为空
> 所以需要去kibana管理声明周期那里,复制一下它的更新语句, 然后为delete阶段添加一个action
> 如下所示, 只展示需改动的部分:

```json

//之前的
{
  "delete": {
    "min_age": "30d",
    "actions": {
    }
  }
}

//添加action之后的
{
  "delete": {
    "min_age": "30d",
    "actions": {
      "delete": {}
    }
  }
}

```