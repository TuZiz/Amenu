# Phase 5: 动态数据与占位符管线 - Research

**Researched:** 2026-04-03
**Domain:** Bukkit/Paper/Folia-safe dynamic menu providers, placeholder resolution, and cache-aware refresh
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

### Claude's Discretion
- Exact DSL key names are open, including whether timed re-evaluation uses `update`, `refresh-interval`, or another canonical name.
- The planner may decide whether dynamic button states reuse and extend the existing `states` model or introduce a more expressive companion structure.
- The planner may choose the smallest stable built-in provider set as long as it proves the abstraction and covers dynamic pages plus stateful button rendering.

### Deferred Ideas (OUT OF SCOPE)
- Full one-to-one compatibility with another plugin's DSL syntax
- Scriptable provider engines or user-authored scripts in this phase
- Deep coupling to one business plugin API or one reward/economy stack
- Web editors or remote visual configuration tooling
</user_constraints>

## Summary

Phase 5 should be planned as an engine-layer refactor plus a small capability expansion, not as a single feature bolt-on. The current codebase already has the right seams: `MenuRepository` owns DSL parsing, `MenuService` owns viewer/menu state and safe rerender entrypoints, `PlatformScheduler` owns Paper/Folia handoff, and Phase 4 already proved async page loading, button states, and placeholder-conditioned rendering. The gap is that all placeholder work is still duplicated and all dynamic data is still tied to `PageRegionDefinition.entries` plus `async-delay`.

The strongest route is a staged runtime pipeline:
1. Parse provider and refresh metadata in `MenuRepository`.
2. Resolve data through a registered provider registry and viewer-scoped cache facade.
3. Merge placeholders through one ordered resolver chain.
4. Materialize buttons/items only on the platform-safe path.
5. Re-render active menus in place, instead of reopening whole menus for every timed update.

The external-doc constraints matter. PlaceholderAPI's official guidance is `provided` plus `softdepend`, and its new Adventure-component parsing path is Paper-only; AMenu still supports Spigot/Paper/Folia, so the canonical bridge should remain string-first, then pass the rendered string into Adventure. Paper's Folia docs also remain explicit: `folia-supported: true` is necessary but not sufficient, and plugin logic must choose the correct scheduler class for global, region, entity, and async work.

**Primary recommendation:** Plan Phase 5 around three new internal subsystems only: `ProviderRegistry`, `PlaceholderPipeline`, and `DynamicRefreshController`, and keep all Bukkit/Paper/Folia mutation behind the existing `PlatformScheduler`.

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Kotlin stdlib | 2.0.21 | Existing implementation language/runtime | Already pinned in `pom.xml`; changing language/runtime adds no value in this phase |
| Paper API | 1.21.11-R0.1-SNAPSHOT | Bukkit/Paper/Folia-facing plugin API baseline | Already pinned; Phase 3 scheduler abstraction and current code target this API |
| Adventure BOM | 4.26.1 | Text/component formatting already used by AMenu | Existing project text stack; keep placeholder resolution upstream of Adventure rendering |
| `PlatformScheduler` (internal) | current project | Safe global/player handoff across Bukkit/Paper/Folia | Already implemented and covered by compatibility tests; Phase 5 should extend it, not bypass it |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| PlaceholderAPI | 2.12.2 | Optional external `%placeholder%` bridge | Use only as a `provided` + `softdepend` dependency, and only inside the shared placeholder pipeline |
| Caffeine | 3.2.3 | TTL/invalidation cache for provider results | Use for provider result caching and refresh metadata; do not use it for active GUI state or task ownership |
| MockBukkit | 4.108.0 | Fast automated regression for parser/runtime/service behavior | Use for Phase 5 unit/service tests; keep live Paper/Folia placeholder integration as smoke/UAT |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Caffeine cache facade | hand-rolled `MutableMap` + timestamp/task cleanup | Worse invalidation, expiry, and testability; more lifecycle bugs |
| String-first PlaceholderAPI bridge | `PAPIComponents` | `PAPIComponents` is Paper-only; AMenu still targets Spigot/Paper/Folia |
| `plugin.yml` soft dependency | `paper-plugin.yml` dependency graph | `paper-plugin.yml` is fine for Paper-only plugins, but AMenu still needs classic `plugin.yml` for Spigot compatibility |
| In-place rerender of active view | reopen menu on every timed refresh | Reopen path risks history churn, state reset, and excess provider reloads |

**Installation:**
```xml
<repositories>
  <repository>
    <id>placeholderapi</id>
    <url>https://repo.helpch.at/releases/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>me.clip</groupId>
    <artifactId>placeholderapi</artifactId>
    <version>2.12.2</version>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.2.3</version>
  </dependency>
</dependencies>
```

**Version verification:** `PlaceholderAPI 2.12.2` was verified on the official GitHub releases page as the latest release on 2026-02-08. `Caffeine 3.2.3` was verified from Maven Central directory metadata dated 2025-10-28. Core project versions were verified from the current `pom.xml`.

## Architecture Patterns

### Recommended Project Structure
```text
src/main/kotlin/cc/keer/amenu/
- config/             # DSL models + parser for provider/cache/refresh keys
- placeholder/        # ordered placeholder stages + PlaceholderAPI bridge
- provider/           # provider contract, registry, cache facade, built-ins
- runtime/            # dynamic view state, refresh plans, surface status
- service/            # MenuService orchestration + refresh controller entrypoints
- platform/           # existing safe scheduler boundary
```

### Pattern 1: Shared Placeholder Pipeline
**What:** A single ordered resolver chain for all text-like surfaces: title, item name, lore, actions, provider params, and button states.
**When to use:** Any time the engine renders or evaluates text.
**Recommended order:** `default placeholders -> open/binding/input context -> provider entry placeholders -> internal AMenu computed page placeholders -> optional PlaceholderAPI bridge`.
**Why:** Current replacement logic is duplicated in `MenuService`, `ChatInputService`, and `MenuBindingListener`, which guarantees drift if Phase 5 adds provider parameters and timed button reevaluation.
**Example:**
```kotlin
interface PlaceholderStage {
    fun apply(player: Player, text: String, context: PlaceholderContext): String
}

class PlaceholderPipeline(
    private val stages: List<PlaceholderStage>,
) {
    fun resolve(player: Player, text: String, context: PlaceholderContext): String {
        return stages.fold(text) { current, stage -> stage.apply(player, current, context) }
    }
}
```

### Pattern 2: Provider Registry + Immutable Provider Result
**What:** A small registry keyed by provider id/type that returns explicit result objects instead of mutating menu state directly.
**When to use:** Any dynamic region/button data source, including built-in list/state providers.
**Contract shape:** `ProviderRequest -> ProviderResult.Success/Loading/Empty/Error`.
**Why:** Current `PageRegionDefinition` and `RegionViewState` already separate definition from viewer state; Phase 5 should preserve that separation and extend it to provider-backed data.
**Example:**
```kotlin
sealed interface ProviderResult {
    data class Success(
        val entries: List<PageEntryDefinition>,
        val placeholders: Map<String, String> = emptyMap(),
        val ttlTicks: Long? = null,
    ) : ProviderResult
    data object Empty : ProviderResult
    data class Error(val messageKey: String) : ProviderResult
}
```

### Pattern 3: Viewer-Scoped Cache Facade
**What:** A cache keyed by `viewer + menu + surface + provider key`, owned by one service, not by individual providers.
**When to use:** Provider-backed list regions, stateful buttons, or expensive placeholder subtrees.
**Why:** Phase decisions explicitly require player/menu/provider scoping and both TTL and explicit invalidation. Cache ownership must stay centralized so close/reload/refresh can invalidate correctly.
**Example:**
```kotlin
data class DynamicCacheKey(
    val viewerId: UUID,
    val menuId: String,
    val surfaceId: String,
    val providerKey: String,
)
```

### Pattern 4: In-Place Active View Refresh
**What:** Periodic reevaluation updates the currently open inventory in place by rerendering the active view, not by re-opening menus.
**When to use:** Timed button state changes, provider-backed regions that refresh while a player keeps the menu open, or explicit page/button refresh commands.
**Why:** `MenuService.renderOpenMenuIfCurrent(...)` already exists and is the correct seam. Reopening the menu for periodic refresh would destroy history semantics and can invalidate cached state too aggressively.

### Pattern 5: Explicit Surface States
**What:** Treat `LOADING`, `READY`, `EMPTY`, and `ERROR` as first-class UI states for dynamic regions and dynamic buttons.
**When to use:** Every provider-backed render surface.
**Why:** Phase 5 decisions explicitly reject silent null rendering. Page regions already have `loadingIcon`/`emptyIcon`; extend the same model to provider errors and stateful button fallbacks.

### Anti-Patterns to Avoid
- **Scattered placeholder replacement:** Do not keep adding `current.replace("{key}", value)` clones in more services.
- **Provider logic inside parser or listeners:** Parse metadata in `MenuRepository`; execute providers in provider services; refresh through `MenuService`.
- **Async Bukkit access:** Do not call `Inventory`, `ItemMeta`, `Player`, or PlaceholderAPI expansions from arbitrary async threads.
- **Whole-menu reopen timers:** Do not implement timed button updates by repeatedly calling `openMenu(...)`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| TTL cache with invalidation | `MutableMap` + timestamps + manual sweep loop | Caffeine-backed `ProviderCache` facade | Handles expiry, invalidation, and size policy cleanly; easier to test |
| Placeholder resolution | more per-class string replacement helpers | `PlaceholderPipeline` with ordered stages | Keeps titles, lore, actions, params, and states consistent |
| Optional PlaceholderAPI detection | reflection-only API probing | `plugin.yml softdepend` + runtime plugin-enabled check | Officially supported load order and safer absence behavior |
| Repeating refresh ownership | ad hoc `BukkitTask` fields sprinkled across states | one `DynamicRefreshController` owning task handles | Prevents orphan tasks on close/reload and centralizes cleanup |
| Provider result state | null/empty-list ambiguity | explicit `Success/Empty/Error` result model | Makes loading, empty, and error icons/actions deterministic |

**Key insight:** Phase 5 looks like "just placeholders and refresh," but it is really a lifecycle problem. The failure modes are not string parsing bugs; they are stale caches, orphaned tasks, viewer-state leaks, and async thread violations.

## Common Pitfalls

### Pitfall 1: Calling PlaceholderAPI on the wrong execution path
**What goes wrong:** Provider fetch is async, then code calls PlaceholderAPI or builds `ItemStack`/`Component` before returning to the player-safe scheduler.
**Why it happens:** "String work" looks harmless, but expansions may still touch Bukkit/plugin state, and final item rendering definitely does.
**How to avoid:** Treat PlaceholderAPI bridge and item construction as part of the safe render path. Fetch remote/business data async, then resolve placeholders and rerender via `PlatformScheduler.executeFor(...)`.
**Warning signs:** Random Folia/Paper thread violations, expansion-specific errors, or menus that work on Paper but fail unpredictably on Folia.

### Pitfall 2: Using `PAPIComponents` as the canonical bridge
**What goes wrong:** Component-level PlaceholderAPI parsing is wired into the core render path and breaks Spigot compatibility.
**Why it happens:** PlaceholderAPI 2.12.x added Adventure component parsing, but official docs state that path is Paper-only.
**How to avoid:** Keep AMenu's canonical bridge string-first. Resolve `%...%` into strings, then pass the result into Adventure formatting.
**Warning signs:** Startup or linkage failures on Spigot, or render path branching that duplicates all text rendering.

### Pitfall 3: Reopening menus for timed refresh
**What goes wrong:** Button/state refresh calls `openMenu(...)` repeatedly, resetting menu history, page position, and cache semantics.
**Why it happens:** The existing `MenuAction.Refresh` already reopens the menu, so it looks reusable for every refresh case.
**How to avoid:** Split "explicit reopen-style refresh" from "periodic in-place rerender." Reuse `renderOpenMenuIfCurrent(...)` for the second path.
**Warning signs:** Players lose their current page, history stack behaves strangely, or provider data reloads too often.

### Pitfall 4: Cache keys that are too broad
**What goes wrong:** One player sees another player's provider result or stale button state, or one menu invalidation flushes unrelated data.
**Why it happens:** Cache is keyed only by provider id or region id.
**How to avoid:** Key by `viewerId + menuId + surfaceId + provider identity`, exactly as Phase decisions require.
**Warning signs:** Cross-player leakage, wrong reward state after switching menus, or refresh commands invalidating unrelated surfaces.

### Pitfall 5: Forgetting task/cache cleanup on close, reload, or menu switch
**What goes wrong:** Repeating reevaluation keeps running after a player closes the inventory or the plugin reloads.
**Why it happens:** Refresh handles are attached to region/button state without one owning controller.
**How to avoid:** Centralize active refresh handles per open menu view and cancel them on close, reload, quit, and menu replacement.
**Warning signs:** Console noise after closing menus, memory growth, or updates targeting offline players.

### Pitfall 6: Trusting every PlaceholderAPI expansion to be Folia-safe
**What goes wrong:** Core PlaceholderAPI works, but an expansion still causes scheduler issues or slow calls.
**Why it happens:** PlaceholderAPI 2.11.7 added Folia support for the plugin core, but the release notes explicitly stop short of guaranteeing every expansion.
**How to avoid:** Keep PlaceholderAPI bridge optional, guarded, and non-fatal. Prefer cached/stale-last-good fallback over hard failure when an expansion errors.
**Warning signs:** Only some placeholders break, usually those backed by other plugins or remote systems.

## Code Examples

Verified patterns from official or repository sources:

### Optional PlaceholderAPI dependency
```yaml
# Source: https://wiki.placeholderapi.com/developers/using-placeholderapi/
softdepend: ["PlaceholderAPI"]
```

### PlaceholderAPI string parsing
```java
// Source: https://wiki.placeholderapi.com/developers/using-placeholderapi/
String rendered = PlaceholderAPI.setPlaceholders(player, rawText);
```

### Folia-safe location-bound work
```java
// Source: https://docs.papermc.io/paper/dev/folia-support/
RegionScheduler scheduler = server.getRegionScheduler();
scheduler.execute(plugin, locationToChange, () -> {
    locationToChange.getBlock().setType(Material.BEEHIVE);
});
```

### Cache entry with TTL
```java
// Source: https://github.com/ben-manes/caffeine/wiki/Eviction
LoadingCache<Key, Value> cache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build(this::loadValue);
```

### Existing in-place rerender seam
```kotlin
// Source: src/main/kotlin/cc/keer/amenu/service/MenuService.kt
private fun renderOpenMenuIfCurrent(player: Player, menuId: String) {
    val holder = player.openInventory.topInventory.holder as? MenuHolder ?: return
    if (holder.menuId != menuId) return
    val menu = repository.menu(menuId) ?: return
    val state = stateFor(player, menu.id)
    renderMenu(player, menu, player.openInventory.topInventory, state)
    player.updateInventory()
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Ad hoc string replacement in multiple services | Staged placeholder pipeline with optional external bridge | Needed now for Phase 5 | One render contract for text, actions, provider params, and states |
| Static `PageRegionDefinition.entries` plus synthetic `async-delay` load | Registered providers returning explicit results | Needed now for Phase 5 | Supports real dynamic pages and stateful buttons without parser/service sprawl |
| Full menu reopen for refresh | In-place active-view rerender plus explicit invalidation | Needed now for Phase 5 | Preserves history/page state and reduces redundant provider calls |
| No external placeholder bridge | PlaceholderAPI soft bridge via `setPlaceholders(...)` | PlaceholderAPI docs long-standing; apply now | Lets AMenu stay self-contained while integrating server ecosystem placeholders |
| PAPI string-only world | PAPI 2.12.x added component parsing on Paper-only servers | PlaceholderAPI 2.12.0 / 2026-02-03 | Confirms AMenu should not make component parsing its cross-platform core |
| PlaceholderAPI core lacking Folia support | PlaceholderAPI 2.11.7 adds Folia support, with expansion caveat | 2025-11-03 | Makes soft bridge viable on Folia, but still demands guarded/non-fatal behavior |

**Deprecated/outdated:**
- Scattering placeholder logic across `MenuService`, `ChatInputService`, and `MenuBindingListener`: Phase 5 should consolidate this.
- Using `async-delay` as the only dynamic-data mechanism: keep it only as a compatibility shim or built-in delayed provider behavior.

## Open Questions

1. **How many built-in provider types should Phase 5 ship initially?**
   - What we know: The phase only needs a "small set" that proves the abstraction and covers dynamic pages plus stateful button rendering.
   - What's unclear: Whether one provider contract with 2-3 built-ins is enough, or whether page and button-state providers should be separate contracts.
   - Recommendation: Plan one core provider contract, then ship two built-ins first: `static/delayed` compatibility provider and `placeholder/state` provider.

2. **Should button reevaluation reuse `states` or add a companion dynamic-state block?**
   - What we know: Existing `states` already encode conditional icon/action/permission overrides.
   - What's unclear: Whether refresh metadata belongs on the button, the state block, or a new dynamic wrapper.
   - Recommendation: Keep `states` as the rendering model; add refresh/provider metadata adjacent to the button definition rather than inventing a second state language.

3. **Should cache implementation be dependency-backed or internal-only in Phase 5?**
   - What we know: TTL + explicit invalidation + scoped keys are mandatory.
   - What's unclear: Whether the project wants a new dependency for this milestone.
   - Recommendation: Prefer Caffeine. If dependency growth is rejected, planner must still require one cache facade class and not allow inline maps.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Build/test runtime | yes | Global: 25.0.2; repo-local: 21.0.10 | Use `.tools/jdk21/bin/java.exe` for all Maven validation |
| Maven | Build/test runner | yes | 3.9.12 | - |
| Paper/Folia server jars | Smoke/manual compatibility checks | yes | Paper/Folia 1.20.6 smoke assets; `libs/folia-1.21.11-14.jar` | MockBukkit for automation; live integration still manual |
| PlaceholderAPI plugin jar | Presence-path live integration | no | - | Cover absence-path in tests; perform presence-path smoke manually after install |

**Missing dependencies with no fallback:**
- None for planning or automated unit/service validation.

**Missing dependencies with fallback:**
- PlaceholderAPI live plugin installation: planner should treat "plugin present" verification as manual smoke, while keeping automated tests focused on soft-dependency absence and bridge isolation.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 6.0.3 + MockBukkit 4.108.0 |
| Config file | none - Maven Surefire + Kotlin Maven plugin in `pom.xml` |
| Quick run command | `set JAVA_HOME=D:\codex\Amenu\.tools\jdk21 && set PATH=%JAVA_HOME%\bin;%PATH% && mvn -q -Dtest=MenuRepositoryDslTest,MenuRuntimeActionTest,MenuServiceCompatibilityTest test` |
| Full suite command | `set JAVA_HOME=D:\codex\Amenu\.tools\jdk21 && set PATH=%JAVA_HOME%\bin;%PATH% && mvn test` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| P5-01 | Provider-backed page regions load, cache, invalidate, and refresh safely | unit/service | `mvn -q -Dtest=MenuDynamicProviderServiceTest test` | no - Wave 0 |
| P5-02 | Shared placeholder pipeline resolves internal placeholders and degrades safely when PlaceholderAPI is absent | unit | `mvn -q -Dtest=PlaceholderPipelineTest test` | no - Wave 0 |
| P5-03 | Button states can reevaluate on an interval without reopening the menu | service | `mvn -q -Dtest=MenuDynamicRefreshTest test` | no - Wave 0 |
| P5-04 | DSL parses provider/cache/refresh metadata without breaking static menus | unit | `mvn -q -Dtest=MenuRepositoryDslTest test` | yes |
| P5-05 | Async provider completion rerenders through platform-safe handoff | compatibility/service | `mvn -q -Dtest=MenuServiceCompatibilityTest test` | yes |
| P5-06 | PlaceholderAPI presence path does not break Spigot/Paper/Folia support guarantees | manual smoke | `manual-only` | no - manual |

### Sampling Rate
- **Per task commit:** `mvn -q -Dtest=MenuRepositoryDslTest,MenuRuntimeActionTest,MenuServiceCompatibilityTest test`
- **Per wave merge:** `mvn test`
- **Phase gate:** Full suite green plus one manual PlaceholderAPI-enabled smoke on Paper or Folia before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/test/kotlin/cc/keer/amenu/service/MenuDynamicProviderServiceTest.kt` - provider cache, invalidation, result-state transitions
- [ ] `src/test/kotlin/cc/keer/amenu/service/PlaceholderPipelineTest.kt` - internal placeholders, soft bridge absent/present shim, parameter ordering
- [ ] `src/test/kotlin/cc/keer/amenu/service/MenuDynamicRefreshTest.kt` - repeating reevaluation, close/reload cleanup, no reopen churn
- [ ] Manual smoke recipe with PlaceholderAPI installed - verify `%player_name%` path, cache invalidation, and one timed button update on Paper or Folia

## Sources

### Primary (HIGH confidence)
- Repository source: `pom.xml` - project-pinned Kotlin/Paper/Adventure/JUnit/MockBukkit versions
- Repository source: `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - current menu state, async region load, and in-place rerender seam
- Repository source: `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - current DSL parsing seams for pages, states, bindings, and conditions
- Repository source: `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - current data model boundaries for buttons, states, and page regions
- Repository source: `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - duplicate placeholder rendering path that should be unified
- Repository source: `src/main/kotlin/cc/keer/amenu/gui/MenuBindingListener.kt` - duplicate binding placeholder path that should be unified
- Repository source: `src/main/kotlin/cc/keer/amenu/platform/PlatformScheduler.kt` and platform implementations - current Bukkit/Folia-safe execution contract
- Official PlaceholderAPI docs: https://wiki.placeholderapi.com/developers/using-placeholderapi/ - dependency model, `softdepend`, `setPlaceholders`, and Paper-only component parsing note
- Official Paper docs: https://docs.papermc.io/paper/dev/plugin-yml/ - `plugin.yml` dependency semantics
- Official Paper docs: https://docs.papermc.io/paper/dev/folia-support/ - `folia-supported` requirement and scheduler model
- Official PlaceholderAPI releases: https://github.com/PlaceholderAPI/PlaceholderAPI/releases - latest version `2.12.2`, component parsing in `2.12.1`, Folia support in `2.11.7`
- Official Caffeine docs: https://github.com/ben-manes/caffeine/wiki/Eviction - eviction/TTL guidance
- Maven Central metadata: https://repo.maven.apache.org/maven2/com/github/ben-manes/caffeine/caffeine/ - latest Caffeine version `3.2.3`

### Secondary (MEDIUM confidence)
- MC Plugin Neuron `async-pagination` topic kit - reinforces async pagination/list separation and state layering
- MC Plugin Neuron `render-stages` topic kit - reinforces stage-based placeholder/render architecture

### Tertiary (LOW confidence)
- None. Unverified ecosystem claims were intentionally excluded.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - core stack is verified from the current repository and official PlaceholderAPI/Paper/Caffeine sources
- Architecture: HIGH - recommended structure matches existing repo seams and official scheduler/dependency constraints
- Pitfalls: HIGH - derived from current code duplication plus official Folia/PlaceholderAPI behavior notes

**Research date:** 2026-04-03
**Valid until:** 2026-05-03
