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
    <version>2.1.2</version>
</dependency>
```

## 使用条件

Spring Boot 3.0.0+

JDK >= 17

## 快速开始

首先在启动类加上**EnableOperationLog**注解

```java

@EnableOperationLog //在启动类加上注解
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

在您没有定义日志应该如何处理之前, 组件默认将日志打印到控制台

会输出以下文字.
```text
operation-log:LogRecord(userId=1, orgId=1, userName=unknown  ..........省略
```

## 方法说明

### 跟踪值的变化

```java
OperationLogContext.follow(()->getStudentInfo(studentId));
        updateStudentInfo(studentId);
```


### 定义日志该如何处理

实现**OperationLogHandler**接口
```java

@Component
public class PushElasticSearch implements OperationLogHandler {

    @Override
    public void handle(LogRecord logRecord) {
        pushES(logRecord);
    }

}

```

### 定义当前用户

实现**OperationLogRecordInitializer接口**
```java
@Component
public class DefaultOperationLogRecordInitializer implements OperationLogRecordInitializer {

    @Override
    public LogRecord init(LogRecord logRecord) {
        /*获取当前的用户*/
        logRecord.setUserId(getCurrentUserId());
        return logRecord;
    }
}

```

### 定义日志结果

如果您本次操作抛出了**异常**, 或者日志被标记了**失败**,

则日志不会被处理.除非您手动指定 **handleOnFail = true**


```java
@OperationLog(handleOnFail = true)
```

在组件中,有两种方法标记当前操作失败:

- 向外抛出异常,自动标记失败

- 手动标记失败

```java
OperationLogContext.fail();
```


## 常见错误解决

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