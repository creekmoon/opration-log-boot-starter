---
description: 系统层 - 技术栈、依赖版本
---

# 技术栈

## 1. 基础技术

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | {Java/TypeScript/Python/Go} | {version} | 开发语言 |
| 框架 | {SpringBoot/Nest.js/Django/Gin} | {version} | 应用框架 |
| 构建 | {Maven/Gradle/npm/pnpm} | {version} | 构建工具 |

---

## 2. 数据层

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 数据库 | {MySQL/PostgreSQL/MongoDB} | {version} | 主存储 |
| 缓存 | {Redis} | {version} | 缓存 |
| ORM | {MyBatis/JPA/Prisma/GORM} | {version} | 数据访问 |

---

## 3. 中间件

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 消息队列 | {Kafka/RabbitMQ/RocketMQ} | {version} | 异步消息 |
| 搜索引擎 | {Elasticsearch} | {version} | 全文检索 |
| 定时任务 | {XXL-Job/Quartz} | {version} | 任务调度 |

---

## 4. 基础设施

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 容器 | {Docker} | {version} | 容器化 |
| 编排 | {Kubernetes} | {version} | 容器编排 |
| 监控 | {Prometheus/Grafana} | {version} | 监控告警 |

---

## 5. 关键依赖

```xml
<!-- 核心依赖示例 -->
<dependency>
    <groupId>{group}</groupId>
    <artifactId>{artifact}</artifactId>
    <version>{version}</version>
</dependency>
```

---

## 6. 配置文件

| 配置 | 文件路径 | 说明 |
|------|----------|------|
| 应用配置 | `application.yml` | 主配置 |
| 环境配置 | `application-{env}.yml` | 环境差异化 |
| 日志配置 | `logback-spring.xml` | 日志规则 |

---

## 7. 可检索关键词

`{技术名}` / `{版本号}` / `{配置项}` / `{依赖名}`

---

## 8. 导航

- ↑ 上级: [系统总览](00-index.md)
- ← 相关: [架构概览](02-architecture.md)
- → 相关: [数据模型](04-data-model.md)
