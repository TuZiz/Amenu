---
phase: 02
slug: runtime-interaction-layer
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 02 - Validation Strategy

> Per-phase validation contract for runtime interaction execution and feedback sampling.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 6.0.3 + MockBukkit 4.108.0 |
| **Config file** | none - Maven Surefire in `pom.xml` |
| **Quick run command** | `mvn "-Dtest=MenuRuntimeActionTest,ChatInputServiceTest" test` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~20 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn "-Dtest=MenuRuntimeActionTest,ChatInputServiceTest" test`
- **After every plan wave:** Run `mvn test`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 20 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | RUN-01 | integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | no - Wave 0 | pending |
| 02-01-02 | 01 | 1 | RUN-02 | integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | no - Wave 0 | pending |
| 02-01-03 | 01 | 1 | RUN-03 | integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | no - Wave 0 | pending |
| 02-02-01 | 02 | 2 | INP-01 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | no - Wave 0 | pending |
| 02-02-02 | 02 | 2 | INP-02 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | no - Wave 0 | pending |
| 02-02-03 | 02 | 2 | INP-03 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | no - Wave 0 | pending |
| 02-02-04 | 02 | 2 | INP-04 | integration | `mvn "-Dtest=ChatInputServiceTest" test` | no - Wave 0 | pending |

---

## Wave 0 Requirements

- [ ] `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt` - covers RUN-01 / RUN-02 / RUN-03
- [ ] `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt` - covers INP-01 / INP-02 / INP-03 / INP-04
- [ ] Shared test helpers for menu opening, slot lookup, message/sound assertions, and scheduler tick advancement
- [ ] Java 21 path standardized for Maven test execution (`.tools/jdk21` or equivalent Java 21 runtime)
- [ ] Stable prompt-driving strategy validated in tests (`AsyncChatEvent` construction or `PlayerMock.chat()`), then reused consistently

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Confirm `main.yml` remains the shortest canonical DSL while runtime showcases live in secondary menus | RUN-01, RUN-03, INP-01 | This is a product-facing example strategy rather than pure code behavior | Review `src/main/resources/menus/main.yml` plus `history.yml` / `admin.yml` / any new runtime showcase menu; verify advanced runtime interactions are not pushed back into the default canonical example |
| Confirm runtime example menus communicate permission denial, back navigation, refresh, and prompt lifecycle clearly to server owners | RUN-03, INP-01, INP-03, INP-04 | Readability and showcase quality are easier to assess manually than with assertions alone | Open the bundled runtime example menus and README snippets; verify a server owner can discover the supported runtime behaviors without reading Kotlin source |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or explicit Wave 0 prerequisites
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all missing runtime regression files
- [ ] No watch-mode flags
- [ ] Feedback latency < 20s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
