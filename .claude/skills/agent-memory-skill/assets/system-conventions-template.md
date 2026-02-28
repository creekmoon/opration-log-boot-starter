---
description: 系统层 - 全局约定、编码规范
---

# 全局约定

## 1. 命名约定

| 类型 | 规则 | 示例 |
|------|------|------|
| 类名 | PascalCase | `OrderService`, `UserController` |
| 方法名 | camelCase | `createOrder()`, `getUserById()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 数据库表 | 小写下划线 | `t_order`, `t_user_profile` |
| 数据库字段 | 小写下划线 | `created_at`, `user_id` |
| API路径 | 小写中划线 | `/api/v1/orders/{id}` |

---

## 2. 包结构约定

```
com.{company}.{project}
├── common/                      # 公共组件
│   ├── config/                   # 全局配置
│   ├── constant/                 # 常量
│   ├── exception/                # 异常
│   ├── util/                     # 工具类
│   └── ...
├── {domainA}/                   # 领域A
│   ├── controller/               # 控制器
│   ├── service/                  # 服务层
│   ├── domain/                   # 领域模型
│   ├── repository/               # 仓储接口
│   ├── infrastructure/          # 基础设施实现
│   └── ...
└── {domainB}/                   # 领域B
    └── ...
```

---

## 3. 异常处理约定

| 场景 | 异常类型 | HTTP状态码 | 错误码 |
|------|----------|------------|--------|
| 参数错误 | `IllegalArgumentException` | 400 | 1000 |
| 未找到 | `NotFoundException` | 404 | 1001 |
| 权限不足 | `UnauthorizedException` | 403 | 1002 |
| 系统错误 | `SystemException` | 500 | 5000 |

---

## 4. 响应格式约定

```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

---

## 5. 代码规范

### 5.1 必须遵守

- {规范1}
- {规范2}

### 5.2 推荐做法

- {推荐1}
- {推荐2}

---

## 6. 可检索关键词

`{命名约定}` / `{包名}` / `{异常名}` / `{规范名}`

---

## 7. 导航

- ↑ 上级: [系统总览](00-index.md)
- ← 相关: [数据模型](04-data-model.md)
- ↓ 深入: [模块层索引](../02-modules/00-index.md)
