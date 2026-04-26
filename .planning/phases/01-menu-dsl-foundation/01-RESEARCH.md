# Phase 1: Menu DSL Foundation - Research

**Researched:** 2026-04-02
**Domain:** Bukkit/Paper menu DSL foundation for a Kotlin + Maven plugin
**Confidence:** MEDIUM

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
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

### Claude's Discretion
- DSL 字段的具体命名细节，只要不违背“更短、更直观”的目标
- README 的组织方式、示例顺序和措辞
- 示例菜单里保留哪些演示按钮，只要不重新把产品写成皮肤专用插件

### Deferred Ideas (OUT OF SCOPE)
- Folia 正式调度兼容与线程安全抽象 - Phase 3
- 分页、异步数据菜单 - Phase 4
- 条件系统、变量系统、更多绑定入口 - Phase 4
- 更丰富的业务示例菜单库 - 可在 Phase 4 之后继续扩展
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| MENU-01 | 服主可以通过 YAML 定义菜单标题、布局字符、按钮映射与默认填充位 | 锁定 `title + layout + fill + buttons` 为 Phase 1 核心 DSL 面，保持 `layout` 为布局真源，`fill` 自动映射 `#` |
| MENU-02 | 服主可以使用更短的按钮写法，如 `head`、`click`、内联 `input` | 将 `head`、`click`、内联 `input` 定义为文档与示例中的 canonical 写法；其余别名仅保留兼容 |
| MENU-03 | 玩家可以通过通用命令打开默认菜单或指定菜单 | 维持 `/amenu` 作为唯一主入口，`/skinmenu` 仅兼容；统一 README、`plugin.yml`、`pom.xml` 描述 |
</phase_requirements>

## Summary

Phase 1 不需要重做菜单引擎，当前代码已经有足够的基础面。`MenuRepository` 已经支持 `fill`、`head`、`click`、内联 `input`、动作别名、`templates`、`prompts`，`AMenuCommand` 已经支持 `/amenu`、`/amenu open <menuId>`、`/amenu reload`，而 `MenuService` / `ChatInputService` 已经把配置解析、运行时动作、输入会话拆层。对规划最重要的判断是：本阶段应当“收敛和显化已有能力”，而不是再发明新语法。

最小稳定 DSL 应只锁定一条 happy path：菜单根层只强调 `title`、`layout`、`fill`、`buttons`；按钮层只强调 `material` 或 `head`、`name`、`lore`、`amount`、`glow`、`click`，以及需要输入时使用内联 `input`。`templates`、`prompts`、`template`、`permission`、`visible-permission`、`deny-actions`、`rows`、以及 `actions` / `run` / `texture` / `deny` 这些兼容别名都可以继续支持，但不应出现在第一屏示例里。

最大的产品风险不是功能不够，而是“默认表达过满”。如果 README、示例菜单、命令别名、`pom.xml` 描述、`plugin.yml` 元数据还同时暴露皮肤语义、重复别名和高级字段，用户会先感知复杂度，再感知能力。这会直接违背“比 TRMenu 更省配置”的目标。

**Primary recommendation:** 把 Phase 1 规划成“DSL 合同收敛 + 对外入口/元数据对齐 + 最小验证脚手架”，不要规划新功能开发。

## Standard Stack

### Core
| Library / Tool | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin | 2.0.21 | 插件实现语言 | 已在 `pom.xml` 固定，当前源码全部基于 Kotlin |
| Maven | 3.9.x | 构建与测试生命周期 | 已有 Maven 项目结构，Phase 1 不应引入额外构建系统 |
| Paper API | 1.20.6-R0.1-SNAPSHOT | Bukkit/Paper 插件 API | 已在 `pom.xml` 固定，当前命令、GUI、事件、配置读取均依赖此层 |
| Bukkit `YamlConfiguration` | Paper/Bukkit API 内置 | 菜单与插件配置加载 | 当前 `MenuRepository` 已使用，足够支撑 Phase 1 DSL 收敛，无需自定义解析器 |

### Supporting
| Library / Tool | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Adventure Components | 随 Paper API 提供 | 标题与消息渲染 | 继续用于菜单标题和系统消息，不新增额外文本层 |
| JUnit Jupiter | 6.0.3 | 单元/集成测试框架 | Phase 1 需要补最基础 DSL 合同测试与命令入口测试 |
| MockBukkit | `org.mockbukkit.mockbukkit:mockbukkit-v1.21` 产品线 | Bukkit/Paper 插件测试宿主 | 用于加载插件、创建玩家、驱动命令与仓库读取测试 |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Bukkit 自带 YAML 解析 | 自定义 DSL 解析器 | Phase 1 只会增加复杂度和维护成本，没有产品收益 |
| 文档只展示一套最完整复用 DSL | 顶层 `templates` / `prompts` 先行 | 更强，但首屏体感会变长，直接损害“更省配置”的目标 |
| 单一 `/amenu` 主入口 | 保留多个泛用别名 | 兼容性更强，但产品认知会继续发散 |

**Installation / setup for Phase 1 validation:**
```xml
<!-- test scope only -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>6.0.3</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.mockbukkit.mockbukkit</groupId>
  <artifactId>mockbukkit-v1.21</artifactId>
  <version>[pin in Wave 0]</version>
  <scope>test</scope>
</dependency>
```

**Version verification notes:**
- Kotlin `2.0.21` and Paper `1.20.6-R0.1-SNAPSHOT` are verified from local `pom.xml`.
- JUnit current docs resolve to `6.0.3` on official JUnit docs.
- MockBukkit official README documents the `mockbukkit-v1.21` artifact line for Maven, but the exact version number still needs pinning during Wave 0.

## Architecture Patterns

### Recommended Project Structure
```text
src/main/kotlin/cc/keer/amenu/
├── config/       # YAML parsing + immutable DSL model
├── command/      # /amenu entry and tab completion
├── service/      # menu runtime, actions, chat input
├── gui/          # inventory holder/listener glue
└── util/         # item/text helpers

src/main/resources/
├── plugin.yml
├── config.yml
└── menus/        # bundled example menus
```

### Pattern 1: Canonical DSL Core
**What:** 只把 `title`、`layout`、`fill`、`buttons` 作为菜单根层稳定面；按钮层只把 `material` / `head`、`name`、`lore`、`amount`、`glow`、`click`、内联 `input` 当作首选写法。  
**When to use:** README 第一段示例、默认示例菜单、Phase 1 解析器命名收敛。  
**Example:**
```yaml
# Source: local repo behavior verified by MenuRepository + main.yml
title: "<gold><bold>示例菜单</bold></gold>"

layout:
  - "#########"
  - "##A#B#C##"
  - "#########"

fill:
  material: WHITE_STAINED_GLASS_PANE
  name: " "

buttons:
  "B":
    head: "base64..."
    name: "<aqua><bold>输入示例</bold></aqua>"
    click:
      - "[close]"
    input:
      start:
        - "<yellow>请输入内容</yellow>"
      submit:
        - "[message] 你输入了 {input}"
        - "[back]"
      cancel:
        - "[back]"
```

### Pattern 2: Parser Tolerant, Docs Strict
**What:** 解析器可以继续接受兼容别名，但文档只“祝福”一套最短命名。  
**When to use:** `MenuRepository` 保兼容时；README 和 bundled example 需要降复杂度时。  
**Canonical first:** `head` 胜过 `texture`，`click` 胜过 `actions` / `run`，`deny-actions` 胜过 `deny`。  

### Pattern 3: Enhancement Layer, Not Default Path
**What:** `templates`、`prompts`、`template`、`permission`、`visible-permission`、`deny-actions` 继续保留，但从第一屏示例和最小文档路径移出。  
**When to use:** 需要复用、权限显隐、拒绝反馈、可复用输入流时。  
**Reason:** 这些能力更适合在 Phase 2 文档或“进阶写法”小节展示，而不是占据最小 DSL 的心智入口。  

### Pattern 4: `layout` Is the Source of Truth
**What:** 对外文档以 `layout` 行数定义菜单高度，`rows` 仅保留兼容或内部修正用途。  
**When to use:** 所有新示例和 README。  
**Reason:** 当前解析器已经能从 `layout` 推导 `rows`；继续同时强调两者只会制造重复概念。  

### Anti-Patterns to Avoid
- **别名平权展示:** 同时在文档里展示 `click/actions/run`、`head/texture`、`deny/deny-actions` 会让 DSL 看起来比实际更复杂。
- **把增强层写成必经之路:** 在第一个示例里放 `templates` 或顶层 `prompts`，会让用户误以为简单菜单也要先建复用结构。
- **示例反向定义产品:** README、`plugin.yml`、`pom.xml` 如果继续保留 skin-only 表述，会抵消 Phase 1 的定位纠偏。
- **把运行时语义塞回配置层:** `MenuRepository` 只负责解析和归一化；导航、权限反馈、输入会话仍应留在 `MenuService` / `ChatInputService`。

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| YAML DSL 解析 | 自定义 token/parser 系统 | 继续使用 Bukkit `YamlConfiguration` + `MenuRepository` 归一化 | 当前 Phase 1 需求只是字段收敛，不是语法创新 |
| 菜单高度声明 | 同时让 `rows` 和 `layout` 作为一等概念 | 以 `layout` 为文档真源，`rows` 仅兼容 | 避免重复配置和认知分裂 |
| 输入流复用 | 强迫所有输入先定义顶层 `prompts` | 优先内联 `input`，需要复用时再用 `prompts` | `MENU-02` 的重点是更短写法，不是复用能力 |
| 第二套命令入口 | 新增 skin-specific 或 experimental 主命令 | 继续收敛到 `/amenu` | Phase 1 的目标是入口一致性 |

**Key insight:** 这个阶段最该避免的是“为了看起来更强而让默认写法更长”。已经存在的能力要保留，但必须从默认路径后退一步。

## Common Pitfalls

### Pitfall 1: 让 README 看起来像高级 DSL 手册
**What goes wrong:** 首屏示例同时出现 `templates`、`prompts`、权限显隐、拒绝动作、多个动作别名。  
**Why it happens:** 当前解析器支持得比 Phase 1 需要更多，作者容易把“能写”的都放进“该展示”的位置。  
**How to avoid:** README 和 `menus/main.yml` 只保留一条最短 happy path，把增强字段单列成“可选增强层”。  
**Warning signs:** 第一屏 YAML 超过一个屏幕，或需要解释多个同义字段。  

### Pitfall 2: 命令与元数据纠偏不彻底
**What goes wrong:** 代码里主命令已经是 `/amenu`，但 `plugin.yml` 仍保留过多 alias，`pom.xml` 描述仍写 skin menu。  
**Why it happens:** 早期皮肤菜单原型痕迹仍留在文档和元数据。  
**How to avoid:** 把 Phase 1 明确规划为“README + `plugin.yml` + `pom.xml` + 示例菜单”同步纠偏。  
**Warning signs:** 用户第一次阅读后仍把插件理解为皮肤插件。  

### Pitfall 3: 把兼容别名也纳入稳定合同
**What goes wrong:** `actions` / `run` / `texture` / `deny` 在文档中与 canonical 字段并列，后续很难再简化。  
**Why it happens:** 为了兼容旧示例或减少即时改动，把内部兼容当成对外合同。  
**How to avoid:** 解析器继续支持，文档只写 canonical 字段。必要时在代码注释中标记 legacy alias。  
**Warning signs:** 规划文档开始使用“任选其一”的表述。  

### Pitfall 4: 验证命令默认失败
**What goes wrong:** Phase 1 计划里写了 `mvn test` 或 `mvn package`，但执行机默认只有 Java 25。  
**Why it happens:** `pom.xml` 的 Java 21 约束已存在，但当前环境没有 JDK 21。  
**How to avoid:** 把 Java 21 切换或安装写进 Wave 0。  
**Warning signs:** `maven-enforcer-plugin` 直接拦截构建。  

## Code Examples

Verified patterns grounded in the current repo and official testing docs:

### Minimal Canonical Menu
```yaml
# Source: local repo - MenuRepository + bundled menus
title: "<gold><bold>主菜单</bold></gold>"

layout:
  - "#########"
  - "##N#O#P##"
  - "#########"

fill:
  material: WHITE_STAINED_GLASS_PANE
  name: " "

buttons:
  "O":
    material: PAPER
    name: "<green><bold>打开子菜单</bold></green>"
    click:
      - "[open other]"
```

### Inline Input Is the Happy Path, Not Top-Level `prompts`
```yaml
# Source: local repo - main.yml pattern
buttons:
  "I":
    head: "base64..."
    name: "<aqua><bold>输入</bold></aqua>"
    click:
      - "[close]"
    input:
      start:
        - "<yellow>请输入内容</yellow>"
      submit:
        - "[player] somecmd {input}"
        - "[back]"
      cancel:
        - "[back]"
```

### MockBukkit Test Harness Shape
```kotlin
// Source: MockBukkit official README usage pattern, adapted to Kotlin
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

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| 用皮肤菜单原型反向定义产品 | 用通用菜单引擎定义产品，皮肤菜单只是 bundled example | 2026-04-02 scope correction | Phase 1 规划必须把文档、命令、元数据全部纠偏 |
| 把可复用结构和增强能力提前展示 | 默认路径优先展示最短 YAML，增强层后置 | 2026-04-02 planning direction | 首次上手成本下降，TRMenu 对比点更清晰 |
| 多别名、多入口并存 | `/amenu` 为主入口，字段采用 canonical first | 2026-04-02 planning direction | 对外合同更清楚，后续文档和测试更容易锁定 |

**Deprecated / outdated for Phase 1 docs:**
- 把 `pom.xml` 描述写成 skin menu plugin：不符合当前产品定位。
- 在第一屏文档里同时强调 `rows` 与 `layout`：不符合最小 DSL 目标。
- 让 `templates` / `prompts` 占据默认示例路径：不符合 `MENU-02` 的“更短写法”目标。

## Open Questions

1. **`rows` 是否还要对外保留文档地位？**
   What we know: 当前解析器支持 `rows`，但 `layout` 已可推导高度。  
   What's unclear: 是否存在必须只写 `rows` 的真实场景。  
   Recommendation: Phase 1 文档不再强调 `rows`；解析兼容保留即可。  

2. **`permission` / `visible-permission` / `deny-actions` 要不要出现在 Phase 1 默认示例？**
   What we know: 代码和现有示例已支持，但它们更偏运行时交互能力。  
   What's unclear: 是否会稀释 Phase 1 的“最短 DSL”心智。  
   Recommendation: 解析保留，默认示例移出，仅在“增强写法”小节或 Phase 2 文档展示。  

3. **是否保留 `menux` / `skingui` 额外 alias？**
   What we know: `plugin.yml` 当前还保留了它们，但 CONTEXT 只锁定 `/skinmenu` 为兼容别名。  
   What's unclear: 是否有真实兼容需求依赖这两个 alias。  
   Recommendation: Phase 1 规划应优先移除，除非发现现有用户依赖。  

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 21 | Maven build, tests, packaging | ✗ | Installed default is 25.0.2 | Install/switch to JDK 21 before validation |
| Maven | Build/test lifecycle | ✓ | 3.9.12 | — |
| Paper API repository | Dependency resolution | ✓ in `pom.xml` | `1.20.6-R0.1-SNAPSHOT` configured | — |
| JUnit Jupiter | Phase 1 validation tests | ✗ not yet in `pom.xml` | — | Add test-scope dependency |
| MockBukkit | Plugin command / repository tests | ✗ not yet in `pom.xml` | — | Add test-scope dependency |

**Missing dependencies with no fallback:**
- Java 21 on the active build path. Current `mvn package` fails immediately under Java 25 due to `maven-enforcer-plugin`.

**Missing dependencies with fallback:**
- JUnit Jupiter and MockBukkit are absent now, but can be added in test scope without affecting runtime packaging.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 6.0.3 + MockBukkit (`mockbukkit-v1.21` line) |
| Config file | none — Maven defaults via `pom.xml` |
| Quick run command | `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test` |
| Full suite command | `mvn test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| MENU-01 | 读取 `title`、`layout`、`fill`、`buttons` 并正确映射默认填充位 | unit/integration | `mvn -Dtest=MenuRepositoryDslTest#loads_core_layout_fill_and_buttons test` | ❌ Wave 0 |
| MENU-02 | `head`、`click`、内联 `input` 解析成稳定模型和动作链 | unit/integration | `mvn -Dtest=MenuRepositoryDslTest#parses_shorthand_head_click_and_inline_input test` | ❌ Wave 0 |
| MENU-03 | `/amenu` 打开默认菜单，`/amenu open <menuId>` 打开指定菜单 | integration | `mvn -Dtest=AMenuCommandTest#opens_default_and_named_menu test` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green under Java 21 before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` — covers `MENU-01`, `MENU-02`
- [ ] `src/test/kotlin/cc/keer/amenu/command/AMenuCommandTest.kt` — covers `MENU-03`
- [ ] `pom.xml` test dependencies for JUnit Jupiter + MockBukkit
- [ ] Maven Surefire / Kotlin test execution sanity check under Java 21
- [ ] Java 21 on the active build path

## Sources

### Primary (HIGH confidence)
- Local repo: `.planning/phases/01-menu-dsl-foundation/01-CONTEXT.md` - locked decisions, scope guardrails, canonical research boundary
- Local repo: `.planning/REQUIREMENTS.md` - `MENU-01` / `MENU-02` / `MENU-03` requirement text
- Local repo: `.planning/PROJECT.md` - product定位、技术约束、维护边界
- Local repo: `.planning/ROADMAP.md` - Phase 1 goal, existing plan slices, deferred scope
- Local repo: `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - current DSL stable model
- Local repo: `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - current shorthand parsing and alias behavior
- Local repo: `src/main/kotlin/cc/keer/amenu/command/AMenuCommand.kt` - command entry already aligned to `/amenu`
- Local repo: `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` and `ChatInputService.kt` - runtime / input responsibilities remain outside the parser
- Local repo: `src/main/resources/menus/main.yml`, `README.md`, `src/main/resources/plugin.yml`, `pom.xml` - current outward DSL feel and metadata drift
- PaperMC Docs: https://docs.papermc.io/paper/dev/how-do-plugins-work/ - plugin lifecycle, permissions, config storage, scheduler guidance

### Secondary (MEDIUM confidence)
- MockBukkit official README: https://github.com/MockBukkit/MockBukkit - Maven artifact line, basic test harness shape, skipped-test caveat
- MockBukkit docs: https://docs.mockbukkit.org/docs/en/user_guide/advanced/paperweight - confirms official docs site and active documentation set
- JUnit docs: https://docs.junit.org/current/user-guide/ - current official user guide resolves to 6.0.3

### Tertiary (LOW confidence)
- None

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM - runtime stack is verified locally, but exact MockBukkit version still needs pinning in Wave 0
- Architecture: HIGH - recommendations are derived directly from the current codebase and locked CONTEXT decisions
- Pitfalls: HIGH - risks are visible in current repo state (`pom.xml` skin wording, alias sprawl, advanced fields in default example, Java 25 build failure)

**Research date:** 2026-04-02
**Valid until:** 2026-05-02
