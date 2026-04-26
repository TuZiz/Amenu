# Phase 4: Advanced Menu Features - Context

**Gathered:** 2026-04-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 4 is where AMenu becomes visibly more modern than TRMenu-style menu plugins. The focus is not "more random features"; it is a coherent advanced menu layer built on top of the already-stabilized DSL, runtime interaction model, and platform compatibility baseline.

This phase should cover the first real step beyond static menus:
- pagination and list-oriented views
- async or delayed data-backed rendering
- conditions and visibility/state-driven rendering
- richer bindings and contextual open paths
- stronger bundled showcase menus that demonstrate the engine as a product

This phase does not reopen Phase 1 simplicity goals, Phase 2 runtime lifecycle contracts, or Phase 3 compatibility abstractions. Advanced capability must remain low-config, teachable, and portable across Spigot/Paper/Folia-aware execution.

</domain>

<decisions>
## Implementation Decisions

### Product Direction
- **D-01:** Phase 4 is the first phase where "more modern than TRMenu" becomes a concrete capability claim rather than only a DSL/readability claim.
- **D-02:** The product remains a general Minecraft menu engine. Bundled menus should now act as a feature lab and showcase, not skin-plugin examples.
- **D-03:** Advanced capabilities must still feel shorter and clearer than equivalent TRMenu-style setups; Phase 4 may add concepts, but should resist verbose configuration explosion.

### Advanced Capability Scope
- **D-04:** `ADV-01` should be interpreted as pagination plus data-driven list rendering, not just adding "next page" and "prev page" actions.
- **D-05:** `ADV-02` should be interpreted as conditions and state-driven rendering that affect visibility, display, and action availability without forcing server owners to duplicate entire menus.
- **D-06:** `ADV-03` should be interpreted as richer bindings and contextual openings, such as event-driven opens, item interaction opens, and per-open contextual variables.
- **D-07:** Input flows added during the current runtime work (`CHAT`, `SIGN`, `ANVIL`) are part of the advanced showcase surface and should be integrated into Phase 4 examples rather than treated as isolated demos.

### Architecture Guardrails
- **D-08:** Do not solve pagination, conditions, and bindings by hardcoding slot behavior inside services. Keep configuration parsing, runtime state, and menu rendering separate.
- **D-09:** Async data-backed menus must respect Bukkit/Paper/Folia execution boundaries: data fetch may happen off-thread, but inventory mutation, item construction, and player-facing state transitions must return through the platform-safe runtime path.
- **D-10:** Conditions and rendering logic should prefer declarative config plus small runtime evaluators instead of embedding command/business logic directly into YAML.
- **D-11:** Advanced features should not require every menu to opt into every system. Static menus must remain simple.

### Showcase Strategy
- **D-12:** The bundled showcase should evolve from "example business menus" into a structured feature lab: navigation, inputs, pagination, async rendering, conditions, and bindings should each have a visible landing area.
- **D-13:** `main.yml` can become a feature-hub menu as long as its writing style still demonstrates the canonical low-config DSL.
- **D-14:** The runtime/input lab already introduced during recent compatibility work should be treated as the seed of the showcase strategy, not replaced with another unrelated example set.

### Compatibility And Verification
- **D-15:** Phase 4 planning must assume the current compatibility layer is the foundation, but it must not claim new advanced features are Folia-safe without explicit verification paths.
- **D-16:** The planner should split "feature design" from "verification cost" so that async lists, bindings, and conditions each get specific regression coverage instead of being bundled into one vague test bucket.

### the agent's Discretion
- Exact names for advanced DSL keys and runtime abstractions are open.
- The planner may choose whether Phase 4 is best delivered as two plans (pagination/async first, conditions/bindings second) or further refined slices.
- The bundled showcase may be reorganized if the new layout better teaches the product without bloating the default menu.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and active milestone state
- `.planning/PROJECT.md` - Product identity and overall milestone scope
- `.planning/REQUIREMENTS.md` - `ADV-01`, `ADV-02`, and `ADV-03`
- `.planning/ROADMAP.md` - Phase 4 goal, dependency order, and current phase structure
- `.planning/STATE.md` - Phase 3 human verification remains pending; Phase 4 is the next feature track

### Prior phase outputs that Phase 4 must build on
- `.planning/phases/01-menu-dsl-foundation/01-VERIFICATION.md` - Canonical short DSL and `/amenu` contract
- `.planning/phases/02-runtime-interaction-layer/02-CONTEXT.md` - Runtime semantics and showcase split decisions
- `.planning/phases/02-runtime-interaction-layer/02-VERIFICATION.md` - Verified runtime action and prompt lifecycle behavior
- `.planning/phases/03-platform-compatibility-layer/03-CONTEXT.md` - Compatibility boundary and scheduler abstraction decisions
- `.planning/phases/03-platform-compatibility-layer/03-HUMAN-UAT.md` - Outstanding live verification boundary that must not be overclaimed

### Existing code that defines the Phase 4 starting point
- `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - Current menu, button, prompt, and action models
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - Current DSL parser and inline prompt parsing path
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - Current menu open/click/action execution surface
- `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - Current multi-input prompt runtime (`CHAT`, `SIGN`, `ANVIL`)
- `src/main/kotlin/cc/keer/amenu/platform/PlatformScheduler.kt` - Current platform-safe execution contract

### Bundled showcase and docs
- `src/main/resources/menus/main.yml` - Current feature-hub default menu
- `src/main/resources/menus/history.yml` - Current navigation showcase
- `src/main/resources/menus/runtime.yml` - Current input capture lab
- `src/main/resources/menus/admin.yml` - Current admin/permission showcase
- `README.md` - Current public explanation of plugin positioning and usage

### Existing regression baseline
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt`
- `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt`
- `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt`
- `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt`
- `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MenuRepository` already supports a compact YAML contract and should remain the only place where advanced DSL syntax lands.
- `MenuService` already centralizes navigation and action execution, making it the natural integration point for future pagination, conditional rendering, and richer bindings.
- `ChatInputService` now provides a unified input bridge (`CHAT`, `SIGN`, `ANVIL`) that Phase 4 can elevate into a stronger feature-lab experience.
- `PlatformScheduler` already provides the right foundation for async list rendering that eventually returns to safe inventory updates.

### Established Patterns
- Keep `main.yml` as a canonical entrypoint while richer behaviors live in secondary showcase menus.
- Preserve the separation between parser, runtime service, listeners, and utility compatibility helpers.
- Prefer proving new features through bundled showcase menus plus regression tests rather than docs-only claims.

### Integration Points
- Pagination and async list work will likely require a runtime state layer attached to player/menu sessions.
- Conditions and bindings will likely require a placeholder/context evaluation layer shared by display rendering and action gating.
- Bundled showcase menus should become the product's teaching surface for advanced features, not just compatibility samples.

</code_context>

<specifics>
## Specific Ideas

- The strongest "modernization" path is likely: feature hub -> input lab -> pagination lab -> async list lab -> condition/binding lab.
- The user explicitly wants the plugin itself, not a skin-specific example, so bundled content should keep moving toward engine-showcase menus.
- The recent input work makes it reasonable for Phase 4 to treat prompts as one part of a broader interactive-menu platform instead of a side feature.
- Avoid a design where pagination, async lists, and conditions each invent their own placeholder/state systems.

</specifics>

<deferred>
## Deferred Ideas

- Visual editor or external GUI builder
- Deep business-plugin-specific integrations
- Large-scale web sync or remote data editor tooling
- Any advanced feature that requires dropping the low-config canonical path for ordinary menus

</deferred>

---

*Phase: 04-advanced-menu-features*
*Context gathered: 2026-04-02*
