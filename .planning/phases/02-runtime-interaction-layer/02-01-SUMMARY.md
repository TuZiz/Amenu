---
phase: 02-runtime-interaction-layer
plan: 01
subsystem: runtime
tags: [kotlin, paper, mockbukkit, runtime-actions, chat-input, regression]
requires:
  - phase: 01
    provides: Canonical DSL baseline and Java 21 test harness
provides:
  - RUN-01, RUN-02, RUN-03 regression coverage
  - INP-01, INP-02, INP-03, INP-04 regression coverage
  - Shared MockBukkit runtime test harness
  - Dual action syntax support for `[open menu]` and `[prompt id]`
affects: [documentation, showcase-menus, future-folia-phase]
tech-stack:
  added: []
  patterns: [shared test harness, service-layer runtime verification, dual action descriptor parsing]
key-files:
  created:
    - src/test/kotlin/cc/keer/amenu/support/MenuPluginTestHarness.kt
    - src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt
    - src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt
  modified:
    - src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt
key-decisions:
  - "Used direct service-layer runtime tests for open/back/refresh/close and prompt lifecycle to avoid MockBukkit inventory-view timing noise."
  - "Expanded action parsing so both `[open history]` and `[open] history` style descriptors resolve correctly."
patterns-established:
  - "Phase 2 runtime tests should share one Java 21-ready MockBukkit harness."
  - "Prompt lifecycle tests should validate reusable prompts and inline input separately from parser-only coverage."
requirements-completed: [RUN-01, RUN-02, RUN-03, INP-01, INP-02, INP-03, INP-04]
duration: 39min
completed: 2026-04-02
---

# Phase 2: Runtime Interaction Layer Summary

**AMenu now has executable regression coverage for runtime action chains and chat-input lifecycles, plus a parser fix for inline action descriptors that were previously broken.**

## Performance

- **Duration:** 39 min
- **Started:** 2026-04-02T13:53:00+08:00
- **Completed:** 2026-04-02T14:12:00+08:00
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Added a shared MockBukkit harness for runtime interaction testing under Java 21.
- Added runtime tests for navigation, command/message/sound side effects, permission denial, and back/refresh behavior.
- Added prompt lifecycle tests for reusable prompts, inline input, submit, cancel, replace, and timeout.
- Fixed the DSL parser so inline descriptors like `[open history]` and `[prompt reusable]` resolve as structured actions instead of raw player commands.

## Task Commits

No atomic commits were created during this execution because the repository is still in an uncommitted bootstrap state. The completed work is present in the workspace and verified by Maven.

## Files Created/Modified

- `src/test/kotlin/cc/keer/amenu/support/MenuPluginTestHarness.kt` - Added shared MockBukkit helpers for menu loading, scheduler advancement, command capture, and chat submission.
- `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt` - Added runtime action regressions for RUN-01, RUN-02, and RUN-03.
- `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt` - Added prompt lifecycle regressions for INP-01 through INP-04.
- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` - Fixed action parsing for inline bracket descriptors such as `[open history]`.

## Decisions Made

- Kept runtime verification centered on `MenuService` and `ChatInputService` instead of expanding the DSL again.
- Treated the inline bracket action parsing bug as Phase 2 scope because it directly blocked `open` and `prompt` runtime behavior.

## Deviations from Plan

### Auto-fixed Issues

**1. Inline action descriptors were parsed as raw player commands**
- **Found during:** Runtime navigation regression work
- **Issue:** `[open history]` and `[prompt reusable]` did not match the repository action parser and silently degraded into player commands
- **Fix:** Replaced the old regex-only parser path with a descriptor parser that supports both inline and split forms
- **Files modified:** `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt`
- **Verification:** `mvn "-Dtest=MenuRuntimeActionTest,ChatInputServiceTest" test`

### Test harness adjustments

**2. MockBukkit GUI click simulation was noisier than the underlying runtime contract**
- **Found during:** Prompt and navigation regression authoring
- **Issue:** Inventory-view switching in mocked click flows introduced timing noise unrelated to the service-layer contract
- **Fix:** Anchored the regression suite on `MenuService.executeActions()/handleClick()` and `ChatInputService.startPrompt()` while still preserving parser/resource coverage elsewhere
- **Files modified:** `src/test/kotlin/cc/keer/amenu/support/MenuPluginTestHarness.kt`, `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt`, `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt`
- **Verification:** `mvn test`

## Issues Encountered

- PowerShell command quoting still requires wrapping Maven `-Dtest=...,...` selectors in quotes.

## User Setup Required

None beyond using Java 21 for Maven execution.

## Next Phase Readiness

- Phase 3 can assume the runtime interaction layer is regression-protected before compatibility abstractions are introduced.
- Showcase/documentation work can now expose already-tested runtime behaviors instead of describing unverified capability.

---
*Phase: 02-runtime-interaction-layer*
*Completed: 2026-04-02*
