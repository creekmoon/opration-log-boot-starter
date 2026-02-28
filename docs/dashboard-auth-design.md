# Dashboard 访问控制方案设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 任务ID | audit-fix-dashboard-auth |
| 文档版本 | 1.0.0 |
| 编写日期 | 2026-03-01 |
| 编写人 | Visionary (产品经理) + CodeSmith (后端工程师) |

---

## 1. 需求分析

### 1.1 问题描述

当前 `DashboardController` 和 `DashboardDataController` 的所有端点均为公开访问，生产环境存在以下安全风险：

- **敏感数据暴露**: 操作日志统计、用户行为数据等敏感信息可被任意访问
- **无访问审计**: 无法追踪谁访问了Dashboard
- **内网渗透风险**: 一旦内网被突破，Dashboard成为攻击入口

### 1.2 安全需求

| 需求 | 优先级 | 说明 |
|------|--------|------|
| IP白名单 | 高 | 只允许特定IP/网段访问 |
| Token认证 | 高 | 通过密钥验证访问者身份 |
| 配置灵活 | 中 | 支持多种安全策略组合 |
| 零侵入 | 高 | 不影响现有Spring Security配置 |

---

## 2. 设计方案

### 2.1 设计原则

1. **轻量级**: 不引入Spring Security等重量级依赖
2. **开箱即用**: 默认提供合理的安全配置
3. **向后兼容**: 默认关闭，不影响现有部署
4. **可配置**: 支持YAML/Properties灵活配置

### 2.2 安全策略组合

```
┌─────────────────────────────────────────────────────────────┐
│                    Dashboard 访问控制                        │
├─────────────────────────────────────────────────────────────┤
│  策略1: IP白名单                                             │
│  ├─ 支持单个IP: 192.168.1.100                               │
│  ├─ 支持CIDR: 192.168.1.0/24                                │
│  └─ 支持localhost/127.0.0.1自动识别                          │
│                                                             │
│  策略2: Token认证                                            │
│  ├─ 请求头: X-Dashboard-Token                               │
│  ├─ Query参数: ?token=xxx                                   │
│  └─ 支持自定义Token生成                                      │
│                                                             │
│  策略组合模式                                                │
│  ├─ OFF: 无认证 (开发环境默认)                               │
│  ├─ IP_ONLY: 仅IP白名单                                     │
│  ├─ TOKEN_ONLY: 仅Token认证                                 │
│  └─ IP_AND_TOKEN: IP白名单 + Token双重认证 (推荐生产环境)      │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 配置项设计

```yaml
operation-log:
  dashboard:
    # 是否启用Dashboard (原有配置)
    enabled: true
    
    # ========== 新增安全配置 ==========
    
    # 访问控制模式: OFF | IP_ONLY | TOKEN_ONLY | IP_AND_TOKEN
    auth-mode: OFF
    
    # IP白名单配置 (支持CIDR格式)
    allow-ips:
      - "127.0.0.1"
      - "192.168.1.0/24"
      - "10.0.0.0/8"
    
    # Token认证配置
    auth-token: "${DASHBOARD_TOKEN:}"  # 生产环境建议从环境变量读取
    
    # Token请求头名称 (自定义)
    token-header: "X-Dashboard-Token"
    
    # 是否允许Query参数传递Token (默认false,更安全)
    allow-token-in-query: false
```

### 2.4 类设计

```
┌─────────────────────────────────────────────────────────────────┐
│  DashboardSecurityFilter (OncePerRequestFilter)                 │
│  ├─ 拦截路径: /operation-log/**, /operation-log-dashboard.html │
│  ├─ 跳过路径: /operation-log/dashboard/auth/* (认证端点)        │
│  └─ 职责: 根据配置执行IP/Token校验                               │
├─────────────────────────────────────────────────────────────────┤
│  DashboardSecurityService                                       │
│  ├─ checkIpAllowed(String ip): boolean                         │
│  ├─ checkTokenValid(String token): boolean                     │
│  └─ getClientIp(HttpServletRequest): String                    │
├─────────────────────────────────────────────────────────────────┤
│  DashboardProperties (扩展)                                     │
│  ├─ authMode: AuthMode                                         │
│  ├─ allowIps: List<String>                                     │
│  ├─ authToken: String                                          │
│  ├─ tokenHeader: String                                        │
│  └─ allowTokenInQuery: boolean                                 │
├─────────────────────────────────────────────────────────────────┤
│  DashboardAuthController (新增)                                 │
│  ├─ GET /operation-log/dashboard/auth/status                   │
│  │   └─ 返回当前安全配置状态 (不脱敏)                          │
│  └─ POST /operation-log/dashboard/auth/verify                  │
│      └─ 验证Token有效性 (用于前端预检)                         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 与Spring Security兼容性

### 3.1 无Spring Security环境

DashboardSecurityFilter 作为标准Servlet Filter工作，无需任何额外配置。

### 3.2 存在Spring Security环境

```java
// 用户可以在Spring Security配置中排除Dashboard路径
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // 允许Dashboard路径，由Dashboard自己的过滤器处理
            .requestMatchers("/operation-log/**").permitAll()
            .anyRequest().authenticated()
        );
    return http.build();
}
```

### 3.3 Filter顺序

```
┌─────────────────────────────────────────┐
│  Filter Chain                           │
├─────────────────────────────────────────┤
│  1. Security Filter (Spring)            │
│  2. DashboardSecurityFilter             │
│     ├─ 配置模式检查                      │
│     ├─ IP白名单校验                      │
│     ├─ Token校验                         │
│     └─ 失败返回401                       │
│  3. DashboardController                 │
└─────────────────────────────────────────┘
```

---

## 4. 实现细节

### 4.1 IP白名单匹配逻辑

```java
// 支持格式:
// - 192.168.1.1      (精确匹配)
// - 192.168.1.0/24   (CIDR匹配)
// - localhost        (自动解析为127.0.0.1)

private boolean matchIp(String clientIp, String allowedIp) {
    if (allowedIp.contains("/")) {
        // CIDR匹配
        return IpUtils.isInRange(clientIp, allowedIp);
    }
    return clientIp.equals(allowedIp);
}
```

### 4.2 Token校验逻辑

```java
// 优先级: Header > Query Parameter
// 安全建议: 生产环境禁用Query Parameter

private String extractToken(HttpServletRequest request) {
    // 1. 从Header获取
    String token = request.getHeader(tokenHeader);
    if (token != null && !token.isEmpty()) {
        return token;
    }
    
    // 2. 从Query Parameter获取 (如果启用)
    if (allowTokenInQuery) {
        return request.getParameter("token");
    }
    
    return null;
}
```

### 4.3 错误响应格式

```json
// 401 Unauthorized
{
  "code": 401,
  "message": "Dashboard access denied",
  "details": {
    "mode": "IP_AND_TOKEN",
    "reason": "IP_NOT_ALLOWED",
    "clientIp": "10.0.0.5"
  }
}
```

---

## 5. 使用指南

### 5.1 开发环境 (无认证)

```yaml
operation-log:
  dashboard:
    enabled: true
    auth-mode: OFF
```

### 5.2 测试环境 (IP白名单)

```yaml
operation-log:
  dashboard:
    enabled: true
    auth-mode: IP_ONLY
    allow-ips:
      - "127.0.0.1"
      - "192.168.1.0/24"
```

### 5.3 生产环境 (双重认证)

```yaml
operation-log:
  dashboard:
    enabled: true
    auth-mode: IP_AND_TOKEN
    allow-ips:
      - "10.0.0.0/8"  # 仅允许内网访问
    auth-token: "${DASHBOARD_TOKEN:change-me-in-production}"
```

### 5.4 前端适配

```javascript
// 在请求Dashboard API时携带Token
fetch('/operation-log/dashboard/api/overview', {
  headers: {
    'X-Dashboard-Token': 'your-secret-token'
  }
})
```

---

## 6. 安全建议

### 6.1 Token管理

1. **长度**: 建议使用32位以上随机字符串
2. **轮换**: 定期更换Token，支持平滑过渡
3. **存储**: 生产环境通过环境变量注入，禁止硬编码
4. **传输**: 强制HTTPS，防止中间人攻击

### 6.2 IP白名单

1. **最小权限**: 仅开放必要的IP段
2. **内网优先**: 生产环境建议仅允许内网IP
3. **VPN集成**: 如需外网访问，通过VPN接入内网

### 6.3 监控与审计

建议后续版本增加：
- 访问日志记录
- 失败尝试次数限制
- 异常访问告警

---

## 7. 向后兼容性

| 场景 | 处理方案 |
|------|----------|
| 未配置安全配置 | 默认 `auth-mode: OFF`，完全向后兼容 |
| 已存在Spring Security | DashboardSecurityFilter独立工作，互不干扰 |
| 升级用户 | 无需修改配置，默认无认证模式 |

---

## 8. 任务清单

- [x] 需求分析与方案设计
- [x] 编写设计文档
- [x] 扩展 DashboardProperties 配置类
- [x] 实现 DashboardSecurityService 校验逻辑
- [x] 实现 DashboardSecurityFilter 过滤器
- [x] 创建 DashboardAuthController 认证端点
- [x] 更新 OperationLogAutoConfiguration 注册Filter
- [x] 编写单元测试
- [x] 提交并推送代码

---

## 9. 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| 1.0.0 | 2026-03-01 | 初始版本 | Visionary + CodeSmith |
