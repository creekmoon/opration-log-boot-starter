---
description: 系统层入口索引 - 项目整体概览和导航
---

# {项目名称} - 系统总览

> 一句话定位: {项目是什么，解决什么问题}

---

## 快速导航

| 文档 | 内容 | 阅读顺序 |
|------|------|----------|
| [01-context.md](01-context.md) | 项目背景、目标、边界 | **1st** |
| [02-architecture.md](02-architecture.md) | 系统架构、组件关系 | **2nd** |
| [03-tech-stack.md](03-tech-stack.md) | 技术栈、依赖版本 | **3rd** |
| [04-data-model.md](04-data-model.md) | 核心数据实体 | 按需 |
| [05-conventions.md](05-conventions.md) | 全局约定、规范 | 编码前 |

---

## 模块导航

> 跳转到 [模块层索引](../02-modules/00-index.md) 查看业务领域划分。

| 模块 | 职责 | 文档 |
|------|------|------|
| {模块A} | {一句话职责} | [mod-{A}](../02-modules/mod-{A}.md) |
| {模块B} | {一句话职责} | [mod-{B}](../02-modules/mod-{B}.md) |

---

## 项目元信息

| 属性 | 值 |
|------|-----|
| 项目名 | {name} |
| 技术栈 | {Java SpringBoot / Node.js / Python ...} |
| 主要语言 | {Java / TypeScript / Python} |
| 创建时间 | {YYYY-MM} |
| 最后更新 | {YYYY-MM-DD} |

---

## 关键入口

```
src/
├── main/
│   ├── java/com/{company}/{project}/
│   │   ├── Application.java          # 启动类
│   │   ├── config/                   # 全局配置
│   │   ├── common/                   # 公共组件
│   │   └── {domain}/                 # 业务模块
└── test/                             # 测试
```

---

## 外部系统

| 系统 | 类型 | 用途 |
|------|------|------|
| {MySQL} | 数据库 | 主数据存储 |
| {Redis} | 缓存 | 会话/热点数据 |
| {Kafka} | 消息队列 | 事件驱动 |
| {第三方} | API | {用途} |
