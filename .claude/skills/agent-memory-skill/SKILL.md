---
name: agent-memory-skill
description: 创建和维护 .agent-memory/ 项目记忆文档，通过三层结构（系统层→模块层→深度层）建立渐进式项目知识库。读模式用于快速了解、接手项目；写模式用于上线后归档稳定实现。当用户说"分析项目"、"建立记忆"、"更新记忆"、"了解项目"、"接手项目"时使用。
---

# Agent Memory Skill

渐进式项目记忆系统。通过三层文档结构让 AI 快速理解项目全貌，按需深入细节。

**核心原则**: `.agent-memory/` 只记录**已上线的事实**，不记录开发中/WIP 内容。

## Quick start

- **接手项目** → 从 `.agent-memory/01-system/00-index.md` 开始阅读
- **建立记忆** → 按 系统层 → 模块层 → 深度层 顺序创建文档
- **更新记忆** → 确认代码已上线后，更新对应层级文档

## Mode detection

根据用户意图判断模式：

| 用户说... | 模式 | 动作 |
|-----------|------|------|
| "帮我了解/接手这个项目" | **读模式** | 阅读已有文档，向用户汇报 |
| "改下XX模块" | **读模式** | 查阅相关模块文档辅助开发 |
| "分析项目，建立记忆文档" | **写模式-初始化** | 扫描项目，创建文档体系 |
| "XX需求已上线，更新记忆" | **写模式-更新** | 更新对应文档 |

## Document structure

```
.agent-memory/
├── 01-system/                    # Layer 1: 全景概览（最稳定）
│   ├── 00-index.md              # 入口索引（总是首先读）
│   ├── 01-context.md            # 项目上下文
│   ├── 02-architecture.md       # 架构概览
│   ├── 03-tech-stack.md         # 技术栈
│   ├── 04-data-model.md         # 核心数据模型
│   └── 05-conventions.md        # 全局约定
│
├── 02-modules/                   # Layer 2: 业务领域划分（按需读）
│   ├── 00-index.md              # 模块清单
│   └── mod-{领域}.md            # 各模块文档
│
└── 03-deep/                      # Layer 3: 实现细节（深入时读）
    ├── 00-index.md              # 深度主题索引
    └── {模块名}/                # 按模块组织的深度文档
        ├── flow-{流程}.md       # 业务流程
        ├── lifecycle-{实体}.md  # 生命周期
        ├── dataflow-{场景}.md   # 数据流
        └── interaction-{协作}.md # 模块交互
```

---

## Instructions: Read mode

### Scenario A: Onboarding (接手项目)

按以下顺序阅读，由浅入深：

1. `01-system/00-index.md` → 系统定位和模块全景
2. `01-system/01-context.md` → 项目背景和边界
3. `01-system/02-architecture.md` → 架构分层
4. `01-system/03-tech-stack.md` → 技术选型
5. `02-modules/00-index.md` → 模块清单
6. 根据任务选择 `mod-{相关模块}.md` 深入

### Scenario B: Task-specific lookup (任务定位)

1. 查阅 `02-modules/mod-{模块}.md` → 找到入口类和关键方法
2. 需要理解实现细节 → `03-deep/{模块名}/` 下的对应文档
3. 基于文档定位代码位置，开始修改

---

## Instructions: Write mode

### Pre-condition (写入前提)

**只有满足以下全部条件才能写入**：
- ✅ 代码已合并到主分支 (main/master)
- ✅ 已发布到生产环境
- ❌ 排除：开发中(WIP)、待评审、未合并、feature branch
- ❌ 排除：实验性代码、临时方案、可能回滚的改动

### Scenario C: Initial setup (首次建立)

**Step 1 — 探索项目**
- 扫描项目结构（pom.xml/package.json、目录树、配置文件）
- 识别技术栈、架构风格、业务领域划分
- 确认分析的是主分支的已发布代码

**Step 2 — 创建系统层** (`01-system/`)
- 使用 `assets/system-*-template.md` 模板
- 从 `00-index.md` 开始，依次填充 01~05
- 约束: 全部控制在 500 行以内，只记录稳定信息

**Step 3 — 创建模块层** (`02-modules/`)
- 按业务领域划分（不按技术分层）
- 使用 `assets/module-template.md` 模板
- 约束: 每篇 ≤ 300 行，用类名/方法名代替代码示例

**Step 4 — 创建深度层** (`03-deep/`) — 按需
- 为每个需要深入的模块创建子目录
- 使用 `assets/deep-*-template.md` 模板
- 约束: 使用 Mermaid 图，禁止贴原始代码

**Step 5 — 验证**
- 检查所有文档间链接可达
- 验证证据锚点（类名/方法名）确实存在于代码中
- 确保三层间有清晰的导航路径

### Scenario D: Post-release update (上线后更新)

**Step 1 — 确认上线状态**
- 代码已合并到主分支且已发布到生产

**Step 2 — 识别变更范围**
- 架构/技术栈变化 → 更新系统层
- 模块能力变化 → 更新对应模块文档
- 数据流/生命周期变化 → 更新深度层

**Step 3 — 执行更新**
- 只修改受影响的文档
- 更新交叉引用和协作点
- 删除已废弃的信息

---

## Layer rules

| 层级 | 行数限制 | 核心内容 | 禁止内容 |
|------|----------|----------|----------|
| 系统层 | 全部 ≤ 500行 | 定位、架构、技术栈、数据模型、约定 | 具体接口列表、字段详情 |
| 模块层 | 每篇 ≤ 300行 | 边界、能力清单(+锚点)、入口类、流程概要 | 长篇代码示例 |
| 深度层 | 按需创建 | 业务流程、生命周期、数据流、模块交互 | 直接贴原始代码 |

## Writing standards

1. **证据锚点**: 每条记录必须有可验证的代码锚点（类名/方法名/路由/表名）
2. **禁止编造**: 不确定的信息不写
3. **面向检索**: 每个文档包含可检索关键词章节
4. **导航链接**: 每个文档底部包含 ↑上级 / →相关 / ↓深入 的导航
5. **模块划分**: 按业务领域划分，不按技术分层

详细格式规范、证据锚点格式、Mermaid 图表规范和正反示例见 [reference.md](reference.md)。

## Templates

模板文件位于本 Skill 的 `assets/` 目录，创建文档时按需使用：

| 层级 | 模板文件 | 生成目标 |
|------|----------|----------|
| 系统层 | `system-*-template.md` (6个) | `01-system/00~05-*.md` |
| 模块层 | `modules-index-template.md` | `02-modules/00-index.md` |
| 模块层 | `module-template.md` | `02-modules/mod-{领域}.md` |
| 深度层 | `deep-index-template.md` | `03-deep/00-index.md` |
| 深度层 | `deep-flow-template.md` | `03-deep/{模块}/flow-{流程}.md` |
| 深度层 | `deep-lifecycle-template.md` | `03-deep/{模块}/lifecycle-{实体}.md` |
| 深度层 | `deep-dataflow-template.md` | `03-deep/{模块}/dataflow-{场景}.md` |
| 深度层 | `deep-interaction-template.md` | `03-deep/{模块}/interaction-{协作}.md` |
