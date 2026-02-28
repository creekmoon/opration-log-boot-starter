#!/usr/bin/env python3
"""
初始化 Agent Memory 文档结构

使用方法:
    python init-memory.py [项目根目录]

如果不指定目录，则在当前目录创建 .agent-memory/ 结构
"""

import os
import sys
import shutil
from pathlib import Path

# 模板文件映射
TEMPLATES = {
    # 系统层
    ".agent-memory/01-system/00-index.md": "assets/system-index-template.md",
    ".agent-memory/01-system/01-context.md": "assets/system-context-template.md",
    ".agent-memory/01-system/02-architecture.md": "assets/system-architecture-template.md",
    ".agent-memory/01-system/03-tech-stack.md": "assets/system-tech-stack-template.md",
    ".agent-memory/01-system/04-data-model.md": "assets/system-data-model-template.md",
    ".agent-memory/01-system/05-conventions.md": "assets/system-conventions-template.md",

    # 模块层
    ".agent-memory/02-modules/00-index.md": "assets/modules-index-template.md",

    # 深度层
    ".agent-memory/03-deep/00-index.md": "assets/deep-index-template.md",
}


def get_skill_dir():
    """获取 skill 目录路径"""
    script_dir = Path(__file__).parent.parent
    return script_dir


def copy_template(skill_dir, target_dir, target_file, template_file):
    """复制模板文件到目标位置"""
    template_path = skill_dir / template_file
    target_path = target_dir / target_file

    if not template_path.exists():
        print(f"❌ 模板文件不存在: {template_path}")
        return False

    # 创建目标目录
    target_path.parent.mkdir(parents=True, exist_ok=True)

    # 复制文件
    shutil.copy2(template_path, target_path)
    print(f"✅ 创建: {target_path}")
    return True


def init_memory(project_dir=None):
    """初始化记忆文档结构"""

    # 确定目标目录
    if project_dir:
        target_dir = Path(project_dir).resolve()
    else:
        target_dir = Path.cwd()

    if not target_dir.exists():
        print(f"❌ 目标目录不存在: {target_dir}")
        sys.exit(1)

    print(f"🚀 初始化 Agent Memory 结构到: {target_dir}")
    print("-" * 50)

    skill_dir = get_skill_dir()

    # 创建 README
    readme_content = """# Agent Memory

本项目使用 [agent-memory-skill](../agent-memory-skill/) 管理项目记忆文档。

## 结构说明

```
.agent-memory/
├── 01-system/          # 系统层 - 项目整体概览
│   ├── 00-index.md    # 入口索引
│   ├── 01-context.md  # 项目上下文
│   ├── 02-architecture.md  # 架构概览
│   ├── 03-tech-stack.md    # 技术栈
│   ├── 04-data-model.md    # 核心数据模型
│   └── 05-conventions.md   # 全局约定
│
├── 02-modules/         # 模块层 - 业务领域
│   ├── 00-index.md    # 模块清单
│   └── mod-*.md       # 各模块文档
│
└── 03-deep/            # 深度层 - 实现细节
    ├── 00-index.md    # 深度主题索引
    ├── dataflow-*.md  # 数据流
    ├── lifecycle-*.md # 生命周期
    └── interaction-*.md  # 模块交互
```

## 使用流程

1. **首次分析**: 运行本 skill，从系统层开始建立文档
2. **日常维护**: 代码变更后同步更新对应层级的文档
3. **接手项目**: 从 `01-system/00-index.md` 开始阅读

## 阅读顺序

```
01-system/00-index.md
→ 01-system/01-context.md
→ 01-system/02-architecture.md
→ 01-system/03-tech-stack.md
→ 02-modules/00-index.md
→ (按需) 03-deep/*.md
```
"""

    readme_path = target_dir / ".agent-memory" / "README.md"
    readme_path.parent.mkdir(parents=True, exist_ok=True)
    readme_path.write_text(readme_content, encoding="utf-8")
    print(f"✅ 创建: {readme_path}")

    # 复制所有模板
    success_count = 0
    for target_file, template_file in TEMPLATES.items():
        if copy_template(skill_dir, target_dir, target_file, template_file):
            success_count += 1

    # 创建 .gitignore
    gitignore_content = """# Agent Memory
# 这个目录是项目记忆文档，建议提交到版本控制
# 如果不希望提交，取消下面这行的注释
# *.md
"""
    gitignore_path = target_dir / ".agent-memory" / ".gitignore"
    gitignore_path.write_text(gitignore_content, encoding="utf-8")
    print(f"✅ 创建: {gitignore_path}")

    print("-" * 50)
    print(f"✨ 初始化完成! 共创建 {success_count + 2} 个文件")
    print()
    print("📖 接下来:")
    print("  1. 编辑 .agent-memory/01-system/ 下的文档")
    print("  2. 根据业务领域创建 02-modules/mod-*.md")
    print("  3. 需要时创建 03-deep/ 下的深度文档")
    print()
    print(f"📂 入口: {target_dir / '.agent-memory' / '01-system' / '00-index.md'}")


def main():
    if len(sys.argv) > 1:
        if sys.argv[1] in ("-h", "--help"):
            print(__doc__)
            sys.exit(0)
        init_memory(sys.argv[1])
    else:
        init_memory()


if __name__ == "__main__":
    main()
