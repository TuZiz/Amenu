---
phase: 03-platform-compatibility-layer
plan: 02
subsystem: runtime-compatibility
tags: [kotlin, bukkit, paper, folia, mockbukkit, maven, minimessage, smoke]
requires:
  - phase: 03-platform-compatibility-layer
    provides: Platform scheduler abstraction and compatibility-safe runtime services from 03-01
provides:
  - Bundled compatibility regressions for command entrypoints and bundled menus
  - Explicit Java 21 packaging proof and shaded MiniMessage contract
  - Operator-facing Paper/Folia smoke scripts and compatibility documentation
affects: [verification, smoke-assets, plugin-metadata, README, release-readiness]
tech-stack:
  added: []
  patterns: [bundled-menu regression protection, explicit shaded-jar proof, documented live smoke boundary]
key-files:
  created:
    - src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt
    - .smoke/README.md
    - .smoke/paper-smoke.ps1
    - .smoke/folia-smoke.ps1
  modified:
    - src/test/kotlin/cc/keer/amenu/command/AMenuCommandTest.kt
    - src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt
    - pom.xml
    - README.md
    - src/main/resources/plugin.yml
key-decisions:
  - "Made the shaded artifact classifier explicit in pom.xml so the MiniMessage packaging contract is deliberate instead of an incidental shade-plugin outcome."
  - "Published compatibility claims with an explicit automated-vs-manual proof boundary so Paper/Folia support is testable without overclaiming live runtime coverage."
patterns-established:
  - "Compatibility closeout should pair automated regressions with a documented smoke boundary rather than treating MockBukkit as proof of live Folia behavior."
  - "Bundled example coverage should assert main/history/admin/runtime as general menu examples, not just as skin-menu fixtures."
requirements-completed: [COMP-01, COMP-02, COMP-03]
duration: 9min
completed: 2026-04-02
---

# Phase 3 Plan 2: Compatibility Closeout Summary

**Bundled menu regressions, Java 21 shaded MiniMessage proof, and explicit Paper/Folia smoke guidance now close AMenu's platform compatibility boundary without changing its general menu-plugin positioning.**

## Performance

- **Duration:** 9 min
- **Started:** 2026-04-02T17:52:00+08:00
- **Completed:** 2026-04-02T18:01:07+08:00
- **Tasks:** 3
- **Files modified:** 9

## Accomplishments

- Added bundled compatibility regressions that keep `main`, `history`, `admin`, and `runtime` loadable and reachable through plugin entrypoints.
- Added `.smoke` assets plus README guidance that distinguish automated proof from live Paper/Folia smoke verification.
- Kept MiniMessage shading explicit and verified that `target/amenu-1.0.0-SNAPSHOT-shaded.jar` still packages `net/kyori/adventure/text/minimessage/MiniMessage.class`.

## Task Commits

Each task landed as an atomic execution commit:

1. **Task 1: Add bundled-menu and command-surface compatibility regressions** - `60be31e` (`test`)
2. **Task 2: Add packaging proof and Paper/Folia smoke-run assets** - `132e3fc` (`feat`)
3. **Task 3: Update plugin metadata and README to publish the compatibility boundary without losing general-menu positioning** - `75aa63b` (`feat`)

## Files Created/Modified

- `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt` - Verifies bundled menus still open through command, navigation, and service entrypoints.
- `src/test/kotlin/cc/keer/amenu/command/AMenuCommandTest.kt` - Extends command regressions through reload and bundled menu availability.
- `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - Locks bundled resource parsing to the full example set rather than a skin-only subset.
- `.smoke/README.md` - States the automated proof surface and the manual Paper/Folia smoke boundary.
- `.smoke/paper-smoke.ps1` - Automates Java 21 Paper startup smoke around the shaded AMenu jar.
- `.smoke/folia-smoke.ps1` - Automates Java 21 Folia startup smoke around the shaded AMenu jar.
- `pom.xml` - Makes the shaded classifier explicit while retaining the MiniMessage dependency contract.
- `README.md` - Publishes the compatibility matrix and keeps `main.yml` vs bundled examples positioned correctly.
- `src/main/resources/plugin.yml` - Marks the plugin as `folia-supported: true` while preserving `/skinmenu` alias compatibility.

## Decisions Made

- Kept the product narrative anchored on a general low-config menu engine and treated `history.yml`, `admin.yml`, and `runtime.yml` as supporting bundled examples.
- Treated packaging proof and live smoke instructions as part of the compatibility contract, not as optional release notes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Switched Maven verification to the repo-local Java 21 runtime**
- **Found during:** Task 1 (bundled-menu and command-surface compatibility regressions)
- **Issue:** The executor defaulted to Java 25, so every Maven verify command failed the existing Java 21 enforcer rule before tests could run.
- **Fix:** Ran all plan verification commands with `D:\codex\Amenu\.tools\jdk21` as `JAVA_HOME` and prepended its `bin` directory to `PATH`.
- **Files modified:** None - execution environment only
- **Verification:** `mvn "-Dtest=BundledMenuCompatibilityTest,AMenuCommandTest,MenuRepositoryDslTest" test`, `mvn test`, and `mvn package` all passed under Java 21
- **Committed in:** N/A - execution-only adjustment

---

**Total deviations:** 1 auto-fixed (1 blocking environment issue)
**Impact on plan:** No scope creep. The fix only restored the required Java 21 validation path already defined by the repository.

## Issues Encountered

- The global shell runtime used Java 25 by default, so plan verification had to be pinned to the repo-local Java 21 toolchain.

## User Setup Required

None - no external service configuration is required. Live Paper/Folia smoke still needs local server jars and a Java 21 runtime, as documented in `.smoke/README.md`.

## Next Phase Readiness

- Phase 3 is now fully closed: scheduler abstraction, bundled regressions, packaging proof, and smoke guidance are all in place.
- Phase 4 can build on a stable compatibility baseline instead of reopening packaging or product-positioning concerns.

---
*Phase: 03-platform-compatibility-layer*
*Completed: 2026-04-02*

## Self-Check: PASSED

- Found `.planning/phases/03-platform-compatibility-layer/03-02-SUMMARY.md`
- Found task commits `60be31e`, `132e3fc`, and `75aa63b` in git history
