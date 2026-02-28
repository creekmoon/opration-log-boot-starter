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
  <a href="#-集成示例">集成示例</a> •
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

## 🎯 为什么选择我们？

### 核心优势：极致轻量级

| 特性 | 说明 |
|------|------|
| **零外部依赖** | 后端仅依赖 Spring Boot + Redis，无数据库、消息队列等其他依赖 |
| **一分钟集成** | 一个依赖、一个注解，即可开始记录操作日志 |
| **高性能** | 异步日志处理，Redis HyperLogLog 高效统计，不影响业务性能 |
| **易维护** | 轻量级代码库，源码易懂，问题易排查 |

### 技术栈

- **后端**: Spring Boot 3.x, Redis
- **前端**: 原生 HTML/CSS/JavaScript (轻量级 CDN 可选)
- **JDK**: 21+

### 我们不做什么 ❌

为了保持轻量级，我们**不会**引入以下依赖：
- 数据库 (MySQL/PostgreSQL/Oracle)
- 消息队列 (Kafka/RabbitMQ/RocketMQ)
- 搜索引擎 (Elasticsearch/Solr) - *可选自定义 Handler 接入*
- 其他重量级中间件

> 💡 **设计哲学**: 只做好一件事——操作日志记录。数据存储和搜索交给你的业务系统决定。

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
public class ConsoleLogHandler implements OperationLogHandler {
    @Override
    public void handle(LogRecord logRecord) {
        // 输出到控制台（默认行为）
        System.out.println("[OperationLog] " + logRecord.getOperationName());
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

### 完整配置示例

```yaml
operation-log:
  # ========== 全局快捷配置 ==========
  heatmap-global-enabled: false       # 是否全局启用热力图统计，默认false
  profile-global-enabled: false       # 是否全局启用用户画像统计，默认false
  handle-on-fail-global-enabled: false # 是否全局默认在失败时记录日志，默认false
  use-value-as-type: false            # 是否全局使用value作为操作类型，默认false
  
  # ========== 热力图模块配置 ==========
  heatmap:
    enabled: true                           # 是否启用热力图模块，默认true
    redis-key-prefix: "operation-log:heatmap" # Redis key前缀，默认"operation-log:heatmap"
    realtime-retention-hours: 24            # 实时数据保留时间(小时)，默认24
    hourly-retention-days: 7                # 小时级数据保留时间(天)，默认7
    daily-retention-days: 90                # 天级数据保留时间(天)，默认90
    top-n-default-size: 10                  # TopN查询默认返回数量，默认10
    top-n-max-size: 100                     # TopN查询最大返回数量，默认100
    sample-rate: 1.0                        # 采样率(0.0-1.0)，默认1.0
    fallback-enabled: true                  # 是否启用降级策略，默认true
    fallback-max-size: 1000                 # 降级时最大本地缓存数量，默认1000
    exclude-operation-types: []             # 排除统计的操作类型列表，默认空
  
  # ========== 用户画像模块配置 ==========
  profile:
    enabled: true                               # 是否启用画像模块，默认true
    auto-infer-type: true                       # 是否自动推断操作类型，默认true
    redis-key-prefix: "operation-log:user-profile" # Redis key前缀，默认"operation-log:user-profile"
    default-stats-days: 30                      # 默认统计时间范围(天)，默认30
    operation-count-retention-days: 90          # 操作计数保留时间(天)，默认90
    fallback-enabled: true                      # 是否启用降级策略，默认true
    async-queue-size: 512                       # 异步更新队列大小，默认512
  
  # ========== Dashboard模块配置 ==========
  dashboard:
    enabled: true           # 是否启用Dashboard，默认true
    refresh-interval: 30    # 自动刷新间隔(秒)，默认30
  
  # ========== CSV导出配置 ==========
  export:
    csv:
      enabled: true         # 是否启用CSV导出，默认true
      with-bom: true        # 是否带BOM(Excel兼容)，默认true
      delimiter: ','        # CSV分隔符，默认','
      max-export-rows: 10000 # 单次导出最大行数，默认10000
      file-name-prefix: "export" # 文件名前缀，默认"export"
```

### 配置项详细说明

#### 全局配置 (operation-log.*)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `heatmap-global-enabled` | boolean | false | 全局启用热力图统计，所有`@OperationLog`方法自动统计 |
| `profile-global-enabled` | boolean | false | 全局启用用户画像统计 |
| `handle-on-fail-global-enabled` | boolean | false | 全局配置：失败时是否记录日志（对应注解的`handleOnFail`） |
| `use-value-as-type` | boolean | false | 全局使用`value`作为`operationType` |

#### 热力图配置 (operation-log.heatmap.*)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 是否启用热力图模块 |
| `redis-key-prefix` | String | "operation-log:heatmap" | Redis键前缀 |
| `realtime-retention-hours` | int | 24 | 实时数据保留时间（小时） |
| `hourly-retention-days` | int | 7 | 小时级数据保留时间（天） |
| `daily-retention-days` | int | 90 | 天级数据保留时间（天） |
| `top-n-default-size` | int | 10 | TopN查询默认返回数量 |
| `top-n-max-size` | int | 100 | TopN查询最大返回数量 |
| `sample-rate` | double | 1.0 | 采样率（0.0-1.0）|
| `fallback-enabled` | boolean | true | Redis故障时是否启用降级 |
| `fallback-max-size` | int | 1000 | 降级时本地缓存最大数量 |
| `exclude-operation-types` | List | [] | 排除统计的操作类型列表 |

#### 用户画像配置 (operation-log.profile.*)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 是否启用画像模块 |
| `auto-infer-type` | boolean | true | 是否自动从value推断操作类型 |
| `redis-key-prefix` | String | "operation-log:user-profile" | Redis键前缀 |
| `default-stats-days` | int | 30 | 默认统计时间范围（天） |
| `operation-count-retention-days` | int | 90 | 操作计数保留时间（天） |
| `fallback-enabled` | boolean | true | 是否启用降级策略 |
| `async-queue-size` | int | 512 | 异步更新队列大小 |

#### Dashboard配置 (operation-log.dashboard.*)

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 是否启用Dashboard |
| `refresh-interval` | int | 30 | 自动刷新间隔（秒） |
| `auth-mode` | enum | `OFF` | 访问控制模式：`OFF`/`IP_ONLY`/`TOKEN_ONLY`/`IP_AND_TOKEN` |
| `allow-ips` | List | `[]` | IP白名单，支持精确IP或CIDR格式（如 `192.168.1.0/24`）|
| `auth-token` | String | `""` | Token认证密钥，生产环境建议从环境变量读取 |
| `token-header` | String | `X-Dashboard-Token` | Token请求头名称 |
| `allow-token-in-query` | boolean | false | 是否允许通过Query参数传递Token |
| `auth-failure-message` | String | `Dashboard access denied` | 认证失败时的响应消息 |

> 💡 **提示**: Dashboard 访问路径固定为 `/operation-log/dashboard`，如需自定义请通过反向代理（Nginx）实现。

**四种认证模式说明：**

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `OFF` | 无认证，直接访问 | 本地开发环境 |
| `IP_ONLY` | 仅IP白名单校验 | 内网环境，固定IP场景 |
| `TOKEN_ONLY` | 仅Token认证 | 外网环境，需动态分发Token |
| `IP_AND_TOKEN` | IP白名单 + Token双重认证（推荐生产环境） | 高安全性要求的生产环境 |

> 🔐 **安全建议**: 生产环境建议使用 `IP_AND_TOKEN` 模式，双重保护更安全。

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

```yaml
operation-log:
  heatmap:
    exclude-operation-types:
      - HEALTH_CHECK
      - PING
      - METRICS
```

**查看统计数据**:

```bash
# 查看所有接口实时统计
curl http://localhost:8080/operation-log/heatmap/stats

# 查看指定接口统计
curl http://localhost:8080/operation-log/heatmap/stats/OrderController/list

# 查看 Top10 热门接口
curl http://localhost:8080/operation-log/heatmap/topn
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

**零配置开箱即用**

```yaml
operation-log:
  profile-global-enabled: true    # 全局开启
  profile:
    auto-infer-type: true         # 自动推断操作类型
```

开启后，**无需任何额外配置**，系统会自动：
1. 从 `@OperationLog("xxx")` 的描述中推断操作类型（查询/创建/更新/删除）
2. 记录用户的操作统计数据

```java
@RestController
public class OrderController {
    
    // 自动推断为 QUERY 类型
    @OperationLog("查询订单")
    @GetMapping("/orders")
    public List<Order> list() {
        return orderService.list();
    }
    
    // 自动推断为 CREATE 类型
    @OperationLog("创建订单")
    @PostMapping("/orders")
    public Order create(@RequestBody Order order) {
        return orderService.create(order);
    }
}
```

**操作统计功能**

用户画像会自动统计以下数据：

| 统计维度 | 说明 |
|----------|------|
| 操作次数 | 按操作类型统计（查询/创建/更新/删除） |
| 时间分布 | 按小时段统计活跃情况 |
| 趋势分析 | 7天/30天操作趋势 |

**查看画像数据**:

```bash
# 查看用户画像
curl http://localhost:8080/operation-log/profile/user/10001

# 查看用户操作统计
curl http://localhost:8080/operation-log/profile/user/10001/stats
```

**编程式使用**:

```java
@Autowired
private ProfileService profileService;

// 获取用户画像
UserProfile profile = profileService.getUserProfile("10001");
Set<String> tags = profile.tags();  // [高频用户, 查询型用户, 工作时间用户]
```

### 📈 可视化 Dashboard

启动应用后访问：

- **Dashboard**: `http://localhost:8080/operation-log/dashboard`

**功能特性**:
- 实时 PV/UV 概览
- 24小时趋势图表
- 热门接口排行
- 用户标签分布
- 响应时间分位数
- 错误率趋势

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

## 🔌 集成示例

### Dashboard 安全配置示例

Dashboard 已内置 IP 白名单 + Token 认证双重保护，无需额外编写代码。

#### 1. 开发环境配置（无认证）

```yaml
operation-log:
  dashboard:
    enabled: true
    auth-mode: OFF    # 开发环境关闭认证
```

#### 2. 测试环境配置（IP白名单）

```yaml
operation-log:
  dashboard:
    enabled: true
    auth-mode: IP_ONLY
    allow-ips:
      - "127.0.0.1"
      - "192.168.1.100"       # 测试服务器IP
      - "192.168.1.0/24"      # 测试网段（CIDR格式）
```

#### 3. 生产环境配置（双重认证 - 推荐）

```yaml
operation-log:
  dashboard:
    enabled: true
    auth-mode: IP_AND_TOKEN    # 双重认证模式
    refresh-interval: 60       # 生产环境60秒刷新
    allow-ips:
      - "127.0.0.1"
      - "192.168.1.0/24"      # 运维网段
    auth-token: ${DASHBOARD_TOKEN:}   # 从环境变量读取，拒绝硬编码
    token-header: X-Dashboard-Token    # Token请求头名称
    allow-token-in-query: false        # 禁用Query传Token，更安全
    auth-failure-message: "Access Denied - Contact Ops Team"
```

**获取 Token 的方式：**

```bash
# 方式1：通过 Header 传递（推荐）
curl -H "X-Dashboard-Token: your-secret-token" \
     http://localhost:8080/operation-log/dashboard

# 方式2：通过 Query 参数传递（需启用 allow-token-in-query: true）
curl http://localhost:8080/operation-log/dashboard?token=your-secret-token
```

#### 4. 与 Spring Security 集成（可选）

如需更复杂的权限控制（如 LDAP/OAuth2），可集成 Spring Security：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Dashboard 只允许 ADMIN 角色访问
                .requestMatchers("/operation-log/dashboard/**").hasRole("ADMIN")
                // API 端点允许认证用户访问
                .requestMatchers("/operation-log/api/**").authenticated()
                // 其他请求放行
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
```

> ⚠️ **注意**: 内置安全认证与 Spring Security 可同时使用，Spring Security 先执行，内置认证作为二次校验。

### 自定义 Handler 完整示例

#### 1. 推送到 Elasticsearch

```java
@Component
@ConditionalOnProperty(name = "operation-log.handler.type", havingValue = "elasticsearch")
public class ElasticsearchLogHandler implements OperationLogHandler {
    
    @Autowired
    private ElasticsearchClient esClient;
    
    @Value("${operation-log.handler.elasticsearch.index:operation-logs}")
    private String indexName;
    
    @Override
    public void handle(LogRecord logRecord) {
        try {
            IndexRequest<LogRecord> request = IndexRequest.of(i -> i
                .index(indexName)
                .document(logRecord)
            );
            esClient.index(request);
        } catch (Exception e) {
            // 降级到控制台输出
            System.err.println("Failed to index log: " + e.getMessage());
            System.out.println(logRecord);
        }
    }
}
```

#### 2. 发送到 Kafka

```java
@Component
@ConditionalOnProperty(name = "operation-log.handler.type", havingValue = "kafka")
public class KafkaLogHandler implements OperationLogHandler {
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("${operation-log.handler.kafka.topic:operation-logs}")
    private String topic;
    
    @Override
    public void handle(LogRecord logRecord) {
        String json = JSON.toJSONString(logRecord.toFlatJson());
        kafkaTemplate.send(topic, logRecord.getUserId().toString(), json)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    System.err.println("Failed to send to Kafka: " + ex.getMessage());
                }
            });
    }
}
```

#### 3. 保存到数据库（异步批量）

```java
@Component
public class DatabaseLogHandler implements OperationLogHandler {
    
    private final List<LogRecord> buffer = new ArrayList<>();
    private static final int BATCH_SIZE = 100;
    
    @Autowired
    private LogRecordRepository repository;
    
    @Override
    public synchronized void handle(LogRecord logRecord) {
        buffer.add(logRecord);
        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }
    
    @Scheduled(fixedRate = 5000) // 每5秒批量写入
    public synchronized void flush() {
        if (buffer.isEmpty()) return;
        
        try {
            repository.saveAll(new ArrayList<>(buffer));
            buffer.clear();
        } catch (Exception e) {
            System.err.println("Failed to save logs: " + e.getMessage());
        }
    }
}
```

#### 4. 多 Handler 组合

```java
@Component
@Primary
public class CompositeLogHandler implements OperationLogHandler {
    
    @Autowired
    private List<OperationLogHandler> handlers;
    
    @Override
    public void handle(LogRecord logRecord) {
        for (OperationLogHandler handler : handlers) {
            if (handler != this) {
                try {
                    handler.handle(logRecord);
                } catch (Exception e) {
                    System.err.println("Handler failed: " + handler.getClass().getSimpleName());
                }
            }
        }
    }
}
```

### 多环境配置示例

#### application-dev.yml (开发环境)

```yaml
operation-log:
  # 开发环境：关闭全局统计，按需开启
  heatmap-global-enabled: false
  profile-global-enabled: false
  
  heatmap:
    enabled: true
    sample-rate: 1.0          # 开发环境全量采样
    fallback-enabled: true
    
  profile:
    enabled: true
    auto-infer-type: true
```

#### application-test.yml (测试环境)

```yaml
operation-log:
  # 测试环境：开启统计用于测试
  heatmap-global-enabled: true
  profile-global-enabled: true
  
  heatmap:
    enabled: true
    sample-rate: 1.0
    realtime-retention-hours: 48   # 测试环境保留48小时
    
  dashboard:
    enabled: true
    refresh-interval: 10           # 测试环境10秒刷新
```

#### application-prod.yml (生产环境)

```yaml
operation-log:
  # 生产环境：按需开启，注意性能
  heatmap-global-enabled: true
  profile-global-enabled: true
  handle-on-fail-global-enabled: true  # 失败时也要记录
  
  heatmap:
    enabled: true
    sample-rate: 0.1                 # 生产环境10%采样
    realtime-retention-hours: 24
    hourly-retention-days: 7
    daily-retention-days: 90
    fallback-enabled: true
    fallback-max-size: 5000
    exclude-operation-types:
      - HEALTH_CHECK
      - PING
      - METRICS
      
  profile:
    enabled: true
    auto-infer-type: true
    default-stats-days: 30
    operation-count-retention-days: 90
    async-queue-size: 1024
    
  dashboard:
    enabled: true
    refresh-interval: 60             # 生产环境60秒刷新
    
  export:
    csv:
      enabled: true
      max-export-rows: 50000         # 生产环境限制导出数量
```

---

## 📚 API 文档

### 核心注解 @OperationLog

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | "未描述的接口" | 操作描述，用于生成操作名称 |
| `type` | String | "DEFAULT" | 操作类型，用于分类统计 |
| `handleOnFail` | boolean | false | 失败时是否记录日志（优先级高于全局配置）|
| `heatmap` | boolean | false | 是否启用热力图统计（优先级低于全局配置）|
| `profile` | boolean | false | 是否启用用户画像（优先级低于全局配置）|

### 全局配置 vs 注解配置优先级

| 功能 | 全局配置 | 注解配置 | 说明 |
|------|----------|----------|------|
| 热力图统计 | `heatmap-global-enabled` | `heatmap = true` | 任一开启即生效 |
| 用户画像 | `profile-global-enabled` | `profile = true` | 任一开启即生效 |
| 失败记录 | `handle-on-fail-global-enabled` | `handleOnFail = true` | 注解优先级更高 |

> 🔥 **最佳实践**: 使用全局配置统一管理，减少重复代码！

### HTTP API

#### 热力图 API

| 接口 | 说明 |
|------|------|
| `GET /operation-log/heatmap/status` | 服务状态检查 |
| `GET /operation-log/heatmap/stats` | 获取所有接口实时统计 |
| `GET /operation-log/heatmap/stats/{className}/{methodName}` | 获取指定接口统计 |
| `GET /operation-log/heatmap/topn` | 获取 TopN 热门接口 |
| `GET /operation-log/heatmap/export/realtime` | 导出实时统计数据(CSV) |
| `GET /operation-log/heatmap/export/topn` | 导出TopN数据(CSV) |

#### 用户画像 API

| 接口 | 说明 |
|------|------|
| `GET /operation-log/profile/status` | 服务状态检查 |
| `GET /operation-log/profile/user/{userId}` | 获取用户完整画像 |
| `GET /operation-log/profile/user/{userId}/tags` | 获取用户标签列表 |
| `GET /operation-log/profile/user/{userId}/stats` | 获取用户操作统计 |
| `GET /operation-log/profile/tag/{tagName}` | 获取标签下用户列表 |
| `GET /operation-log/profile/export/user/{userId}` | 导出用户画像(CSV) |
| `GET /operation-log/profile/export/tag/{tagName}` | 导出标签用户(CSV) |

---

## 🔧 常见问题

### Q: 热力图数据占用多少 Redis 内存？

A: 基于 HyperLogLog 算法，千万级 UV 统计仅需约 **12KB** 内存。

### Q: 如何排除特定接口的热力图统计？

A: 可通过配置排除特定 operation-type：

```yaml
operation-log:
  heatmap:
    exclude-operation-types:
      - HEALTH_CHECK
      - PING
      - METRICS
```

### Q: Redis 故障会影响业务吗？

A: 不会。启用 `fallback-enabled: true` 后，Redis 故障会自动降级，不影响业务功能。

### Q: Dashboard 访问需要认证吗？

A: **Dashboard 已实现完善的访问控制机制**，支持四种认证模式：

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `OFF` | 无认证 | 本地开发 |
| `IP_ONLY` | 仅IP白名单 | 内网环境 |
| `TOKEN_ONLY` | 仅Token认证 | 外网环境 |
| `IP_AND_TOKEN` | 双重认证（推荐生产） | 高安全要求 |

默认配置为 `OFF`（无认证），建议生产环境配置为 `IP_AND_TOKEN`：

```yaml
operation-log:
  dashboard:
    auth-mode: IP_AND_TOKEN
    allow-ips:
      - "127.0.0.1"
      - "192.168.1.0/24"
    auth-token: ${DASHBOARD_TOKEN:}
```

### Q: 配置项 `handle-on-fail-global-enabled` 和 `handle-on-fail-global-enabled` 有什么区别？

A: 代码中实际使用的是 `handle-on-fail-global-enabled`，README 之前版本有误，现已修正。注解中的 `handleOnFail` 对应全局配置的 `handle-on-fail-global-enabled`。

---

## 📄 许可证

[Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

---

<div align="center">

**Made with ❤️ by creekmoon**

</div>
