# CSV 导出功能开发执行计划

## 目标
为操作热力图和用户行为画像模块添加 CSV 导出功能

## 排期
- **Phase 0**: 技术预研 - 可视化HTML集成可行性 (2h) - 09:47 ~ 11:47
- **Phase 1**: 基础框架 (2h) - 11:47 ~ 13:47
- **Phase 2**: 热力图导出 (3h) - 13:47 ~ 16:47
- **Phase 3**: 画像导出 (3h) - 16:47 ~ 19:47
- **Phase 4**: 测试与文档 (2h) - 19:47 ~ 21:47
- **Phase 5**: 集成与推送 (1h) - 21:47 ~ 22:47

**总计**: 约 13 小时

## 任务清单

### Phase 0: 技术预研 - 可视化HTML集成
- [x] 调研嵌入式HTML方案（Spring Boot + 静态资源）
- [x] 调研图表库选型（ECharts / Chart.js / 原生Canvas）
- [x] 评估代价：包体积增加、依赖复杂度、维护成本
- [x] 输出预研结论文档

**预研结论**: 推荐方案B (Starter嵌入轻量HTML+JS)
- 包体积: ~5KB
- 使用 Chart.js (CDN)
- 维护成本低
- 预计开发时间: 2-3小时

### Phase 1: 基础框架
- [x] 创建 `CsvExportService` 接口
- [x] 创建 `CsvExportServiceImpl` 实现
- [x] 创建 `CsvExportProperties` 配置类
- [x] 添加 Apache Commons CSV 依赖

### Phase 2: 热力图 CSV 导出
- [x] `HeatmapService` 新增导出方法
- [x] `HeatmapExportController` 新增导出端点
- [ ] 单元测试

### Phase 3: 用户画像 CSV 导出
- [x] `ProfileService` 新增导出方法
- [x] `ProfileExportController` 新增导出端点
- [ ] 单元测试

### Phase 4: 测试与文档
- [x] `CsvExportServiceImplTest`
- [x] 更新 LLM 模块文档
- [ ] 更新 README.md

### Phase 5: 集成与推送
- [x] 代码审查
- [x] Git 提交
- [x] 推送到 `kimiclaw` 分支

## 设计变更记录

### 2026-02-28 09:46 - 移除自定义标签规则功能
**原因**: 保持项目精简，避免过度设计

**影响范围**:
- `ProfileProperties` 中的 `tagRules` 配置项移除
- `TagRule`、`TagCondition` 类保留但简化（仅支持内置4种标签）
- 标签规则改为硬编码，不可配置

**替代方案**:
- 如需扩展标签，通过继承 `ProfileService` 或实现自定义 `TagEvaluator` 接口
- 后续可考虑 SPI 扩展机制，让使用方注入自定义标签计算器

### 2026-02-28 09:47 - 新增可视化HTML技术预研
**目标**: 评估在Starter中直接嵌入可视化HTML的可行性

**关键问题**:
1. 是否增加包体积？（ECharts min ~300KB，Chart.js ~60KB）
2. 是否引入前端构建流程？（否，直接用CDN或内联）
3. 维护成本？（低，纯静态HTML+JS）
4. 与Actuator的集成方式？（新增端点 `/actuator/operation-log-dashboard`）

**预期输出**:
- 技术方案对比文档
- 推荐方案及理由
- 如可行，提供PoC代码

### 2026-02-28 09:47 - 移除Actuator支持
**原因**: 保持项目精简，避免强依赖Spring Boot Actuator

**影响范围**:
- 移除 `HeatmapActuatorEndpoint`、`ProfileActuatorEndpoint`
- 改为独立的Controller或纯Service API
- 移除 `spring-boot-starter-actuator` 依赖

**替代方案**:
- 使用方自行实现Controller调用Service
- 或提供可选的 `@EnableOperationLogDashboard` 注解导入独立配置

## 开始时间
2026-02-28 09:45 AM (Asia/Shanghai)
