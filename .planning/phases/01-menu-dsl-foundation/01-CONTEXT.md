# Phase 1: Menu DSL Foundation - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

本阶段只负责打稳 AMenu 作为“通用 Minecraft 菜单插件”的 DSL 基座与对外入口，不负责继续扩更多高级能力。范围聚焦在更短的 YAML 写法、统一命令入口、默认示例菜单加载方式，以及把项目从“皮肤插件”认知里彻底纠偏为“通用菜单引擎”。

</domain>

<decisions>
## Implementation Decisions

### Product Positioning
- **D-01:** AMenu 的产品定位锁定为通用 Minecraft 菜单插件，不再定义为皮肤插件。
- **D-02:** 皮肤菜单只作为 bundled example，用来证明 DSL 和交互能力，不得反向定义项目边界。
- **D-03:** 对外主命令统一为 `/amenu`，`/skinmenu` 仅保留为兼容别名。

### DSL Direction
- **D-04:** Phase 1 优先追求“比 TRMenu 更省配置、更直观”的最小 DSL，而不是继续增加新的复杂结构。
- **D-05:** 低配置优先字段已经锁定为 `layout`、`fill`、`buttons`、`head`、`click`、内联 `input`。
- **D-06:** 高阶结构如 `templates`、`prompts` 可以保留，但必须作为“可选增强层”，不能成为简单菜单的必需写法。
- **D-07:** 默认配置示例应优先展示简写路径，而不是展示最完整但更长的复用写法。

### Phase 1 Scope Guardrails
- **D-08:** 本阶段不继续扩分页、异步列表、条件系统、绑定入口等高级能力，这些明确留到后续 phase。
- **D-09:** 本阶段不把 Folia 兼容实现做深，只允许保留平台适配预留位；正式兼容工作属于 Phase 3。
- **D-10:** 本阶段的“现代化”优先体现为配置长度、可读性、入口一致性和示例表达，而不是功能数量。

### Example Strategy
- **D-11:** 默认示例菜单仍可继续使用皮肤菜单场景，因为它对输入流、动作链和跳转链最直观。
- **D-12:** 示例文档必须明确写出“示例不等于插件定位”，防止后续用户或规划再次误判产品边界。

### the agent's Discretion
- DSL 字段的具体命名细节，只要不违背“更短、更直观”的目标
- README 的组织方式、示例顺序和措辞
- 示例菜单里保留哪些演示按钮，只要不重新把产品写成皮肤专用插件

</decisions>

<specifics>
## Specific Ideas

- 用户明确纠正过一次：这个项目不是皮肤插件，而是“我的世界菜单插件，目标是比 TRMenu 更现代、更省配置，更多功能，兼容 Spigot/Folia”。
- 用户明确指出过一次当前实现“能力更强，但默认示例更复杂”，因此 Phase 1 的核心判断标准之一就是默认写法是否足够短。
- 用户对 TRMenu 的参考不是要求兼容原语法，而是要求在常见菜单场景里达到“更低配置成本”和“更现代的结构表达”。

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and requirements
- `.planning/PROJECT.md` - 当前产品边界、核心价值、约束和关键决策
- `.planning/REQUIREMENTS.md` - Phase 1 对应的 MENU-01 / MENU-02 / MENU-03 需求映射
- `.planning/ROADMAP.md` - Phase 1 的目标、边界和后续阶段切分
- `.planning/STATE.md` - 当前项目状态与“先纠偏再规划”的推进背景

### Current product-facing docs
- `README.md` - 当前对外说明、示例写法和用户感知入口
- `src/main/resources/plugin.yml` - 当前主命令、别名和插件描述

### Existing DSL implementation
- `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - 当前菜单、按钮、动作、图标的数据结构
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - 当前 YAML DSL 的解析入口和简写支持实现
- `src/main/resources/menus/main.yml` - 当前 bundled example，能反映默认 DSL 体感

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt`: 已经具备 `fill`、`head`、`click`、内联 `input` 等简写解析能力，Phase 1 不应推翻，只应整理和收敛。
- `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt`: 已有 MenuDefinition / ButtonDefinition / PromptDefinition / MenuAction 等核心模型，可作为 Phase 1 的 DSL 稳定面。
- `src/main/kotlin/cc/keer/amenu/command/AMenuCommand.kt`: 通用主命令入口已经存在，适合作为后续计划的固定入口点。
- `src/main/resources/menus/*.yml`: 已有 bundled examples，可直接用来验证 DSL 是否真的足够短。

### Established Patterns
- 当前代码已经采用“配置层 / GUI 层 / 输入层 / 动作层”分离模式，Phase 1 不应把这些边界重新揉回去。
- 当前示例从复用型 DSL 逐步演化到简写 DSL，说明项目已经有“高阶能力保留、默认写法压短”的既有趋势。
- 当前项目更偏 Maven + Kotlin + Bukkit API 兼容式实现，Phase 1 应延续这个基线，不引入额外构建复杂度。

### Integration Points
- Phase 1 的改动主要会继续落在 `MenuRepository`、`MenuModels`、`README.md`、`plugin.yml` 和 bundled example 菜单文件。
- 后续 planner 应把“用户看到的 DSL 体感”视作一等产物，不只看 Kotlin 解析代码是否能跑。

</code_context>

<deferred>
## Deferred Ideas

- Folia 正式调度兼容与线程安全抽象 - Phase 3
- 分页、异步数据菜单 - Phase 4
- 条件系统、变量系统、更多绑定入口 - Phase 4
- 更丰富的业务示例菜单库 - 可在 Phase 4 之后继续扩展

</deferred>

---

*Phase: 01-menu-dsl-foundation*
*Context gathered: 2026-04-02*
