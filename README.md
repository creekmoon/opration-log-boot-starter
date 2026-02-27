# operation-log-boot-starter
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cn.creekmoon/operation-log-boot-starter/badge.svg)](https://mvnrepository.com/artifact/cn.creekmoon/operation-log-boot-starter)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

## 能做什么?  
简易的业务操作日志AOP实现类, 用于记录业务中的Controller的操作日志,能记录用户什么时候修改了哪些字段

**新增功能(v2.2.0+):**
- **操作热力图统计**: 基于Redis HyperLogLog统计接口PV/UV,支持实时/小时/天级维度
- **用户行为画像**: 基于用户操作历史自动生成行为标签,支持精细化运营

#### maven引用方式

```xml

<dependency>
    <groupId>cn.creekmoon</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>2.1.3</version>
</dependency>
```

## 使用条件

Spring Boot 3.0.0+

JDK >= 21

Redis (用于热力图和用户画像功能)

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

## 新增功能: 操作热力图统计

### 开启方式

在 `@OperationLog` 注解上添加 `heatmap = true`:

```java
@OperationLog(heatmap = true)
@GetMapping("/list")
public List<Order> list() {
    // 业务代码
}
```

### 配置项

```yaml
operation-log:
  heatmap:
    enabled: true                    # 是否启用热力图统计
    redis-key-prefix: "operation-log:heatmap"  # Redis key前缀
    realtime-retention-hours: 24     # 实时数据保留时间(小时)
    hourly-retention-days: 7         # 小时级数据保留时间(天)
    daily-retention-days: 90         # 天级数据保留时间(天)
    top-n-default-size: 10           # TopN查询默认返回数量
    top-n-max-size: 100              # TopN查询最大返回数量
    fallback-enabled: true           # 是否启用降级策略
    sample-rate: 1.0                 # 采样率(0.0-1.0)
```

### 查看数据

通过Actuator端点访问:

```bash
# 查看服务状态
GET /actuator/operation-log-heatmap

# 查看所有接口实时统计
GET /actuator/operation-log-heatmap/stats

# 查看指定接口统计
GET /actuator/operation-log-heatmap/stats/{className}/{methodName}

# 查看TopN接口
GET /actuator/operation-log-heatmap/topn
```

### 编程式使用

```java
@Autowired
private HeatmapService heatmapService;

// 获取实时统计
HeatmapStats stats = heatmapService.getRealtimeStats("OrderService", "list");
System.out.println("PV: " + stats.pv() + ", UV: " + stats.uv());

// 获取Top10接口(PV)
List<HeatmapTopItem> topList = heatmapService.getTopN(
    TimeWindow.REALTIME, MetricType.PV, 10);

// 获取趋势数据
List<HeatmapTrendPoint> trend = heatmapService.getTrend(
    "OrderService", "list", TimeWindow.HOURLY, 24);
```

## 新增功能: 用户行为画像

### 开启方式

在 `@OperationLog` 注解上添加 `profile = true`,并定义操作类型:

```java
@OperationLog(value = "查询订单", type = "ORDER_QUERY", profile = true)
@GetMapping("/list")
public List<Order> list() {
    // 业务代码
}

@OperationLog(value = "提交订单", type = "ORDER_SUBMIT", profile = true)
@PostMapping("/submit")
public Result submit(@RequestBody Order order) {
    // 业务代码
}
```

### 配置项

```yaml
operation-log:
  profile:
    enabled: true                    # 是否启用用户画像
    redis-key-prefix: "operation-log:user-profile"  # Redis key前缀
    default-stats-days: 30           # 默认统计时间范围(天)
    operation-count-retention-days: 90  # 操作计数保留时间(天)
    user-tags-retention-days: 90     # 用户标签保留时间(天)
    tag-engine-enabled: true         # 是否启用标签规则引擎
    fallback-enabled: true           # 是否启用降级策略
```

### 自定义标签规则

```yaml
operation-log:
  profile:
    tag-rules:
      - name: "VIP用户"
        condition: "ORDER_SUBMIT > 20 AND ORDER_REFUND < 3"
        priority: 10
        description: "下单超过20次且退款少于3次"
      - name: "羊毛党"
        condition: "ORDER_REFUND > 10"
        priority: 5
        description: "退款超过10次"
```

### 查看数据

通过Actuator端点访问:

```bash
# 查看服务状态
GET /actuator/operation-log-profile

# 查看用户画像
GET /actuator/operation-log-profile/user/{userId}

# 查看用户标签
GET /actuator/operation-log-profile/user/{userId}/tags

# 查看用户操作统计
GET /actuator/operation-log-profile/user/{userId}/stats

# 根据标签查询用户
GET /actuator/operation-log-profile/tag/{tagName}
```

### 编程式使用

```java
@Autowired
private ProfileService profileService;

// 获取用户画像
UserProfile profile = profileService.getUserProfile("user123");
System.out.println("用户标签: " + profile.tags());
System.out.println("操作统计: " + profile.operationStats());

// 获取用户标签
Set<String> tags = profileService.getUserTags("user123");

// 根据标签查询用户
List<String> users = profileService.getUsersByTag("高价值用户", 0, 20);
long count = profileService.getUserCountByTag("高价值用户");

// 刷新用户标签
profileService.refreshUserTags("user123");
```

### 内置标签规则

组件默认提供以下标签规则:

| 标签名称 | 规则条件 | 说明 |
|---------|---------|------|
| 高频查询用户 | ORDER_QUERY > 50 | 查询操作超过50次 |
| 高价值用户 | ORDER_SUBMIT > 10 AND ORDER_REFUND < 2 | 下单超过10次且退款少于2次 |
| 潜在流失用户 | ORDER_QUERY > 30 AND ORDER_SUBMIT = 0 | 查询超过30次但从不下单 |
| 高频退款用户 | ORDER_REFUND > 5 | 退款超过5次 |

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