---
phase: 02-runtime-interaction-layer
plan: 02
subsystem: showcase
tags: [yaml, readme, prompts, permissions, runtime-showcase]
requires:
  - phase: 02
    plan: 01
    provides: Runtime regressions and stable action parsing
provides:
  - Bundled runtime showcase menus
  - README runtime interaction guidance
  - Parser coverage for history/admin/runtime bundled examples
affects: [onboarding, examples, operator-docs]
tech-stack:
  added: []
  patterns: [canonical main menu stays short, richer runtime examples live in secondary menus]
key-files:
  created:
    - src/main/resources/menus/runtime.yml
  modified:
    - README.md
    - src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt
    - src/main/resources/menus/history.yml
    - src/main/resources/menus/admin.yml
    - src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt
key-decisions:
  - "Added a dedicated runtime showcase menu instead of bloating main.yml."
  - "Saved runtime.yml through plugin bootstrap so server owners receive the example automatically."
patterns-established:
  - "main.yml remains the shortest canonical entry point."
  - "history/admin/runtime are the teaching surface for advanced runtime behavior."
requirements-completed: [RUN-01, RUN-03, INP-01, INP-03, INP-04]
duration: 9min
completed: 2026-04-02
---

# Phase 2: Runtime Interaction Layer Summary

**AMenu now ships dedicated showcase menus and docs that make runtime interaction behavior discoverable without expanding the canonical `main.yml` example.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-04-02T14:12:00+08:00
- **Completed:** 2026-04-02T14:21:00+08:00
- **Tasks:** 3
- **Files modified:** 6

## Accomplishments

- Added `runtime.yml` as a dedicated bundled runtime showcase for reusable prompts, inline input, cancel flow, and timeout guidance.
- Updated `history.yml` and `admin.yml` so server owners can discover back navigation, runtime routing, permission denial, and refresh behavior from bundled examples.
- Rewrote the README around the canonical-vs-showcase split and documented the runtime example menus.
- Added parser coverage to ensure `history`, `admin`, and `runtime` continue loading with the intended structures.

## Task Commits

No atomic commits were created during this execution because the repository is still in an uncommitted bootstrap state. The completed work is present in the workspace and verified by Maven.

## Files Created/Modified

- `src/main/resources/menus/runtime.yml` - Added a dedicated runtime interaction lab example.
- `src/main/resources/menus/history.yml` - Added a routed entry into the runtime showcase while preserving back-stack behavior.
- `src/main/resources/menus/admin.yml` - Added a local `deny-actions` example and clarified refresh/admin behavior.
- `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt` - Bootstraps `runtime.yml` for first-run installs.
- `README.md` - Documents the runtime showcase split and keeps the first example canonical.
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - Added bundled showcase resource assertions.

## Decisions Made

- Kept `main.yml` untouched as the shortest entrypoint and pushed richer runtime teaching into `history.yml`, `admin.yml`, and `runtime.yml`.
- Used ASCII-based showcase text for the new and rewritten example resources to avoid further encoding noise during testing and maintenance.

## Deviations from Plan

None. The showcase and documentation work stayed inside the planned Phase 2 scope.

## Issues Encountered

- Existing resource files had encoding noise in shell output, so the showcase resources and README were rewritten cleanly in ASCII rather than patched line-by-line.

## User Setup Required

None. `runtime.yml` is now bundled and auto-bootstrapped.

## Next Phase Readiness

- Phase 3 compatibility work can now assume server owners have clear bundled examples for runtime behavior before platform abstractions are added.

---
*Phase: 02-runtime-interaction-layer*
*Completed: 2026-04-02*
