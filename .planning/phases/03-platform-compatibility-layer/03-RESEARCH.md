# Phase 3: Platform Compatibility Layer - Research

**Researched:** 2026-04-02  
**Status:** Ready for planning

## Scope Recap

Phase 3 is not a feature phase. It hardens AMenu's existing runtime for supported server platforms and turns Folia support from an implicit future idea into an explicit compatibility contract.

The phase must satisfy:
- `COMP-01`: runtime menu behavior respects Spigot/Paper main-thread GUI and command boundaries
- `COMP-02`: the plugin stops assuming one global main thread and introduces a Folia-compatible scheduling abstraction
- `COMP-03`: bundled example menus remain examples of a general menu engine rather than redefining the product as a skin-only plugin

## Current Implementation Findings

### 1. Compatibility pressure is concentrated in three runtime entrypoints

The current code keeps runtime behavior nicely centralized, which makes Phase 3 tractable:
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` owns menu opening, navigation, and action dispatch
- `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` owns async chat capture, timeout scheduling, cancel/submit flow, and lifecycle cleanup
- `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt` owns plugin bootstrap, reload, and shutdown lifecycle

This means Phase 3 should add a compatibility layer beneath these services rather than rewriting the DSL or menu repository.

### 2. Direct Bukkit scheduler assumptions still exist

`ChatInputService.kt` currently uses:
- `plugin.server.scheduler.runTaskLater(...)`
- `plugin.server.scheduler.runTask(...)`

Those calls work for classic Bukkit/Paper expectations, but they make two assumptions that Phase 3 must eliminate:
- a single global safe thread is always the correct execution target
- async chat completion can always hand off through the classic scheduler instead of a player/region-safe scheduler

### 3. Packaging correctness is already part of platform compatibility

The recent `/amenu` crash proved that compatibility is broader than thread scheduling:
- `TextFormatter.kt` directly references `MiniMessage`
- the plugin previously relied on server runtime availability instead of shading the dependency
- adding `adventure-bom` plus `adventure-text-minimessage` to `pom.xml` fixed the runtime `NoClassDefFoundError`

Planning must therefore treat packaging/runtime dependency correctness as part of the compatibility phase, not as an unrelated build detail.

## Recommended Architecture

### 1. Introduce a narrow `PlatformScheduler` abstraction

The smallest credible interface for AMenu is:
- `isFolia`
- `isPlayerThread(player)`
- `executeFor(player, task)`
- `executeGlobal(task)`
- `runLaterFor(player, delayTicks, task)`
- `runRepeatingFor(player, delayTicks, periodTicks, task)`
- `TaskHandle.cancel()`

This is narrow enough to avoid framework creep, but broad enough to cover:
- opening inventories
- action execution after clicks
- prompt submit/cancel completion
- timeout expiry
- reload/shutdown cleanup

### 2. Use runtime-facade services, not platform branches in YAML or commands

The scheduler abstraction should sit below:
- `AMenuPlugin`
- `MenuService`
- `ChatInputService`

It should not leak into:
- YAML parsing
- menu models
- command argument parsing

That keeps Phase 3 aligned with the current architecture and prevents compatibility logic from contaminating the DSL.

### 3. Prefer reflective Folia support with a Bukkit fallback

MC Plugin Neuron surfaced an existing stable route from another Bukkit GUI project:
- `PlatformScheduler`
- `BukkitPlatformScheduler`
- `FoliaPlatformScheduler`
- `PlatformSchedulerFactory`
- `TaskHandle`

The pattern is:
- detect Folia support reflectively rather than hard-linking to Folia-only APIs
- use player-bound or global schedulers when available
- fall back to Bukkit scheduling when reflective hooks are not present

This is a good fit for AMenu because it preserves:
- one jar
- Spigot/Paper compatibility
- explicit Folia support

It also reduces risk versus a Paper-only refactor or a compile-time Folia split.

### 4. Converge all thread-sensitive runtime paths through the abstraction

The important design choice is not just "add a scheduler class"; it is "stop letting each service invent its own thread handoff."

Phase 3 should route these paths through the compatibility layer:
- `MenuService.openDefaultMenu()`
- `MenuService.openMenu()`
- `MenuService.openBack()`
- `MenuService.handleClick()` / `executeActions()`
- `ChatInputService.startPrompt()`
- `ChatInputService.onAsyncChat()` completion handoff
- `ChatInputService` timeout scheduling
- reload/shutdown cleanup paths that cancel active tasks or sessions

The clearest contract is:
- async listeners may parse data off-thread
- any Bukkit inventory open/close, command dispatch, message chain side effect, or prompt completion action runs through `PlatformScheduler`

## Verification Implications

### 1. MockBukkit remains necessary but insufficient

MockBukkit is still the fastest proof for:
- command entry
- menu navigation behavior
- prompt lifecycle behavior
- scheduler-driven timeout cleanup

But MockBukkit does not prove:
- real Folia scheduler selection
- player/region ownership behavior
- startup/runtime packaging on a live server

So Phase 3 needs both automated regression and a documented smoke boundary.

### 2. Compatibility proof should be split into two layers

Layer A: automated local proof
- scheduler factory tests prove classic runtime selects Bukkit mode under MockBukkit
- service regressions prove `MenuService` and `ChatInputService` no longer depend directly on `plugin.server.scheduler`
- bundled menu regression keeps `main.yml`, `history.yml`, `admin.yml`, and `runtime.yml` loadable after compatibility refactors
- packaging verification proves `MiniMessage.class` remains inside the shaded jar

Layer B: live-platform smoke proof
- boot plugin on Paper
- boot plugin on Folia
- confirm startup succeeds without `NoClassDefFoundError`, scheduler reflection failure, or command registration failure
- manually open `/amenu`, trigger one prompt flow, cancel it, and confirm shutdown/reload does not leave hanging prompt tasks

### 3. Compatibility claims must say what is automated versus manual

Phase 3 should not overclaim. The repository should say exactly:
- what is covered by MockBukkit/JUnit
- what is covered by `mvn package` + shaded jar inspection
- what still requires Paper/Folia smoke verification

That will make later release hardening cleaner.

## Validation Architecture

Phase 3 should use a two-wave validation architecture.

### Wave 1 validation

Focus:
- compatibility abstraction
- service refactor
- scheduler-dependent regressions

Fast loop:
- `mvn "-Dtest=PlatformSchedulerFactoryTest,MenuServiceCompatibilityTest,ChatInputServiceCompatibilityTest" test`

Wave gate:
- `mvn test`

### Wave 2 validation

Focus:
- bundled example compatibility
- command/reload compatibility
- packaging correctness
- operator-facing compatibility docs and smoke scripts

Fast loop:
- `mvn "-Dtest=BundledMenuCompatibilityTest,AMenuCommandTest" test`

Wave gate:
- `mvn test`
- `mvn package`
- `jar tf target/amenu-1.0.0-SNAPSHOT-shaded.jar` contains `net/kyori/adventure/text/minimessage/MiniMessage.class`

### Manual-only validation

Required after execution:
- Paper startup smoke
- Folia startup smoke
- player opens `/amenu`
- one prompt submit/cancel roundtrip
- reload/shutdown with an active prompt session

## Recommended Plan Split

### 03-01: Platform scheduler abstraction and runtime handoff convergence

This plan should:
- add the platform scheduler package
- wire `AMenuPlugin`, `MenuService`, and `ChatInputService` to it
- add focused compatibility regressions

It primarily serves `COMP-01` and `COMP-02`.

### 03-02: Compatibility verification, packaging proof, and product-boundary docs

This plan should:
- add bundled example compatibility regressions
- document Paper/Folia smoke steps
- prove shaded runtime dependencies remain packaged
- keep README/plugin metadata aligned with the general menu-engine positioning

It closes the loop for `COMP-01`, `COMP-02`, and `COMP-03`.

## Risks To Avoid During Planning

- Do not expand the menu DSL or add Phase 4 features while touching services
- Do not scatter Folia conditionals across unrelated classes; keep them inside the compatibility layer
- Do not rely on MockBukkit alone as proof of Folia compatibility
- Do not let bundled skin examples rewrite the product story
- Do not regress the MiniMessage packaging fix while changing build or smoke steps

## Planning Notes For The Planner

- Keep the abstraction narrow and implementation-focused
- Reuse the existing shared MockBukkit harness rather than creating a second testing style
- Make acceptance criteria grep-able and command-verifiable
- Ensure at least one plan explicitly verifies jar packaging and smoke-path documentation

---

*Phase: 03-platform-compatibility-layer*  
*Research completed: 2026-04-02*
