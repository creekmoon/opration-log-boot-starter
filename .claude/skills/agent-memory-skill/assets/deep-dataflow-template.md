---
description: 深度层 - {模块} 数据流详解
---

# {模块} 数据流

## 1. 概述

{数据流的整体描述，数据从哪里产生，经过哪里，最终到哪里}

---

## 2. 完整数据流图

```mermaid
flowchart LR
    subgraph Input["输入"]
        I1[HTTP请求]
        I2[消息消费]
        I3[定时任务]
    end

    subgraph Process["处理"]
        P1[参数校验]
        P2[业务处理]
        P3[状态更新]
    end

    subgraph Output["输出"]
        O1[HTTP响应]
        O2[消息发送]
        O3[数据写入]
    end

    I1 --> P1
    I2 --> P2
    I3 --> P2
    P1 --> P2
    P2 --> P3
    P3 --> O1
    P3 --> O2
    P3 --> O3
```

---

## 3. 详细数据流

### 3.1 {场景A} 数据流

**触发条件**: {描述}

```mermaid
sequenceDiagram
    participant S as Source
    participant C as {Controller}
    participant V as {Validator}
    participant Svc as {Service}
    participant Repo as {Repository}
    participant E as {EventBus}

    S->>C: {请求数据}
    C->>V: validate()
    V-->>C: 校验结果
    C->>Svc: process()
    Svc->>Repo: save()
    Repo-->>Svc: Entity
    Svc->>E: publish()
    Svc-->>C: Result
    C-->>S: 响应
```

**数据转换**:

| 阶段 | 数据类型 | 转换 | 代码位置 |
|------|----------|------|----------|
| 输入 | RequestDTO | - | `{Controller}` |
| 校验 | RequestDTO | 字段校验 | `{Validator}` |
| 处理 | Entity | DTO→Entity | `{Service}` |
| 存储 | Entity | 持久化 | `{Repository}` |
| 输出 | ResponseDTO | Entity→DTO | `{Service}` |

---

## 4. 数据存储映射

| 数据对象 | 存储类型 | 存储位置 | 键/表 |
|----------|----------|----------|-------|
| {EntityA} | MySQL | 主库 | `{t_entity_a}` |
| {CacheData} | Redis | 集群 | `{cache:key}` |
| {Event} | Kafka | 集群 | `{topic}` |

---

## 5. 可检索关键词

`{数据对象}` / `{表名}` / `{Topic}` / `{CacheKey}` / `{转换类}`

---

## 6. 导航

- ↑ 上级: [深度主题索引](00-index.md)
- ← 相关: [{模块}](../02-modules/mod-{模块}.md)
