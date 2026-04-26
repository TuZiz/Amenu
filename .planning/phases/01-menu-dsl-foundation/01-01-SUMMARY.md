---
phase: 01-menu-dsl-foundation
plan: 01
subsystem: config
tags: [kotlin, maven, paper, mockbukkit, yaml, dsl]
requires: []
provides:
  - Java 21 Maven validation path for the workspace
  - Canonical Phase 1 menu DSL example in menus/main.yml
  - MockBukkit regression coverage for MENU-01 and MENU-02
affects: [runtime-interaction-layer, documentation, testing]
tech-stack:
  added: [junit-jupiter, mockbukkit-v1.21, maven-surefire-plugin]
  patterns: [canonical DSL path, inline input prompt registration, MockBukkit plugin loading]
key-files:
  created:
    - src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt
  modified:
    - pom.xml
    - src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt
    - src/main/resources/menus/main.yml
    - .gitignore
key-decisions:
  - "Used the MockBukkit manifest's Paper-Version (1.21.11-R0.1-SNAPSHOT) to align the workspace build path with the test harness."
  - "Kept templates/prompts support in the parser but moved the first-screen example back to the shortest DSL path."
patterns-established:
  - "Phase 1 examples should prefer layout + fill + buttons + head + click + inline input."
  - "MockBukkit tests require the plugin main class to be open for ByteBuddy proxying."
requirements-completed: [MENU-01, MENU-02]
duration: 24min
completed: 2026-04-02
---

# Phase 1: Menu DSL Foundation Summary

**Canonical YAML menu DSL with inline input and MockBukkit regression tests now anchors the AMenu Phase 1 contract.**

## Performance

- **Duration:** 24 min
- **Started:** 2026-04-02T12:43:00+08:00
- **Completed:** 2026-04-02T13:07:00+08:00
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments

- Established a repeatable Java 21 Maven validation path in the workspace and wired in JUnit + MockBukkit.
- Reduced the bundled `main.yml` example back to the shortest Phase 1 path with `fill`, `head`, `click`, and inline `input`.
- Added regression tests that prove the repository loads `MENU-01` and `MENU-02` correctly.

## Task Commits

No atomic commits were created during this execution because the repository is still in an uncommitted bootstrap state. The completed work is present in the workspace and verified by Maven.

## Files Created/Modified

- `pom.xml` - Added the Phase 1 test stack and aligned Paper API with MockBukkit's declared Paper version.
- `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt` - Marked the plugin main class `open` so MockBukkit can proxy-load it.
- `src/main/resources/menus/main.yml` - Rewrote the default example to the compact H/C/L layout.
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - Added DSL regression tests for MENU-01 and MENU-02.
- `.gitignore` - Ignored the local `.tools/` Java 21 runtime used for validation.

## Decisions Made

- Matched Paper API to MockBukkit's manifest (`Paper-Version: 1.21.11-R0.1-SNAPSHOT`) instead of guessing a 1.21 snapshot.
- Preserved parser compatibility aliases while making the bundled example strictly canonical.

## Deviations from Plan

### Auto-fixed Issues

**1. MockBukkit required a more specific Paper API version than the initial test harness guess**
- **Found during:** Task 2 (test harness installation)
- **Issue:** `CustomModelDataComponent` was missing when using a generic 1.21 snapshot
- **Fix:** Read the MockBukkit JAR manifest and aligned `paper.version` to `1.21.11-R0.1-SNAPSHOT`
- **Files modified:** `pom.xml`
- **Verification:** `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test`

**2. MockBukkit could not proxy a final Kotlin plugin class**
- **Found during:** Task 2 (test harness installation)
- **Issue:** ByteBuddy failed with `Cannot subclass ... final types: class cc.keer.amenu.AMenuPlugin`
- **Fix:** Marked `AMenuPlugin` as `open`
- **Files modified:** `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt`
- **Verification:** `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test`

---

**Total deviations:** 2 auto-fixed
**Impact on plan:** Both fixes were required to make the planned MockBukkit validation path executable. No scope creep was introduced.

## Issues Encountered

- PowerShell split raw Maven `-Dtest=...,...` arguments on commas, so verification commands needed quoting.

## User Setup Required

None. The temporary Java 21 runtime lives in `.tools/jdk21` and is ignored by Git.

## Next Phase Readiness

- Phase 2 can build on a stable, tested DSL contract instead of continuing to mutate the default syntax.
- Command/documentation alignment work can now assume the canonical example and test harness already exist.

---
*Phase: 01-menu-dsl-foundation*
*Completed: 2026-04-02*
