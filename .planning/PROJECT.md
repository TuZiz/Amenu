# AMenu

## What This Is

AMenu 是一个面向 Spigot、Paper，并预留 Folia 兼容路线的通用 Minecraft 菜单插件。它的目标不是做“皮肤插件”，而是做一个比 TRMenu 更现代、更省配置、更容易扩展的菜单引擎。皮肤菜单只是 bundled example。

## Core Value

让服主用尽量短、尽量直观的配置，稳定搭出可扩展的 Minecraft 菜单系统。

## Requirements

### Validated

- [x] MENU-01 / MENU-02 / MENU-03 validated in Phase 1: canonical DSL, `/amenu` command surface, and bundled example loading are covered by code and tests

### Active

- [ ] 完成运行时动作链、输入流、返回栈、刷新与权限反馈的完整交互层
- [ ] 维持 bundled example 与产品定位解耦，避免示例反向定义插件边界
- [ ] 推进 Spigot/Paper/Folia 兼容策略，但将正式调度抽象延后到兼容阶段

### Out of Scope

- 只做某一个业务主题的专用插件
- Web 可视化编辑器
- 与固定业务插件做强耦合首发方案

## Current State

Phase 1 complete. 当前仓库已经具备：

- 简化后的默认 DSL 示例
- 统一 `/amenu` 主命令和 `/skinmenu` 兼容别名
- README / plugin.yml / pom.xml 的菜单引擎定位对齐
- MockBukkit 回归测试，用于 DSL 与命令层验证

## Constraints

- **Tech stack**: Kotlin + Maven + Paper API
- **Build runtime**: Java 21 required for validation/build in this workspace
- **Architecture**: 保持配置层、菜单渲染层、动作执行层、输入层、平台兼容层分离
- **Scope discipline**: 分页、条件系统、Folia 正式实现均不得混入 Phase 1/2 的基础目标

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| AMenu 定位为通用菜单引擎 | 用户已明确纠偏，示例不能定义产品边界 | Validated |
| 默认 DSL 优先 `layout / fill / buttons / head / click / inline input` | 直接回应“更省配置”的核心目标 | Validated |
| `/amenu` 作为唯一主命令，`skinmenu` 仅兼容保留 | 对外入口必须体现通用菜单插件定位 | Validated |
| MockBukkit 作为 Phase 1 的自动化验证基础 | 为后续 Phase 2/3 提供可复用测试底座 | Validated |

---
*Last updated: 2026-04-02 after Phase 1 completion*
