# Phase 3: Platform Compatibility Layer - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 3 hardens AMenu for Spigot and Paper, and introduces a maintainable Folia compatibility abstraction for runtime menu execution. The scope is compatibility and execution safety: scheduler handoff, thread/region-safe menu actions, safe reload/open paths, and a verification matrix for bundled examples and chat input flows.

This phase does not add new menu capabilities. Pagination, conditions, variable systems, async list features, and richer bindings remain Phase 4 work.

</domain>

<decisions>
## Implementation Decisions

### Platform Baseline
- **D-01:** Phase 3 must satisfy `COMP-01`, `COMP-02`, and `COMP-03` without expanding the menu DSL or action catalog.
- **D-02:** The compatibility target is modern Spigot + Paper first, with Folia treated as an explicit supported execution mode rather than a best-effort afterthought.
- **D-03:** Compatibility planning should assume the current Paper-facing code paths are the reference behavior, and the work is to isolate platform assumptions rather than rewrite runtime features.

### Scheduler And Thread Boundaries
- **D-04:** Runtime services must stop depending directly on `plugin.server.scheduler` as the only execution path. Phase 3 should introduce a platform scheduler abstraction that owns sync handoff, delayed tasks, and player-bound execution semantics.
- **D-05:** `ChatInputService` remains the canonical example of a thread boundary: async chat capture may read input text, but all menu actions, GUI state changes, reload effects, and command dispatch must execute through the compatibility abstraction on a platform-safe path.
- **D-06:** Menu opening, refresh, back navigation, prompt completion, and timeout cleanup should all converge on the same compatibility layer instead of each service making its own scheduler assumptions.

### Verification Strategy
- **D-07:** MockBukkit remains the fast regression baseline for service-level behavior, but it is not sufficient evidence for Folia compatibility by itself.
- **D-08:** Phase 3 must add a compatibility verification path that includes at least startup/runtime smoke coverage for Paper and Folia, plus a clear statement of what is proven automatically versus what still needs live manual confirmation.
- **D-09:** Spigot/Paper/Folia verification should focus on command entry, menu open/reload safety, prompt lifecycle handoff, and shutdown cleanup before considering any future advanced feature coverage.

### Packaging And Runtime Dependencies
- **D-10:** The plugin may not assume every Adventure component used by the code is present in the server runtime. Runtime-only text libraries that AMenu references directly must be packaged with the plugin when needed.
- **D-11:** Compatibility work should treat the recent `MiniMessage` crash as a class of problem to prevent: platform support includes packaging correctness, not just scheduler correctness.

### Product Boundary
- **D-12:** Bundled menus must remain examples of a general Minecraft menu engine. Compatibility work must not rename the product, re-center it around skin-only behavior, or let example commands redefine plugin scope.
- **D-13:** `main.yml` stays the canonical short example, while compatibility verification should prove that `history.yml`, `admin.yml`, and `runtime.yml` remain safe and portable across supported platforms.

### the agent's Discretion
- Exact abstraction names and file layout for the compatibility layer are open.
- The planner may decide whether diagnostic logging, startup warnings, or platform capability banners belong in Phase 3 if they directly support compatibility verification.
- The exact split between automated smoke scripts and documented manual smoke steps is open, as long as the final verification surface is explicit.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and requirements
- `.planning/PROJECT.md` - Product identity, constraints, and scope discipline
- `.planning/REQUIREMENTS.md` - `COMP-01`, `COMP-02`, and `COMP-03` definitions
- `.planning/ROADMAP.md` - Phase 3 goal, plan slots, and dependency chain
- `.planning/STATE.md` - Current milestone position after Phase 2 completion

### Prior phase contracts
- `.planning/phases/01-menu-dsl-foundation/01-VERIFICATION.md` - Canonical command and DSL contract that compatibility work must preserve
- `.planning/phases/02-runtime-interaction-layer/02-CONTEXT.md` - Locked runtime behavior decisions from Phase 2
- `.planning/phases/02-runtime-interaction-layer/02-01-SUMMARY.md` - Runtime regression harness and service-layer verification pattern
- `.planning/phases/02-runtime-interaction-layer/02-02-SUMMARY.md` - Bundled showcase split between `main.yml` and richer secondary examples
- `.planning/phases/02-runtime-interaction-layer/02-VERIFICATION.md` - Verified runtime behavior that Phase 3 must keep intact

### Existing code that defines compatibility pressure points
- `pom.xml` - Build/runtime packaging model, Paper API target, Java 21 requirement, and shaded jar behavior
- `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt` - Bootstrap, reload, enable/disable lifecycle, bundled resource writes
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - Menu open/refresh/back/action execution paths
- `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - Async chat capture, delayed timeout tasks, reload/shutdown cleanup
- `src/main/kotlin/cc/keer/amenu/gui/MenuListener.kt` - Inventory click entrypoint into runtime services
- `src/main/kotlin/cc/keer/amenu/util/TextFormatter.kt` - Direct runtime dependency on Adventure formatting libraries
- `src/main/resources/plugin.yml` - Public command and permission contract that compatibility work must preserve

### Bundled examples and regression coverage
- `src/main/resources/menus/main.yml` - Canonical short example that must stay simple
- `src/main/resources/menus/history.yml` - Back-stack and routed showcase example
- `src/main/resources/menus/admin.yml` - Permission and refresh showcase example
- `src/main/resources/menus/runtime.yml` - Prompt and timeout showcase example
- `src/test/kotlin/cc/keer/amenu/support/MenuPluginTestHarness.kt` - Shared MockBukkit harness
- `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt` - Runtime action regression coverage
- `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt` - Prompt lifecycle regression coverage

### External specs
- No external specs outside the repository are currently required; Phase 3 requirements are fully captured in the repository refs above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MenuService`: already centralizes menu action execution and navigation semantics; Phase 3 should preserve this as the high-level runtime facade.
- `ChatInputService`: already models the key async-to-sync boundary for prompt handling and is the best place to anchor platform-safe execution design.
- `MenuPluginTestHarness`: already provides a shared regression base for compatibility-safe refactors before adding live-platform smoke coverage.

### Established Patterns
- Runtime logic is already separated into repository/config, GUI listener, menu service, and chat input service. Phase 3 should add a compatibility layer beneath these services rather than mixing platform branches into YAML parsing or command logic.
- `main.yml` is intentionally short, while richer behavior is taught through secondary menus. Compatibility verification should preserve this documentation structure.
- Java 21 and shaded packaging are already part of the validated build contract and must remain true after compatibility changes.

### Integration Points
- `AMenuPlugin.onEnable()`, `reloadPlugin()`, and `onDisable()` are the lifecycle points where a scheduler/platform service can be created, swapped, and cleaned up.
- `MenuService.openMenu()` and `executeActions()` are the main paths that need platform-safe execution guarantees.
- `ChatInputService.onAsyncChat()` and timeout scheduling are the clearest current pressure points for Folia-safe design.
- `plugin.yml`, README command docs, and bundled examples are the outward compatibility surface that must remain stable across platform work.

</code_context>

<specifics>
## Specific Ideas

- The recent `MiniMessage` crash should be treated as compatibility evidence: platform support is not only "runs on Folia", but also "does not assume missing runtime libraries".
- Phase 3 should prefer a narrow compatibility abstraction that wraps scheduling and execution context first, rather than a broad platform framework introduced too early.
- The strongest automated proof for this phase is likely a mix of MockBukkit regressions plus startup/runtime smoke checks on Paper and Folia.

</specifics>

<deferred>
## Deferred Ideas

- Pagination, async data menus, and public skin library expansion - Phase 4
- Condition systems, placeholders-as-logic, and richer bindings - Phase 4
- Any GUI editor or web-based tooling - out of scope

</deferred>

---

*Phase: 03-platform-compatibility-layer*
*Context gathered: 2026-04-02*
