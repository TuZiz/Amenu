# Phase 5: 动态数据与占位符管线：支持可插拔 page provider、PlaceholderAPI、缓存与刷新策略 - Context

**Gathered:** 2026-04-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 5 upgrades AMenu from "static page entries plus simple placeholder substitution" into a true dynamic menu engine layer. The phase should let menus resolve page entries, text placeholders, and stateful button rendering through pluggable providers with explicit cache and refresh behavior.

This phase is about the engine boundary, not business-specific gameplay content. It should support dynamic menus such as one-time gift claims, state-aware reward cards, and externally-backed list pages without turning ordinary static menus into a heavyweight system.

</domain>

<decisions>
## Implementation Decisions

### Provider Model
- **D-01:** Phase 5 should introduce a registered provider abstraction instead of hardcoding more special cases into `MenuService` or `MenuRepository`.
- **D-02:** The first implementation should combine a unified provider contract with a small set of built-in provider types, so the runtime is extensible without requiring scripting or custom engines on day one.
- **D-03:** Existing static page definitions remain valid. Static menus should continue to work without opting into any provider system.

### Placeholder Pipeline
- **D-04:** Internal placeholders such as `{player}` remain the canonical built-in style for AMenu configs.
- **D-05:** PlaceholderAPI should be treated as a soft-dependency bridge, not a hard requirement. If PlaceholderAPI is installed, external `%placeholder%` variables may resolve through the same pipeline; if it is absent, menus must degrade safely instead of failing to load.
- **D-06:** Text display, lore, action arguments, provider parameters, and button-state rendering should all resolve through one shared placeholder pipeline instead of each subsystem doing ad hoc string replacement.

### Cache And Refresh Strategy
- **D-07:** Dynamic data should default to cache keys scoped by player plus menu and region/provider identity, so data is isolated per viewer and menu surface.
- **D-08:** Phase 5 should support explicit cache invalidation through menu refresh behavior, rather than relying only on time-based expiration.
- **D-09:** Provider-backed content should support TTL-style caching as a first-class behavior, so frequently-opened menus do not re-fetch everything on every render.
- **D-10:** The phase should support periodic re-evaluation for stateful surfaces, so menus can update button appearance or availability while a player keeps the menu open.

### Stateful Button Rendering
- **D-11:** Phase 5 should support button-level state switching based on permissions, placeholders, or provider-backed state without forcing server owners to duplicate whole menus.
- **D-12:** The engine should support the practical scenario the user referenced: a gift/reward button that can hide after claim, switch icon/name/lore based on permission state, and re-evaluate on an update interval while still using ordinary menu actions.
- **D-13:** AMenu does not need to lock itself to one-for-one TRMenu syntax compatibility, but Phase 5 should deliberately cover the behavior class represented by that example: conditional button skins, timed updates, and external placeholder or command variables.

### Failure And Degradation Behavior
- **D-14:** Missing PlaceholderAPI, provider failures, empty provider results, and cache-expired reloads must produce safe player-facing fallbacks instead of broken menus or platform-unsafe behavior.
- **D-15:** Dynamic data fetch may happen off-thread, but inventory mutation, item construction, and visible menu refresh must return through the platform-safe runtime path already established for Bukkit/Paper/Folia.
- **D-16:** Loading, empty, and error states should remain explicit engine concepts for provider-backed regions or buttons rather than being left to silent null rendering.

### Architecture Guardrails
- **D-17:** Keep parser, provider registry, runtime evaluation, and platform-safe refresh boundaries separate. Do not solve Phase 5 by scattering PlaceholderAPI calls and cache maps across listeners and utility classes.
- **D-18:** Static menus and low-config showcase content must remain teachable. Phase 5 may add dynamic capabilities, but it should not force every menu file to declare provider metadata, cache blocks, or external integration settings.

### the agent's Discretion
- Exact DSL key names are open, including whether timed re-evaluation uses `update`, `refresh-interval`, or another canonical name.
- The planner may decide whether dynamic button states reuse and extend the existing `states` model or introduce a more expressive companion structure.
- The planner may choose the smallest stable built-in provider set as long as it proves the abstraction and covers dynamic pages plus stateful button rendering.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and active milestone state
- `.planning/PROJECT.md` - Product identity, low-config promise, and scope discipline
- `.planning/REQUIREMENTS.md` - Existing validated requirements and the project boundary Phase 5 must build on
- `.planning/ROADMAP.md` - Phase 5 goal, dependency chain, and milestone structure
- `.planning/STATE.md` - Current milestone position, pending Phase 3 live UAT, and recent Phase 4 outcomes

### Prior phase outputs Phase 5 must build on
- `.planning/phases/03-platform-compatibility-layer/03-CONTEXT.md` - Thread and platform safety constraints that dynamic refresh must honor
- `.planning/phases/04-advanced-menu-features/04-CONTEXT.md` - Locked decisions around pagination, async data-backed rendering, conditions, bindings, and showcase direction
- `.planning/phases/04-advanced-menu-features/04-RESEARCH.md` - Technical research baseline for pagination and advanced rendering

### Existing engine code that defines the Phase 5 starting point
- `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - Current menu, page, binding, condition, and action models
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - Current DSL parser, page region parsing, state parsing, and condition parsing boundaries
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - Current menu state, page region rendering, refresh behavior, and placeholder merge path
- `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - Existing placeholder-aware prompt flow that should converge on the same resolution pipeline
- `src/main/kotlin/cc/keer/amenu/gui/MenuBindingListener.kt` - Existing contextual placeholder injection path from item bindings
- `src/main/kotlin/cc/keer/amenu/platform/PlatformScheduler.kt` - Platform-safe execution contract for any async provider refresh flow

### Public examples and regression baseline
- `src/main/resources/menus/showcase.yml` - Current feature-lab menu positioning
- `src/main/resources/menus/history.yml` - Current page/list showcase surface
- `src/main/resources/menus/runtime.yml` - Existing dynamic input showcase that already depends on placeholder substitution
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - DSL regression baseline
- `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt` - Runtime action and refresh regression baseline
- `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt` - Bundled menu verification baseline
- `src/test/kotlin/cc/keer/amenu/service/MenuServiceCompatibilityTest.kt` - Service compatibility boundary baseline

### External specs
- No external specs are required for this phase yet; requirements are fully captured in repository documents and decisions above.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MenuService` already owns viewer-scoped menu state, page region state, and refresh entrypoints. It is the natural runtime integration point for provider-backed rendering and cache invalidation.
- `MenuRepository` already centralizes the YAML parser and should remain the single place where new dynamic DSL keys are interpreted.
- `MenuBindingListener` already injects open-time contextual placeholders and can serve as one of the input sources into the shared placeholder pipeline.
- `ChatInputService` already carries placeholder maps through prompt lifecycle callbacks, so its rendering path should align with the same canonical placeholder resolver.

### Established Patterns
- Keep static and advanced features coexisting: ordinary menus remain simple while advanced menus opt into richer behavior.
- Preserve separation between parser, runtime service, listeners, and platform helpers.
- Respect the platform-safe execution rule already established in Phase 3: data fetch may be async, but GUI mutation and player-visible transitions must re-enter the safe runtime path.

### Integration Points
- `PageRegionDefinition` and `RegionViewState` are the clearest starting point for provider-backed region data and region-scoped cache metadata.
- Placeholder resolution is currently duplicated across settings, menu runtime, chat input, and bindings; Phase 5 should consolidate this into a shared resolver.
- Existing button `states` and condition evaluation are the most natural place to extend toward timed, stateful button rendering for reward/gift scenarios.

</code_context>

<specifics>
## Specific Ideas

- The user explicitly wants support for a menu item similar to a reward/gift card that:
  - can run ordinary close/delay/command/console action chains,
  - can switch display and behavior by permission or state,
  - can disappear after claim,
  - can re-evaluate while the menu remains open,
  - can use external placeholder variables like `%player_name%`.
- The target capability matters more than one exact borrowed syntax. The engine should support the behavior class even if the final canonical DSL differs from the pasted example.
- The strongest product interpretation for Phase 5 is "dynamic menu engine primitives" rather than "one more showcase gimmick."

</specifics>

<deferred>
## Deferred Ideas

- Full one-to-one compatibility with another plugin's DSL syntax
- Scriptable provider engines or user-authored scripts in this phase
- Deep coupling to one business plugin API or one reward/economy stack
- Web editors or remote visual configuration tooling

</deferred>

---

*Phase: 05-page-provider-placeholderapi*
*Context gathered: 2026-04-03*
