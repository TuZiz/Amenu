---
phase: 03-platform-compatibility-layer
verified: 2026-04-02T10:19:47.1474931Z
status: human_needed
score: 6/6 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 5/6
  gaps_closed:
    - "Bundled example menus remain bundled examples of a general menu engine even after compatibility messaging is expanded."
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Paper startup smoke"
    expected: "The server reaches Done, enables AMenu cleanly, and shows no NoClassDefFoundError, scheduler reflection failures, or prompt cleanup exceptions."
    why_human: "Requires a real Paper server jar and startup environment outside MockBukkit."
  - test: "Folia startup smoke"
    expected: "The server reaches Done, enables AMenu cleanly, and shows no NoClassDefFoundError, scheduler reflection failures, or prompt cleanup exceptions."
    why_human: "Reflective Folia scheduler selection cannot be proven in MockBukkit."
  - test: "Live prompt and navigation pass on Paper and Folia"
    expected: "Opening /amenu, navigating bundled menus, and completing/canceling/reloading prompt flows remain stable with clean logs."
    why_human: "Requires live player interaction and real platform thread ownership behavior."
---

# Phase 3: Platform Compatibility Layer Verification Report

**Phase Goal:** Harden the plugin for Spigot/Paper and introduce a maintainable Folia compatibility abstraction.
**Verified:** 2026-04-02T10:19:47.1474931Z
**Status:** human_needed
**Re-verification:** Yes - after gap closure

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Players can still open the default menu, navigate to secondary menus, and trigger runtime actions safely after the compatibility handoff is introduced. | VERIFIED | `MenuService` still routes player-bound execution through `platformScheduler.executeFor` in `src/main/kotlin/cc/keer/amenu/service/MenuService.kt:213`, and `MenuServiceCompatibilityTest` still covers default open and click-driven secondary navigation in `src/test/kotlin/cc/keer/amenu/service/MenuServiceCompatibilityTest.kt:50` and `src/test/kotlin/cc/keer/amenu/service/MenuServiceCompatibilityTest.kt:62`. |
| 2 | Prompt submit, cancel, timeout, reload, and shutdown cleanup remain safe and observable after async chat input returns to the compatibility scheduler. | VERIFIED | `ChatInputService` still uses `platformScheduler.runLaterFor` and `platformScheduler.executeFor` in `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt:55` and `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt:80`, and the compatibility regressions still cover async completion and reload/shutdown cleanup in `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt:45` and `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt:94`. |
| 3 | One shaded plugin jar supports classic Spigot/Paper execution and Folia-aware scheduling without changing the menu DSL. | VERIFIED | `PlatformSchedulerFactory.create()` still selects the runtime scheduler in `src/main/kotlin/cc/keer/amenu/platform/PlatformSchedulerFactory.kt:9`, `plugin.yml` still declares `folia-supported: true` in `src/main/resources/plugin.yml:5`, and `mvn -DskipTests package` still produces `target/amenu-1.0.0-SNAPSHOT-shaded.jar`. |
| 4 | Bundled example menus remain bundled examples of a general menu engine even after compatibility messaging is expanded. | VERIFIED | `BundledMenuCompatibilityTest` is now 71 lines, contains three focused regressions at `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt:14`, `:50`, and `:73`, asserts the full bundled set `main/history/admin/runtime` at `:17`, and still drives command, navigation, prompt-entry, and permission-feedback paths. |
| 5 | The repository states exactly what is proven by automated tests, packaging checks, and live Paper/Folia smoke. | VERIFIED | `README.md` still separates automated proof from manual smoke in `README.md:78`, `README.md:88`, and `README.md:89`, while `.smoke/README.md` keeps the explicit proof boundary and live smoke workflow in `.smoke/README.md:5`, `:24`, `:36`, and `:50`. |
| 6 | The shaded jar keeps direct Adventure runtime dependencies, including MiniMessage, inside the plugin artifact. | VERIFIED | `pom.xml` still keeps `adventure-text-minimessage` and `maven-shade-plugin` in `pom.xml:54` and `pom.xml:144`, and `jar tf target/amenu-1.0.0-SNAPSHOT-shaded.jar` still finds `net/kyori/adventure/text/minimessage/MiniMessage.class`. |

**Score:** 6/6 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `src/main/kotlin/cc/keer/amenu/platform/PlatformScheduler.kt` | Single compatibility contract for player-bound and global runtime execution | VERIFIED | Exists and still declares `interface PlatformScheduler` in `src/main/kotlin/cc/keer/amenu/platform/PlatformScheduler.kt:5`. |
| `src/main/kotlin/cc/keer/amenu/platform/FoliaPlatformScheduler.kt` | Reflective Folia execution path with Bukkit fallback | VERIFIED | Exists and still declares `class FoliaPlatformScheduler` in `src/main/kotlin/cc/keer/amenu/platform/FoliaPlatformScheduler.kt:10`; reflective `null` branches are fallback plumbing, not stubs. |
| `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt` | Regression coverage for async chat handoff, timeout scheduling, and cleanup through the compatibility layer | VERIFIED | Exists, passes the plan artifact check, and still contains the targeted async/reload cleanup tests in `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt:45` and `:94`. |
| `.smoke/README.md` | Explicit Paper/Folia smoke workflow and manual verification boundary | VERIFIED | Exists, contains `MiniMessage`, and documents automated proof plus Paper/Folia live smoke in `.smoke/README.md:5`, `:14`, `:24`, `:36`, and `:50`. |
| `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt` | Regression coverage for bundled menus and command-entry compatibility | VERIFIED | Exists, passes the plan artifact check, is 71 lines long, and now covers bundled menu set, navigation/prompt entrypoints, and admin permission feedback across three tests. |
| `README.md` | Operator-facing compatibility matrix and general-product positioning | VERIFIED | Exists, contains `Compatibility` in `README.md:78`, and keeps the general-plugin positioning plus bundled-example scope in `README.md:3` and `README.md:91`. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| `src/test/kotlin/cc/keer/amenu/platform/PlatformSchedulerFactoryTest.kt` | `src/main/kotlin/cc/keer/amenu/platform/PlatformSchedulerFactory.kt` | factory assertions prove the runtime selects the correct scheduler path | WIRED | `PlatformSchedulerFactoryTest` still calls `PlatformSchedulerFactory.create(plugin)` in `src/test/kotlin/cc/keer/amenu/platform/PlatformSchedulerFactoryTest.kt:14` and `:28`, matching `fun create(plugin: JavaPlugin): PlatformScheduler` in `src/main/kotlin/cc/keer/amenu/platform/PlatformSchedulerFactory.kt:9`. |
| `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt` | `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` | compatibility regressions exercise async handoff and timeout cleanup | WIRED | The test still targets async completion and reload/shutdown cleanup in `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceCompatibilityTest.kt:45` and `:94`, matching the runtime handoff entrypoint `fun onAsyncChat(event: AsyncChatEvent)` and `platformScheduler.executeFor(...)` in `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt:74` and `:80`. |
| `.smoke/README.md` | `pom.xml` | smoke instructions depend on the Java 21 and shaded jar contract | WIRED | `.smoke/README.md` still instructs `mvn ... test` and jar inspection in `.smoke/README.md:12` and `:14`, while `pom.xml` still provides the `adventure-text-minimessage` dependency and `maven-shade-plugin` in `pom.xml:54` and `:144`. |
| `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt` | `src/main/resources/menus/runtime.yml` | bundled-menu regression covers the secondary examples after compatibility changes | WIRED | The regression opens and asserts `runtime` in `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt:17`, `:31`, `:42`, and `:55`, and the bundled resource is still saved by `AMenuPlugin` in `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt:67` and loaded through `MenuRepository.loadMenus()` in `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt:16`. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `src/main/kotlin/cc/keer/amenu/service/MenuService.kt` | `menuId` and resolved menu definition | `AMenuCommand` / runtime actions -> `MenuRepository.menu(menuId)` -> YAML-backed menu definitions | Yes | FLOWING |
| `src/main/kotlin/cc/keer/amenu/service/ChatInputService.kt` | `message` and prompt session state | `AsyncChatEvent.message()` -> `complete()` -> `menuService.executeActions(...)` | Yes | FLOWING |
| `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt` | `bundledMenuIds` | `AMenuPlugin.saveBundledResource(...)` -> `MenuRepository.loadMenus()` -> `listMenuIds()` | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Phase 03 targeted compatibility regressions | `mvn "-Dtest=PlatformSchedulerFactoryTest,MenuServiceCompatibilityTest,ChatInputServiceCompatibilityTest,BundledMenuCompatibilityTest,AMenuCommandTest,MenuRepositoryDslTest" test` | 16 tests, 0 failures, BUILD SUCCESS under repo-local Java 21 | PASS |
| Full automated test suite | `mvn test` | 23 tests, 0 failures, BUILD SUCCESS under repo-local Java 21 | PASS |
| Shaded plugin packaging | `mvn -DskipTests package` | BUILD SUCCESS; shaded jar attached at `target/amenu-1.0.0-SNAPSHOT-shaded.jar` | PASS |
| MiniMessage packaged inside plugin jar | `jar tf target/amenu-1.0.0-SNAPSHOT-shaded.jar | Select-String 'net/kyori/adventure/text/minimessage/MiniMessage.class'` | `MiniMessage.class` found in shaded jar | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| `COMP-01` | `03-01`, `03-02` | Runtime menu behavior is compatible with Spigot/Paper main-thread GUI and command boundaries. | SATISFIED | `MenuService` still gates player-bound execution through the scheduler and the menu/command regressions still pass across default open, secondary open, reload, and bundled entrypoints. |
| `COMP-02` | `03-01`, `03-02` | The plugin introduces a Folia-compatible scheduling abstraction instead of assuming a single main thread. | SATISFIED | `PlatformScheduler`, `FoliaPlatformScheduler`, `PlatformSchedulerFactory`, `AMenuPlugin`, and `ChatInputService` remain wired through the scheduler abstraction and the platform compatibility tests still pass. |
| `COMP-03` | `03-02` | Bundled example menus remain examples and do not redefine the product as a skin-only plugin. | SATISFIED | `README.md` still states AMenu is not a skin-only plugin, and the expanded 71-line bundled regression keeps `main/history/admin/runtime` positioned as bundled examples rather than product scope. |

Phase 3 requirement IDs in `REQUIREMENTS.md`: `COMP-01`, `COMP-02`, `COMP-03`.
Plan frontmatter requirement IDs across `03-01-PLAN.md` and `03-02-PLAN.md`: `COMP-01`, `COMP-02`, `COMP-03`.
Orphaned Phase 3 requirements: none.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `src/main/kotlin/cc/keer/amenu/platform/FoliaPlatformScheduler.kt` | 167 | Reflective `return null` fallback branch | INFO | Not a stub. The branch is part of capability detection and immediately feeds the Bukkit fallback path when Folia hooks are unavailable. |

### Human Verification Required

### 1. Paper Startup Smoke

**Test:** Run `.smoke\paper-smoke.ps1 -ServerDir <paper-server-dir> -ServerJar <paper-jar>` after `mvn package`.
**Expected:** The server reaches `Done (`; logs show `Enabling AMenu`; no `NoClassDefFoundError`, scheduler reflection failures, or prompt cleanup exceptions.
**Why human:** Requires a real Paper server jar and startup environment outside MockBukkit.

### 2. Folia Startup Smoke

**Test:** Run `.smoke\folia-smoke.ps1 -ServerDir <folia-server-dir> -ServerJar <folia-jar>` after `mvn package`.
**Expected:** The server reaches `Done (`; logs show `Enabling AMenu`; no `NoClassDefFoundError`, scheduler reflection failures, or prompt cleanup exceptions.
**Why human:** Reflective Folia scheduler selection cannot be proven in MockBukkit.

### 3. Live Prompt / Navigation Pass

**Test:** On both Paper and Folia, open `/amenu`, navigate bundled menus, start one prompt in `runtime.yml`, then submit, cancel, reload, and stop.
**Expected:** Menu navigation remains stable, prompt cleanup is visible, and logs stay clean during reload and shutdown.
**Why human:** Requires real player interaction and live platform thread ownership behavior.

### Gaps Summary

No automated gaps remain. The previous plan-contract failure is closed: `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt` now meets the declared `min_lines: 70` threshold at 71 lines and materially deepens coverage instead of padding the file.

Phase 03 now verifies cleanly at the automated level: all must-have truths pass, all required artifacts pass the plan checks, the key links are wired, the full test suite passes under Java 21, and the shaded jar still packages MiniMessage. The remaining boundary is the same one documented by the phase itself: live Paper/Folia startup and real-player smoke still need human execution.

---

_Verified: 2026-04-02T10:19:47.1474931Z_
_Verifier: Claude (gsd-verifier)_
