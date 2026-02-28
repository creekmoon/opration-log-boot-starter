# 可视化HTML技术预研报告

## 预研目标
评估在 operation-log-starter 中直接嵌入可视化HTML的可行性

## 时间
2026-02-28 11:35 AM (Asia/Shanghai)

---

## 方案对比

### 方案A: 纯后端数据 + 前端独立部署

**实现方式**:
- Starter 只提供数据API (已有CSV导出接口可复用)
- 前端单独部署（用户自行实现或使用官方示例项目）

**优点**:
- 零包体积增加
- 无额外依赖
- 前端技术栈灵活
- 维护成本低

**缺点**:
- 需要额外部署前端
- 用户体验不连贯

**适用场景**: 企业级应用，已有前端团队

---

### 方案B: Starter嵌入轻量HTML+JS

**实现方式**:
- 在 Starter 中嵌入静态HTML页面
- 使用 CDN 加载 Chart.js (~60KB) 或 ECharts (~300KB)
- 通过 `/operation-log/dashboard` 访问

**技术实现**:
```java
@Controller
public class DashboardController {
    
    @GetMapping("/operation-log/dashboard")
    public String dashboard() {
        return "forward:/static/operation-log-dashboard.html";
    }
}
```

**HTML页面**:
- 纯静态HTML+JS
- 通过AJAX调用后端API获取数据
- 使用 Chart.js 绘制简单图表

**包体积**:
- HTML+JS: ~5KB (内联)
- 或 CDN 引用: 0KB

**优点**:
- 开箱即用，无需额外部署
- 包体积小 (~5KB)
- 用户体验连贯
- 维护成本低（纯静态）

**缺点**:
- 功能受限（只能做简单图表）
- 依赖CDN（或内联JS增加包体积）

**适用场景**: 快速查看、简单监控

---

### 方案C: 完整管理后台

**实现方式**:
- 内嵌 Vue/React 前端构建产物
- 完整的用户界面和交互
- 支持复杂的数据分析和可视化

**包体积**:
- 前端构建产物: ~500KB+
- 需要引入前端构建流程

**优点**:
- 功能强大
- 用户体验好

**缺点**:
- 包体积大
- 引入前端构建流程，复杂度高
- 维护成本高
- 与Starter轻量级理念冲突

**适用场景**: 独立产品，非Starter

---

## 代价评估

| 维度 | 方案A | 方案B | 方案C |
|------|-------|-------|-------|
| 包体积 | 0KB | ~5KB | ~500KB+ |
| 依赖复杂度 | 低 | 低 | 高 |
| 维护成本 | 低 | 低 | 高 |
| 开发成本 | 低 | 中 | 高 |
| 用户体验 | 中 | 高 | 高 |
| 灵活性 | 高 | 中 | 低 |

---

## 推荐方案

**推荐: 方案B (Starter嵌入轻量HTML+JS)**

**理由**:
1. 符合 Starter 轻量级设计理念
2. 包体积增加可忽略 (~5KB)
3. 开箱即用，用户体验好
4. 维护成本低（纯静态HTML）
5. 功能足够满足基本监控需求

**实现建议**:
1. 创建 `DashboardController` 提供 `/operation-log/dashboard` 入口
2. 将 HTML/JS 放在 `src/main/resources/static/`
3. 使用 Chart.js (CDN) 绘制图表
4. 通过 AJAX 调用现有导出API获取数据
5. 可选配置 `operation-log.dashboard.enabled=true/false`

**页面功能**:
- 热力图 PV/UV 实时图表
- TopN 接口排行表格
- 用户画像标签分布饼图
- 简单的时间范围筛选

---

## 下一步行动

如需实施方案B:
1. 创建 `DashboardController`
2. 设计 HTML 页面结构
3. 实现图表渲染逻辑
4. 添加配置开关
5. 预计开发时间: 2-3小时

---

*预研完成时间: 2026-02-28 12:35 PM (预估)*
