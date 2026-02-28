<div align="center">

# 🔥 operation-log-boot-starter

<p align="center">
  <strong>一站式业务操作日志解决方案</strong>
</p>

<p align="center">
  <a href="https://mvnrepository.com/artifact/cn.creekmoon/operation-log-boot-starter">
    <img src="https://maven-badges.herokuapp.com/maven-central/cn.creekmoon/operation-log-boot-starter/badge.svg" alt="Maven Central">
  </a>
  <a href="http://www.apache.org/licenses/LICENSE-2.0.html">
    <img src="http://img.shields.io/:license-apache-brightgreen.svg" alt="License">
  </a>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.0+-green.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/JDK-21+-blue.svg" alt="JDK">
</p>

<p align="center">
  <a href="#-功能特性">功能特性</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-配置说明">配置说明</a> •
  <a href="#-高级功能">高级功能</a> •
  <a href="#-api文档">API文档</a>
</p>

</div>

---

## ✨ 功能特性

<table>
<tr>
<td width="50%">

### 📝 核心能力
- **零侵入日志记录** - 一个注解自动记录操作日志
- **字段变更追踪** - 记录数据修改前后的变化
- **异步高性能** - 独立的线程池处理，不阻塞业务
- **多存储支持** - 控制台/Elasticsearch/自定义Handler

</td>
<td width="50%">

### 📊 分析能力 (v2.2+)
- **操作热力图** - 接口 PV/UV 实时统计
- **用户行为画像** - 基于操作历史生成用户标签
- **可视化 Dashboard** - 内置 Web 监控面板
- **CSV 数据导出** - 支持各类数据导出分析

</td>
</tr>
</table>

---

## 🚀 快速开始

### 1️⃣ 添加依赖

```xml
<dependency>
    <groupId>cn.creekmoon</groupId>
    <artifactId>operation-log-boot-starter</artifactId>
    <version>2.2.0</version>
</dependency>
```

### 2️⃣ 启用日志记录

```java
@SpringBootApplication
@EnableOperationLog  // ← 添加这个注解
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3️⃣ 标记需要记录的方法

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @OperationLog("创建订单")  // ← 添加这个注解
    @PostMapping
    public Order create(@RequestBody Order order) {
        // 业务逻辑...
        return orderService.create(order);
    }
    
    @OperationLog(value = "更新订单", type = "ORDER_UPDATE")
    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @RequestBody Order order) {
        // 跟踪字段变更
        OperationLogContext.follow(() -> orderService.getById(id));
        return orderService.update(id, order);
    }
}
```

### 4️⃣ 定义日志处理器

```java
@Component
public class EsLogHandler implements OperationLogHandler {
    @Override
    public void handle(LogRecord logRecord) {
        // 推送到 Elasticsearch
        elasticsearchClient.index(logRecord.toFlatJson());
    }
}
```

### 5️⃣ 查看效果

启动应用后，操作日志会自动输出到控制台：

```
operation-log: LogRecord(
  userId=10001, 
  userName=zhangsan, 
  operationName=创建订单,
  operationType=DEFAULT,
  methodName=create,
  classFullName=com.example.OrderController.create,
  requestResult=true,
  operationTime=2026-02-28T23:30:00
)
```

---

## ⚙️ 配置说明

### 基础配置

#### 方式一：详细配置（推荐）

在 `heatmap` 和 `profile` 配置段中启用全局开关：

```yaml
operation-log:
  heatmap:
    enabled: true           # 启用热力图模块
    global-enabled: true    # 全局开启所有接口的热力图统计
  
  profile:
    enabled: true           # 启用画像模块
    global-enabled: true    # 全局开启所有接口的用户画像统计
```

#### 方式二：快捷配置

也可使用根级快捷配置（与方式一等效）：

```yaml
operation-log:
  heatmap-global-enabled: true   # 快捷方式：operation-log.heatmap.global-enabled
  profile-global-enabled: true   # 快捷方式：operation-log.profile.global-enabled
  handle-on-fail-global-enabled: false
  use-value-as-type: false
```

### 热力图配置

```yaml
operation-log:
  heatmap:
    enabled: true                    # 是否启用热力图模块
    global-enabled: true             # 全局开关（优先级高于注解）
    redis-key-prefix: "oplog:heatmap" # Redis key 前缀
    realtime-retention-hours: 24     # 实时数据保留时间
    hourly-retention-days: 7         # 小时级数据保留时间
    daily-retention-days: 90         # 天级数据保留时间
    top-n-default-size: 10           # TopN 默认返回数量
    top-n-max-size: 100              # TopN 最大返回数量
    sample-rate: 1.0                 # 采样率 (0.0-1.0)
    fallback-enabled: true           # Redis 故障时降级处理
```

> 💡 **提示**: 当 `heatmap.global-enabled: true` 时，**所有**带有 `@OperationLog` 的方法都会自动启用热力图统计，无需在每个方法上添加 `heatmap = true`。

### 用户画像配置

```yaml
operation-log:
  profile:
    enabled: true                    # 是否启用画像模块
    global-enabled: true             # 全局开关
    redis-key-prefix: "oplog:profile" # Redis key 前缀
    default-stats-days: 30           # 默认统计时间范围
    operation-count-retention-days: 90  # 操作计数保留时间
    user-tags-retention-days: 90     # 用户标签保留时间
    fallback-enabled: true           # 降级策略
```

### Dashboard 配置

```yaml
operation-log:
  dashboard:
    enabled: true                    # 是否启用 Dashboard
    path: "/operation-log/dashboard"  # 访问路径
    refresh-interval: 30             # 自动刷新间隔(秒)
```

---

## 🎯 高级功能

### 📊 热力图统计

全局开启后，自动统计所有接口的访问量：

```java
// 全局开启后，无需额外配置
@OperationLog("查询订单")
@GetMapping("/orders")
public List<Order> list() {
    // 自动统计 PV/UV
    return orderService.list();
}
```

如需在特定方法上**禁用**热力图统计：

```java
// 未来版本将支持通过配置排除特定接口
// 当前可通过配置排除特定 operation-type
```

**查看统计数据**:

```bash
# 查看所有接口实时统计
curl http://localhost:8080/operation-log/heatmap/stats

# 查看指定接口统计
curl http://localhost:8080/operation-log/heatmap/stats/OrderController/list

# 查看 Top10 热门接口
curl http://localhost:localhost:8080/operation-log/heatmap/topn
```

**编程式使用**:

```java
@Autowired
private HeatmapService heatmapService;

// 获取实时统计
HeatmapStats stats = heatmapService.getRealtimeStats("OrderController", "list");
System.out.println("PV: " + stats.pv() + ", UV: " + stats.uv());

// 导出 CSV
List<List<String>> csvData = heatmapService.exportRealtimeStatsToCsv();
```

### 👤 用户行为画像

全局开启后，自动收集用户操作数据：

```java
// 全局开启后，无需额外配置
@OperationLog(value = "提交订单", type = "ORDER_SUBMIT")
@PostMapping("/orders")
public Order submit(@RequestBody Order order) {
    return orderService.submit(order);
}
```

**查看画像数据**:

```bash
# 查看用户画像
curl http://localhost:8080/operation-log/profile/user/10001

# 查看用户标签
curl http://localhost:8080/operation-log/profile/user/10001/tags

# 根据标签查询用户
curl http://localhost:8080/operation-log/profile/tag/高价值用户
```

**编程式使用**:

```java
@Autowired
private ProfileService profileService;

// 获取用户画像
UserProfile profile = profileService.getUserProfile("10001");
Set<String> tags = profile.tags();  // [高频用户, 高价值用户, 深夜活跃]
```

### 📈 可视化 Dashboard

启动应用后访问：

- **基础版 Dashboard**: `http://localhost:8080/operation-log/dashboard`
- **专业版 Dashboard**: `http://localhost:8080/operation-log-dashboard-pro.html`

**功能特性**:
- 实时 PV/UV 概览
- 24小时趋势图表
- 热门接口排行
- 用户标签分布
- 响应时间分位数 (Pro)
- 错误率趋势 (Pro)
- 地域/终端分布 (Pro)

### 📤 CSV 导出

所有统计数据支持 CSV 导出：

```bash
# 热力图数据导出
curl -o heatmap.csv http://localhost:8080/operation-log/heatmap/export/realtime
curl -o topn.csv "http://localhost:8080/operation-log/heatmap/export/topn?timeWindow=REALTIME&metricType=PV&topN=10"

# 用户画像导出
curl -o profile.csv http://localhost:8080/operation-log/profile/export/user/10001
curl -o users.csv http://localhost:8080/operation-log/profile/export/tag/高价值用户
```

---

## 📚 API 文档

### 核心注解

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | "未描述的接口" | 操作描述 |
| `type` | String | "DEFAULT" | 操作类型，用于分类统计 |
| `handleOnFail` | boolean | false | 失败时是否记录日志 |

### 全局配置 vs 注解配置

| 功能 | 全局配置 (推荐) | 注解配置 (细粒度) |
|------|----------------|-------------------|
| 热力图统计 | `heatmap.global-enabled: true` | `@OperationLog(heatmap = true)` |
| 用户画像 | `profile.global-enabled: true` | `@OperationLog(profile = true)` |
| 失败记录 | `handle-on-fail-global-enabled: true` | `@OperationLog(handleOnFail = true)` |

> 🔥 **最佳实践**: 使用全局配置统一管理，减少重复代码！

### HTTP API

#### 热力图 API

| 接口 | 说明 |
|------|------|
| `GET /operation-log/heatmap/status` | 服务状态 |
| `GET /operation-log/heatmap/stats` | 所有接口统计 |
| `GET /operation-log/heatmap/stats/{class}/{method}` | 指定接口统计 |
| `GET /operation-log/heatmap/topn` | TopN 排行 |
| `GET /operation-log/heatmap/export/realtime` | 导出实时数据 |
| `GET /operation-log/heatmap/export/topn` | 导出排行数据 |

#### 用户画像 API

| 接口 | 说明 |
|------|------|
| `GET /operation-log/profile/status` | 服务状态 |
| `GET /operation-log/profile/user/{userId}` | 用户画像 |
| `GET /operation-log/profile/user/{userId}/tags` | 用户标签 |
| `GET /operation-log/profile/user/{userId}/stats` | 操作统计 |
| `GET /operation-log/profile/tag/{tagName}` | 标签用户列表 |

#### Dashboard Pro API

| 接口 | 说明 |
|------|------|
| `GET /operation-log/dashboard/api/response-time` | 响应时间分位数 |
| `GET /operation-log/dashboard/api/error-rate` | 错误率趋势 |
| `GET /operation-log/dashboard/api/geo-distribution` | 地域分布 |
| `GET /operation-log/dashboard/api/terminal-distribution` | 终端分布 |

---

## 🔧 常见问题

### Q: 热力图数据占用多少 Redis 内存？

A: 基于 HyperLogLog 算法，千万级 UV 统计仅需约 **12KB** 内存。

### Q: 如何排除特定接口的热力图统计？

A: 当前版本可通过配置排除特定 operation-type：

```yaml
operation-log:
  heatmap:
    exclude-operation-types:
      - HEALTH_CHECK
      - PING
```

### Q: Redis 故障会影响业务吗？

A: 不会。启用 `fallback-enabled: true` 后，Redis 故障会自动降级，不影响业务功能。

### Q: Dashboard 访问需要认证吗？

A: 当前版本 Dashboard 为公开访问，生产环境建议通过反向代理添加认证。

---

## 📄 许可证

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

---

<div align="center">

**Made with ❤️ by creekmoon**

</div>
