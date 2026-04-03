---
phase: 05-page-provider-placeholderapi
plan: 02
subsystem: runtime
tags: [bukkit, kotlin, caffeine, placeholderapi, provider-cache]
requires:
  - phase: 05-page-provider-placeholderapi
    provides: shared placeholder pipeline and provider/cache/update DSL metadata
provides:
  - registered page provider runtime for MenuService
  - viewer/menu/surface/provider-scoped cache with TTL and targeted invalidation
  - placeholder-backed button state rerender through platform-safe handoff
affects: [05-03, dynamic-refresh, provider-runtime]
tech-stack:
  added: [com.github.ben-manes.caffeine:caffeine:3.2.3]
  patterns: [provider-registry, stale-last-good cache fallback, pre-render dynamic surface sync]
key-files:
  created:
    - src/main/kotlin/cc/keer/amenu/service/provider/MenuDataProvider.kt
    - src/main/kotlin/cc/keer/amenu/service/provider/MenuProviderRegistry.kt
    - src/main/kotlin/cc/keer/amenu/service/provider/ProviderCache.kt
    - src/main/kotlin/cc/keer/amenu/service/provider/ProviderRequest.kt
    - src/main/kotlin/cc/keer/amenu/service/provider/ProviderResult.kt
    - src/main/kotlin/cc/keer/amenu/service/provider/builtin/EntriesPageProvider.kt
    - src/main/kotlin/cc/keer/amenu/service/provider/builtin/PlaceholderStateProvider.kt
    - src/test/kotlin/cc/keer/amenu/service/MenuDynamicProviderServiceTest.kt
  modified:
    - pom.xml
    - src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt
    - src/main/kotlin/cc/keer/amenu/service/MenuService.kt
    - src/test/kotlin/cc/keer/amenu/service/MenuServiceCompatibilityTest.kt
key-decisions:
  - "Use a central ProviderCache keyed by viewerId + menuId + surfaceId + providerType so refresh can invalidate one surface without cross-player leakage."
  - "Register built-in entries and placeholder-state providers, with legacy-static aliases routed through entries to preserve Phase 4 page behavior."
  - "Pre-sync dynamic page surfaces before slot rendering so provider-backed placeholders can drive button state selection in the same render pass."
patterns-established:
  - "Provider runtime pattern: MenuService builds ProviderRequest, MenuProviderRegistry resolves implementation, and ProviderCache owns TTL plus stale-last-good fallback."
  - "Safe rerender pattern: provider completion may happen off-thread, but visible menu refresh always returns through PlatformScheduler.executeFor before renderOpenMenuIfCurrent."
requirements-completed: [P5-01, P5-05]
duration: 24min
completed: 2026-04-03
---

# Phase 05 Plan 02: Provider Runtime Summary

**Provider-backed page regions now run through a registry and Caffeine-backed cache, with stale-safe fallback and scheduler-safe rerender for dynamic buttons and pages**

## Performance

- **Duration:** 24 min
- **Started:** 2026-04-03T09:31:00Z
- **Completed:** 2026-04-03T09:55:12Z
- **Tasks:** 2
- **Files modified:** 12

## Accomplishments
- Added RED coverage for viewer/menu/surface cache scoping, targeted refresh invalidation, resolved provider params, stale-last-good fallback, and placeholder-driven button state updates.
- Implemented provider runtime primitives under `service/provider`, including built-in `entries` and `placeholder-state` providers plus a Caffeine-backed cache facade.
- Reworked `MenuService` to resolve provider params through `PlaceholderPipeline`, preload dynamic surfaces before render, and rerender active menus only through `PlatformScheduler.executeFor`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Write RED tests for provider cache scope, invalidation, and scheduler-safe rerender** - `bbdc180` (test)
2. **Task 2: Implement the provider registry, Caffeine cache facade, and safe provider-backed page runtime** - `575f715` (feat)

## Files Created/Modified
- `src/main/kotlin/cc/keer/amenu/service/provider/MenuDataProvider.kt` - common provider contract returning async `ProviderResult`.
- `src/main/kotlin/cc/keer/amenu/service/provider/MenuProviderRegistry.kt` - provider registration plus built-in aliases for `entries` and `placeholder-state`.
- `src/main/kotlin/cc/keer/amenu/service/provider/ProviderCache.kt` - viewer/menu/surface/provider cache facade with TTL, stale-last-good retention, and targeted invalidation.
- `src/main/kotlin/cc/keer/amenu/service/provider/ProviderRequest.kt` - runtime request model carrying resolved params and region metadata.
- `src/main/kotlin/cc/keer/amenu/service/provider/ProviderResult.kt` - explicit success, empty, and error result states.
- `src/main/kotlin/cc/keer/amenu/service/provider/builtin/EntriesPageProvider.kt` - compatibility provider for existing page entries and async-delay behavior.
- `src/main/kotlin/cc/keer/amenu/service/provider/builtin/PlaceholderStateProvider.kt` - provider that projects resolved params into runtime placeholder state.
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - page rendering, invalidation, fallback, and safe rerender now flow through provider runtime seams.
- `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt` - plugin bootstrap now owns the provider registry and cache.
- `src/test/kotlin/cc/keer/amenu/service/MenuDynamicProviderServiceTest.kt` - regression suite for cache scoping, fallback, and reward/gift-style button state changes.
- `src/test/kotlin/cc/keer/amenu/service/MenuServiceCompatibilityTest.kt` - compatibility assertion that async provider completion rerenders through scheduler handoff.
- `pom.xml` - adds Caffeine 3.2.3 for cache storage.

## Decisions Made

- Use provider aliases instead of parser branching so legacy static pages and new provider-backed pages share one execution seam.
- Keep stale-last-good snapshots in `ProviderCache` rather than scattering timestamps or fallback state inside `MenuService`.
- Sync dynamic page surfaces before per-slot render so placeholder-state provider outputs can affect button conditions, icon/name/lore rendering, and reward-style swaps without duplicating menus.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- PowerShell parsed the first Maven verify command incorrectly because the `-Dtest=` list was not quoted; rerunning with a quoted property fixed verification.
- MC Plugin Neuron source indexing was unavailable for this repo snapshot, so repository docs were used via MCP while file-level source reads fell back to direct workspace reads.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `05-03` can now build timed reevaluation on top of a stable provider registry and per-surface cache invalidation model.
- PlaceholderAPI live smoke and timed refresh cleanup are still outstanding for the next plan and remain the main verification boundary before Phase 5 closeout.

## Self-Check

PASSED

- Found `.planning/phases/05-page-provider-placeholderapi/05-02-SUMMARY.md`
- Found task commits `bbdc180` and `575f715` in `git log --oneline --all`

---
*Phase: 05-page-provider-placeholderapi*
*Completed: 2026-04-03*
