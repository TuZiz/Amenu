---
phase: 05-page-provider-placeholderapi
plan: 01
subsystem: ui
tags: [kotlin, bukkit, placeholderapi, yaml, tdd]
requires:
  - phase: 04-advanced-menu-features
    provides: pagination regions, button states, and binding-aware menu runtime
provides:
  - shared placeholder pipeline with internal-first rendering and optional PlaceholderAPI bridge
  - explicit provider/cache/update/surface contracts for page-region DSL parsing
  - RED/GREEN regression coverage for provider metadata parsing and state rendering
affects: [05-02-PLAN, 05-03-PLAN, provider runtime, refresh lifecycle]
tech-stack:
  added: [PlaceholderAPI 2.12.2 provided dependency]
  patterns: [shared placeholder pipeline, parser-runtime separation, soft dependency bridge]
key-files:
  created: [src/main/kotlin/cc/keer/amenu/service/PlaceholderPipeline.kt, src/main/kotlin/cc/keer/amenu/service/PlaceholderApiBridge.kt, src/test/kotlin/cc/keer/amenu/service/PlaceholderPipelineTest.kt]
  modified: [pom.xml, src/main/kotlin/cc/keer/amenu/config/MenuModels.kt, src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt, src/main/kotlin/cc/keer/amenu/service/MenuService.kt, src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt, src/main/kotlin/cc/keer/amenu/gui/MenuBindingListener.kt, src/main/resources/plugin.yml, src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt]
key-decisions:
  - "Keep MenuRepository as the single parser seam for provider/cache/update/loading-empty-error metadata."
  - "Resolve AMenu {..} placeholders before delegating %..% to PlaceholderAPI so internal placeholders stay canonical."
  - "Evaluate button-state conditions and rendered text through the same PlaceholderPipeline used by actions and bindings."
patterns-established:
  - "Parser owns normalized provider contracts; runtime services consume explicit models instead of ad hoc maps."
  - "Optional ecosystem integration stays behind a bridge interface so PlaceholderAPI absence is non-fatal."
requirements-completed: [P5-02, P5-04]
duration: 17min
completed: 2026-04-03
---

# Phase 05 Plan 01: Placeholder Pipeline Contract Summary

**Dynamic provider DSL contracts plus an internal-first PlaceholderPipeline now define how AMenu parses provider metadata and renders button, binding, and action text with optional PlaceholderAPI fallback.**

## Performance

- **Duration:** 17 min
- **Started:** 2026-04-03T17:11:57Z
- **Completed:** 2026-04-03T17:28:45Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- Added RED/GREEN coverage for provider/cache/update DSL parsing, optional PlaceholderAPI behavior, and button-state rendering through the shared pipeline.
- Introduced explicit provider/cache/update/surface models and taught `MenuRepository` to parse `provider.type`, `provider.params`, `provider.cache.ttl`, `update.interval`, `loading`, `empty`, and `error`.
- Routed `MenuService`, `ChatInputService`, and `MenuBindingListener` through one shared `PlaceholderPipeline` backed by an optional `PlaceholderApiBridge`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Write RED tests for the dynamic DSL and shared placeholder pipeline** - `0aee2ff` (test)
2. **Task 2: Implement the shared placeholder pipeline, soft PlaceholderAPI bridge, and dynamic DSL contracts** - `980a8e2` (feat)

## Files Created/Modified
- `src/main/kotlin/cc/keer/amenu/service/PlaceholderPipeline.kt` - Shared ordered render entrypoint for internal placeholders plus optional PlaceholderAPI expansion.
- `src/main/kotlin/cc/keer/amenu/service/PlaceholderApiBridge.kt` - Soft dependency bridge that only invokes PlaceholderAPI when the plugin is enabled.
- `src/main/kotlin/cc/keer/amenu/config/MenuModels.kt` - Provider/cache/update/surface model contracts for dynamic page regions.
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - DSL parser updates for provider metadata and loading/empty/error surface state normalization.
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` - Shared pipeline integration for menu titles, action arguments, page-entry placeholders, button-state rendering, and condition evaluation.
- `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` - Prompt text rendering now flows through the shared placeholder pipeline.
- `src/main/kotlin/cc/keer/amenu/gui/MenuBindingListener.kt` - Binding placeholder expansion and placeholder-backed conditions now use the shared pipeline.
- `pom.xml` - Added PlaceholderAPI repository and `provided` dependency.
- `src/main/resources/plugin.yml` - Declared `softdepend: [PlaceholderAPI]`.
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - Added provider metadata parsing regression plus bundled static menu compatibility assertion.
- `src/test/kotlin/cc/keer/amenu/service/PlaceholderPipelineTest.kt` - Added internal-vs-external placeholder and state-rendering regression coverage.

## Decisions Made

- Provider/cache/update/loading-empty-error metadata stays inside `MenuRepository` so later provider execution plans can build on one normalized contract.
- Internal `{player}`-style placeholders remain the canonical built-in syntax, with `%player_name%` only resolving through the optional bridge when PlaceholderAPI is present.
- Button-state matching now supports placeholder expressions such as `{player}` and `%player_name%` without breaking existing direct key lookups like `binding-type`.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- PowerShell parsed the initial Maven `-Dtest=...` invocation incorrectly; rerunning with explicit argument quoting resolved it without code changes.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 05 downstream plans can now assume a stable parser contract for provider metadata and a single runtime placeholder seam.
- PlaceholderAPI presence-path smoke still belongs to later verification, but absence behavior and bridge isolation are now covered automatically.

---
*Phase: 05-page-provider-placeholderapi*
*Completed: 2026-04-03*

## Self-Check: PASSED

- FOUND: .planning/phases/05-page-provider-placeholderapi/05-01-SUMMARY.md
- FOUND: 0aee2ff
- FOUND: 980a8e2
