# Phase 2: Runtime Interaction Layer - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

本阶段只负责把 AMenu 的运行时交互层收口为稳定、可验证、可继续扩展的菜单执行能力。重点是动作链执行、聊天输入流程、返回栈/刷新、权限拒绝反馈，以及这些行为在 bundled examples 中的展示方式。

本阶段不再扩展 Phase 1 的最小 DSL 合同，也不引入分页、条件系统、异步数据菜单或正式 Folia 调度抽象。
</domain>

<decisions>
## Implementation Decisions

### Runtime Scope
- **D-01:** Phase 2 必须覆盖 `RUN-01`、`RUN-02`、`RUN-03`、`INP-01`、`INP-02`、`INP-03`、`INP-04`，并以当前 `MenuService` / `ChatInputService` 为主实现入口继续收敛。
- **D-02:** 动作链的核心交互面锁定为 `close`、`open`、`back`、`refresh`、`prompt`、`player`、`console`、`message`、`sound`，本阶段优先保证这些动作的顺序、反馈和会话衔接是稳定的，而不是增加新的动作种类。
- **D-03:** 本阶段的“现代交互”定义优先体现在玩家体验上：点击后的链式反馈、输入会话替换、取消/超时处理、返回来源菜单、权限拒绝时的明确反馈。

### Input Flow
- **D-04:** 输入流程要同时稳定支持可复用 `prompts` 和按钮内联 `input`，但本阶段讨论重点不是语法写法，而是运行时生命周期。
- **D-05:** 输入提交后的 `{input}` 注入、取消关键字、会话替换提示、超时清理都必须视为一等运行时行为，而不是“顺带支持”的边缘能力。
- **D-06:** 聊天输入完成后的后续动作必须回到 Bukkit/Paper 安全线程边界内执行，不把异步聊天事件直接当成可任意操作 GUI/命令的环境。

### Navigation And Permission Feedback
- **D-07:** `back`、`refresh`、`permission`、`visible-permission`、`deny-actions` 这些已经存在的能力要在本阶段被正式验证和展示，而不是只停留在“代码里支持”。
- **D-08:** 权限拒绝反馈优先采用按钮级 `deny-actions`，只有未声明拒绝动作时才退回系统默认 `no-permission` 提示。
- **D-09:** 返回栈要继续以“菜单来源历史”为核心语义，避免在本阶段把它扩展成复杂的上下文栈或多层状态机。

### Example Strategy
- **D-10:** `main.yml` 保持 Phase 1 的最短 canonical DSL，不再塞回高级交互演示。
- **D-11:** 运行时交互能力的展示优先放在 `history.yml`、`admin.yml` 或新的运行时示例菜单中完成，让“默认写法最短”和“运行时能力完整”同时成立。

### Product Positioning
- **D-12:** 本阶段继续维持产品定位：AMenu 是通用 Minecraft 菜单插件，皮肤菜单只是 bundled examples；即便示例仍使用皮肤场景，也不能让实现或文档回退成“皮肤插件路线”。

### the agent's Discretion
- 运行时示例菜单是扩展 `history/admin` 还是新增独立示例，只要不污染 `main.yml` 的最短路径
- 动作链和输入流的测试拆分方式，只要最后能稳定覆盖需求并沿用 MockBukkit 基线
- README 是否在 Phase 2 增加单独“运行时交互”小节，由 planner 自行决定
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and requirements
- `.planning/PROJECT.md` - 当前产品定位、已验证需求与阶段约束
- `.planning/REQUIREMENTS.md` - Phase 2 对应的 RUN / INP requirement 定义
- `.planning/ROADMAP.md` - Phase 2 目标、依赖关系与计划切片
- `.planning/STATE.md` - 当前项目位置已经推进到 Phase 1 complete

### Prior phase outputs
- `.planning/phases/01-menu-dsl-foundation/01-CONTEXT.md` - Phase 1 锁定的最小 DSL 合同
- `.planning/phases/01-menu-dsl-foundation/01-01-SUMMARY.md` - Java 21 / MockBukkit / canonical DSL 收口结果
- `.planning/phases/01-menu-dsl-foundation/01-02-SUMMARY.md` - /amenu 入口和文档对齐结果
- `.planning/phases/01-menu-dsl-foundation/01-VERIFICATION.md` - Phase 1 已通过验证，说明哪些内容已完成、哪些不能重复计入

### Existing runtime implementation
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - 动作链执行、返回栈、权限反馈、菜单打开逻辑
- `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - 输入会话生命周期、取消/超时/提交处理
- `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - ButtonDefinition / PromptDefinition / MenuAction 运行时模型
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - 运行时动作/输入定义如何从 YAML 落入模型
- `src/main/resources/config.yml` - 系统消息、全局取消关键字、输入超时配置

### Bundled examples
- `src/main/resources/menus/main.yml` - 继续保持最短 canonical DSL
- `src/main/resources/menus/history.yml` - 适合继续承接 back/open 等运行时交互展示
- `src/main/resources/menus/admin.yml` - 适合继续承接权限与拒绝反馈展示

### Existing tests
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - 当前 DSL 回归基线
- `src/test/kotlin/cc/keer/amenu/command/AMenuCommandTest.kt` - 当前命令打开菜单基线
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MenuService` 已经具备 `back`、`refresh`、`open`、`permission`、`deny-actions` 等运行时执行骨架，Phase 2 更适合补验证和边界，而不是重写。
- `ChatInputService` 已经具备会话替换、超时清理、取消关键字与提交动作执行的主流程，可以直接作为输入层的稳定化入口。
- `history.yml` 和 `admin.yml` 已经存在，适合扩成运行时能力展示用例，而不用污染 `main.yml`。

### Established Patterns
- 当前项目保持“配置解析层 / 运行时执行层 / 输入层 / GUI 监听层”分离，Phase 2 不应把这些边界重新揉在一起。
- Phase 1 已建立 MockBukkit 回归基线，Phase 2 应沿用这套方式验证运行时交互，而不是退回纯人工测试。
- 主命令、README、默认示例已经在 Phase 1 收口；Phase 2 只应在此基础上扩展示例和测试，不应再次改动产品定位。

### Integration Points
- `MenuListener` 是点击进入 `MenuService.handleClick` 的入口，适合 Phase 2 继续覆盖动作链行为。
- `ChatInputService.onAsyncChat` 与 `MenuService.executeActions` 的衔接是输入流程验证的关键连接点。
- `config.yml` 的 `chat-input` 和 `messages` 是输入取消、超时、权限拒绝反馈的可配置来源。
</code_context>

<specifics>
## Specific Ideas

- Phase 2 最值得锁定的是“运行时体验合同”，而不是再发明配置语法。
- 默认 `main.yml` 已经够短，后续想展示更多功能，应该放到二级示例菜单或专门 runtime example。
- MockBukkit 已经跑通插件加载、命令打开菜单和 DSL 解析，下一步应继续用于点击、权限、输入、超时等运行时回归。
</specifics>

<deferred>
## Deferred Ideas

- 分页、异步列表与公共资源库展示 - Phase 4
- 条件系统、变量系统、更多绑定入口 - Phase 4
- 正式 Folia 调度抽象与线程边界实现 - Phase 3
- 更复杂的上下文导航栈或菜单状态机 - 后续如有必要再拆分

</deferred>

---

*Phase: 02-runtime-interaction-layer*
*Context gathered: 2026-04-02*
