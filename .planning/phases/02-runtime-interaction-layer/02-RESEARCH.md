# Phase 2: Runtime Interaction Layer - Research

**Researched:** 2026-04-02
**Domain:** Paper/Kotlin Minecraft menu runtime interaction layer
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

### Claude's Discretion
- 运行时示例菜单是扩展 `history/admin` 还是新增独立示例，只要不污染 `main.yml` 的最短路径
- 动作链和输入流的测试拆分方式，只要最后能稳定覆盖需求并沿用 MockBukkit 基线
- README 是否在 Phase 2 增加单独“运行时交互”小节，由 planner 自行决定

### Deferred Ideas (OUT OF SCOPE)
- 分页、异步列表与公共资源库展示 - Phase 4
- 条件系统、变量系统、更多绑定入口 - Phase 4
- 正式 Folia 调度抽象与线程边界实现 - Phase 3
- 更复杂的上下文导航栈或菜单状态机 - 后续如有必要再拆分
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RUN-01 | 按钮支持关闭菜单、返回上一级、刷新当前菜单、打开其他菜单 | `MenuService` 已实现对应动作；Phase 2 重点是点击路径回归测试、返回栈语义验证、示例菜单展示 |
| RUN-02 | 按钮支持玩家命令、控制台命令、消息、声音等动作链 | `MenuAction`/`MenuService` 已实现；Phase 2 需要验证顺序、副作用和 MockBukkit 可观测性 |
| RUN-03 | 动作链按声明顺序执行，并支持权限拒绝反馈链 | `executeActions()` 已按列表顺序执行，`deny-actions`/默认 `no-permission` 已实现；Phase 2 要补自动化验证 |
| INP-01 | 服务端可以定义可复用输入流程，也可以在按钮内联定义输入流程 | `MenuRepository` 已支持 `prompts` 与内联 `input`；Phase 2 要验证运行时发起和生命周期，不是再改语法 |
| INP-02 | 玩家输入后可将 `{input}` 注入后续动作 | `ChatInputService.complete()` 已注入 `input` placeholder；Phase 2 要做 submit 行为测试 |
| INP-03 | 玩家可以通过取消关键字终止当前输入流程 | `config.yml` + prompt 局部 cancel 关键字已经存在；Phase 2 要验证 cancel path 和消息反馈 |
| INP-04 | 输入超时后插件会安全清理会话并提示玩家 | `ChatInputService` 已有 timeout task 和 cleanup；Phase 2 要验证调度推进与 session 清理 |
</phase_requirements>

## Summary

Phase 2 不是重写运行时层，而是把已经具备主干的运行时能力正式收口为“可计划、可展示、可回归”的合同。当前代码已经覆盖了绝大多数目标动作和输入生命周期：`MenuService` 已负责动作链执行、返回栈、权限拒绝与菜单跳转，`ChatInputService` 已负责 prompt 会话替换、取消、超时清理和异步聊天回到主线程后的动作执行。真正缺的是自动化验证、示例菜单的运行时展示面，以及对边界行为的显式确认。

对 planner 最重要的判断是：Phase 2 不需要引入新的动作种类，也不需要把 DSL 再做复杂。应该把工作切成四个明确切片：动作链执行合同、输入生命周期合同、权限/返回栈展示菜单、MockBukkit 测试扩展。这样既满足 `RUN-*`/`INP-*`，又不会误把 Phase 3 的线程/平台抽象或 Phase 4 的分页/条件系统带进来。

**Primary recommendation:** 保持 `MenuService`/`ChatInputService` 架构不变，用最小代码修正加上系统级 MockBukkit 测试和运行时示例菜单，把已有能力变成稳定交付面。

## Current Capability Inventory

### Already Exists
| Area | Evidence | Confidence |
|------|----------|------------|
| 核心动作模型 | `MenuAction` 已覆盖 `close/open/back/refresh/prompt/player/console/message/sound` | HIGH |
| 动作链顺序执行 | `MenuService.executeActions()` 逐项执行动作列表 | HIGH |
| 返回栈 | `history: MutableMap<UUID, ArrayDeque<String>>` + `NavigationMode.PUSH_CURRENT/ROOT/NONE` | HIGH |
| 权限拒绝反馈 | `permission`、`visible-permission`、`deny-actions` 与默认 `no-permission` 已实现 | HIGH |
| Prompt 运行时主流程 | `ChatInputService.startPrompt()/complete()/expire()` 已形成完整会话生命周期 | HIGH |
| 异步聊天回主线程 | `AsyncChatEvent` 中提取文本后，通过 `scheduler.runTask` 回到 Bukkit/Paper 安全边界 | HIGH |
| 基础示例菜单 | `history.yml` 展示 `back`，`admin.yml` 展示 `permission/back/refresh/message` | HIGH |
| Phase 1 测试基线 | 已有 MockBukkit 插件加载、命令打开菜单、DSL 解析回归 | HIGH |

### Partially Present
| Area | Current State | Gap |
|------|---------------|-----|
| 示例菜单展示面 | `history.yml`/`admin.yml` 已存在，但还没有把 deny path、visible-permission、prompt replace/timeout 作为正式示例合同 | 需要扩展示例或新增专用 runtime menu |
| 输入 DSL 与运行时衔接 | `MenuRepository` 已把复用 prompt 和内联 input 落到模型，当前只有解析测试 | 需要补“点击发起 prompt -> 异步聊天 -> submit/cancel/timeout”行为测试 |
| 动作链副作用可观测性 | 代码支持 player/console/message/sound，但没有 Phase 2 级别断言 | 需要用 MockBukkit 的命令、消息、音效和 inventory state 做断言 |
| 返回栈/刷新合同 | 代码已存在，但没有验证“来源栈语义”与 refresh 不污染历史 | 需要显式测试 |

### Real Gaps For Phase 2
| Gap | Why It Matters | Recommended Fix |
|-----|----------------|-----------------|
| 缺少点击级运行时测试 | 当前只验证 `/amenu` 能打开菜单，没有验证按钮行为 | 新增 `MenuRuntimeActionTest` 覆盖 click -> action chain |
| 缺少输入生命周期测试 | submit/cancel/replace/timeout 还没形成回归网 | 新增 `ChatInputServiceTest` 或等价测试类 |
| 缺少示例菜单的正式运行时展示 | 代码里支持不等于产品层可见 | 扩展 `history.yml`、`admin.yml` 或新增 `runtime.yml` |
| 缺少 Phase 2 文档表达 | 用户很难从 README 看出运行时层成熟度 | README 可选补充“运行时交互”小节，但不应成为主目标 |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.0.21 | 插件主语言 | 已在项目中固定，适合当前 Kotlin + Paper 代码风格 |
| Paper API | 1.21.11-R0.1-SNAPSHOT | 菜单、事件、调度 API | 与当前 MockBukkit 测试基线对齐 |
| MockBukkit | 4.108.0 (`mockbukkit-v1.21`) | Bukkit/Paper 单元与集成级 mock 测试 | 官方 README 直接推荐用于插件单元测试，并建议从 MockBukkit JAR manifest 对齐 Paper 版本 |
| JUnit Jupiter | 6.0.3 | JVM 测试框架 | 项目已采用，官方文档与 Maven 支持明确 |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| BukkitScheduler / Paper scheduler | Paper API 内置 | 主线程回切、超时任务推进 | prompt 提交、超时清理、测试里推进 tick |
| Adventure Component / plain serializer | Paper API 内置 | 聊天内容解析与断言 | 从 `AsyncChatEvent` 读取纯文本输入 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| MockBukkit 运行时测试 | 纯人工进服回归 | 无法稳定验证 timeout、deny path、动作顺序，回归成本高 |
| 保持当前服务分层 | 把逻辑塞回 Listener/Command | 会降低可测性，并破坏 Phase 1 已建立的层次边界 |
| 继续扩充 `main.yml` | 在二级示例菜单展示运行时能力 | `main.yml` 会失去最短 canonical DSL 的定位 |

**Version verification:**
- `pom.xml` 固定：Kotlin `2.0.21`、Paper API `1.21.11-R0.1-SNAPSHOT`、JUnit Jupiter `6.0.3`、MockBukkit `4.108.0`
- MockBukkit 官方 README 明确建议从 MockBukkit JAR manifest 提取 `Paper-Version` 对齐 Paper API
- JUnit 官方 User Guide 当前页重定向到 `6.0.3`

## Architecture Patterns

### Recommended Project Structure
```text
src/
├── main/kotlin/cc/keer/amenu/config/     # YAML -> runtime model
├── main/kotlin/cc/keer/amenu/service/    # action execution + prompt lifecycle
├── main/kotlin/cc/keer/amenu/gui/        # inventory listener / holder
├── main/resources/menus/                 # canonical + showcase menus
└── test/kotlin/cc/keer/amenu/            # MockBukkit runtime regression suite
```

### Pattern 1: Runtime Actions Stay In `MenuService`
**What:** 点击后的所有 menu runtime 副作用都由 `MenuService` 统一分发，Listener 只负责转发 click。  
**When to use:** 所有按钮动作、权限拒绝链、返回栈、菜单跳转。  
**Why:** 这能让 Phase 2 的测试聚焦在一个主入口 `handleClick()/executeActions()`，避免把验证散到多个监听器里。

### Pattern 2: Prompt Lifecycle Stays In `ChatInputService`
**What:** 输入会话的创建、替换、取消、超时、提交都由独立服务持有。  
**When to use:** 所有 `prompt` / 内联 `input` 的运行时行为。  
**Why:** 生命周期状态是 Phase 2 的核心复杂度，不能分散到菜单逻辑里。

### Pattern 3: Runtime Showcase Menus Are Separate From Canonical DSL
**What:** `main.yml` 保持最短写法，运行时高级演示放入 `history.yml`、`admin.yml` 或独立 runtime 示例。  
**When to use:** back、refresh、permission、deny-actions、prompt lifecycle 的产品展示。  
**Why:** 这符合 D-10 / D-11，也能避免 default example 膨胀。

### Recommended Planning Slices
1. **切片 A: 动作链执行合同**
   - 锁定 click -> `MenuService.handleClick()` -> 顺序动作执行
   - 验证 `open/back/refresh/close/player/console/message/sound`
2. **切片 B: 输入生命周期合同**
   - 锁定 prompt 发起、submit、cancel、replace、timeout
   - 锁定 `{input}` 注入和 async chat -> main thread handoff
3. **切片 C: 权限反馈与返回栈展示**
   - 用 bundled examples 明确展示 `permission`、`visible-permission`、`deny-actions`、`back`
4. **切片 D: MockBukkit 测试扩展**
   - 抽出共享测试夹具，统一插件加载、点击模拟、scheduler tick 推进、chat event 驱动

### Anti-Patterns to Avoid
- **把新动作种类塞进 Phase 2：** 这会冲掉当前 phase 的稳定化目标。
- **在 AsyncChatEvent 线程直接执行 Bukkit API：** Paper 文档明确提示这是不安全的。
- **用更复杂的导航状态机替代当前 history deque：** 现阶段没有需求，复杂度只会上升。
- **把 runtime showcase 塞回 `main.yml`：** 会破坏 Phase 1 已收口的最短 DSL。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| 运行时测试环境 | 自定义 Bukkit 假实现 | MockBukkit `ServerMock` / `PlayerMock` / scheduler | MockBukkit 已提供 inventory click、scheduler tick、plugin loading |
| Prompt 主线程回切 | 自建线程池或直接在 async chat 执行 Bukkit API | Bukkit/Paper scheduler `runTask` | Paper 文档明确建议需要 Bukkit API 时切回 scheduler |
| 导航上下文管理 | 复杂菜单状态机 | 当前 `history` deque | 当前需求只有来源菜单返回，不需要状态机 |
| 权限拒绝消息系统 | 独立权限框架 | 现有 `deny-actions` + `messages.no-permission` | 已满足当前产品面，避免重复抽象 |

**Key insight:** Phase 2 的挑战不是“缺框架”，而是把现有实现变成可验证合同。继续手搓基础设施只会放大范围。

## Common Pitfalls

### Pitfall 1: 在 `AsyncChatEvent` 里直接操作 GUI 或执行命令
**What goes wrong:** 事件线程不是 Bukkit 主线程，可能引发线程安全问题。  
**Why it happens:** 把“拿到输入”和“执行后续 Bukkit 行为”混为一步。  
**How to avoid:** 只在 async 线程里提取纯文本，然后用 `scheduler.runTask` 回主线程执行 submit/cancel actions。  
**Warning signs:** 输入完成后直接 `openInventory()`、`dispatchCommand()`、`playSound()` 写在事件处理函数体内。

### Pitfall 2: 只测“菜单能打开”，不测“动作链真的执行”
**What goes wrong:** Phase 2 看起来完成，实际上 click path、deny path、refresh/back 都可能回归。  
**Why it happens:** 继承了 Phase 1 只做命令打开验证的思路。  
**How to avoid:** Phase 2 测试必须覆盖按钮点击、副作用、顺序、权限分支。  
**Warning signs:** 测试里没有 `simulateInventoryClick()` 或 scheduler tick 推进。

### Pitfall 3: 把示例菜单当成全部运行时能力的堆放区
**What goes wrong:** `main.yml` 失去“最短 DSL”意义，用户误判产品复杂度。  
**Why it happens:** 想一次性展示全部能力。  
**How to avoid:** `main.yml` 只保留 canonical example，runtime showcase 放二级菜单。  
**Warning signs:** `main.yml` 开始重新出现权限、刷新、管理操作、复杂提示流。

### Pitfall 4: 把代码中“已支持”误当成“Phase 2 已完成”
**What goes wrong:** planner 低估缺口，导致 phase 完成后没有回归网和产品展示。  
**Why it happens:** 运行时能力已经有雏形，容易忽略验证成本。  
**How to avoid:** 区分“已实现”“已被测试”“已被示例和文档暴露”。  
**Warning signs:** 需求映射只能指向源码，不能指向测试和示例。

## Code Examples

Verified patterns from official sources and current codebase:

### MockBukkit Plugin Test Bootstrap
```kotlin
@BeforeEach
fun setUp() {
    server = MockBukkit.mock()
    plugin = MockBukkit.load(AMenuPlugin::class.java)
}

@AfterEach
fun tearDown() {
    MockBukkit.unmock()
}
```
Source: MockBukkit README usage pattern; same structure is already used in existing tests.

### Async Chat Handoff Back To Main Thread
```kotlin
@EventHandler
fun onAsyncChat(event: AsyncChatEvent) {
    event.isCancelled = true
    val message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim()
    plugin.server.scheduler.runTask(plugin) {
        complete(event.player, session, message)
    }
}
```
Source: Paper chat event docs state `AsyncChatEvent` is asynchronous and Bukkit API is unsafe there; scheduler docs recommend synchronous tasks for game logic.

### Scheduler-Driven Timeout Verification
```kotlin
menuService.handleClick(player, "main", promptSlot)
server.scheduler.performTicks(timeoutSeconds * 20L)
assertNull(player.openInventory.topInventory.holder as? MenuHolder)
```
Source: MockBukkit `BukkitSchedulerMock` exposes `performTicks(long)`; local code uses `runTaskLater` for prompt expiry.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 只证明命令能打开菜单 | 运行时层通过 MockBukkit 验证 click/input/timeout 行为 | Phase 2 | 让菜单插件从“配置可解析”升级为“交互可回归” |
| 默认示例承载全部展示 | `main.yml` 作为最短 DSL，runtime showcase 迁到二级示例 | Phase 1 -> Phase 2 | 更容易体现“更省配置” |
| 把异步聊天当作普通事件处理 | 明确 async capture + scheduler handoff | 已在当前代码中建立 | 避免 Bukkit API 线程安全问题 |

**Deprecated/outdated:**
- 把 Phase 2 当成新 DSL 扩张阶段：与当前 roadmap 冲突。
- 把正式 Folia 调度抽象纳入本 phase：已明确延期到 Phase 3。

## Open Questions

1. **输入生命周期测试是直接构造 `AsyncChatEvent`，还是通过 `PlayerMock.chat()` 驱动？**
   - What we know: `AsyncChatEvent` 构造器可用，`PluginManagerMock`/scheduler 支持异步事件与 tick 推进；`PlayerMock` 也有 `chat(String)`。
   - What's unclear: 哪条路径在当前 MockBukkit 版本下更稳定、更少依赖内部实现。
   - Recommendation: Wave 0 先做最小实验；优先选可稳定断言 timeout/cancel/replace 的驱动方式。

2. **runtime showcase 是扩展现有 `history.yml`/`admin.yml` 还是新增 `runtime.yml`？**
   - What we know: 两个文件已经各自承担 history/admin 场景。
   - What's unclear: 若强行继续扩展，会不会让示例语义变混乱。
   - Recommendation: 优先保持示例职责单一；如果 deny-actions/prompt lifecycle 会让现有文件过载，就新增独立 runtime example。

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Maven | `mvn test` / `mvn package` | ✓ | 3.9.12 | — |
| Java (system default) | Maven build | ✓ | 25.0.2 | 不可直接用，触发 enforcer |
| Java 21 local runtime | Maven build / test | ✓ | 21.0.10 | `.tools/jdk21/bin/java.exe` |
| MockBukkit dependency | Runtime regression tests | ✓ | 4.108.0 | — |

**Missing dependencies with no fallback:**
- None.

**Missing dependencies with fallback:**
- 默认 `java` 指向 25.0.2，不满足 `[21,22)`。执行阶段必须显式切到 `.tools/jdk21` 或其他 Java 21。

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 6.0.3 + MockBukkit 4.108.0 |
| Config file | none — Maven Surefire in `pom.xml` |
| Quick run command | `mvn "-Dtest=MenuRuntimeActionTest,ChatInputServiceTest" test` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RUN-01 | click 触发 `close/open/back/refresh` 且返回栈语义正确 | integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | ❌ Wave 0 |
| RUN-02 | click 触发 `player/console/message/sound`，副作用可观测 | integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | ❌ Wave 0 |
| RUN-03 | action chain 保持声明顺序，权限拒绝优先走 `deny-actions`，否则 fallback `no-permission` | integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | ❌ Wave 0 |
| INP-01 | 可复用 prompt 与内联 input 都能发起输入会话 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | ❌ Wave 0 |
| INP-02 | submit 后 `{input}` 注入后续动作 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | ❌ Wave 0 |
| INP-03 | cancel 关键字触发 cancel-actions | integration | `mvn "-Dtest=ChatInputServiceTest" test` | ❌ Wave 0 |
| INP-04 | timeout 后清理会话并发送提示 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn "-Dtest=MenuRuntimeActionTest,ChatInputServiceTest" test`
- **Per wave merge:** `mvn test`
- **Phase gate:** `mvn test` green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt` — 覆盖 RUN-01/RUN-02/RUN-03
- [ ] `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt` — 覆盖 INP-01/02/03/04
- [ ] 共享测试辅助：菜单打开、slot 定位、消息/声音/命令断言、scheduler tick 推进
- [ ] Java 21 执行路径统一：测试命令需要明确使用 `.tools/jdk21` 或等价 Java 21 环境

## Sources

### Primary (HIGH confidence)
- Local code: `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - 动作链、返回栈、权限反馈现状
- Local code: `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - prompt 生命周期、async chat handoff、timeout cleanup
- Local code: `src/main/kotlin/cc/keer/amenu/gui/MenuListener.kt` - 点击入口
- Local code: `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` / `MenuRepository.kt` - runtime model 与 YAML 落模方式
- Local resources: `src/main/resources/config.yml`, `menus/history.yml`, `menus/admin.yml` - 系统消息与 bundled examples 现状
- Local tests: `MenuRepositoryDslTest.kt`, `AMenuCommandTest.kt` - 当前测试覆盖边界
- MockBukkit README - https://github.com/MockBukkit/MockBukkit
- Paper Docs: Chat events - https://docs.papermc.io/paper/dev/chat-events/
- Paper Docs: Scheduler - https://docs.papermc.io/paper/dev/scheduler/
- JUnit User Guide 6.0.3 - https://docs.junit.org/6.0.3/overview.html

### Secondary (MEDIUM confidence)
- `javap` inspection of local dependencies:
  - `org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock`
  - `org.mockbukkit.mockbukkit.entity.PlayerMock`
  - `org.mockbukkit.mockbukkit.plugin.PluginManagerMock`
  - `io.papermc.paper.event.player.AsyncChatEvent`

### Tertiary (LOW confidence)
- None.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - 项目依赖、MockBukkit README、JUnit/Paper 官方文档一致
- Architecture: HIGH - 主要来自当前代码结构与 locked decisions，范围稳定
- Pitfalls: HIGH - Paper 官方文档对 async chat 和 scheduler 边界给出明确约束

**Research date:** 2026-04-02
**Valid until:** 2026-05-02
