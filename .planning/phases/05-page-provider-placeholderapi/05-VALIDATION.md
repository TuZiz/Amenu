---
phase: 05
slug: page-provider-placeholderapi
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-04-03
---

# Phase 05 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 6.0.3 + MockBukkit 4.108.0 |
| **Config file** | none - Maven Surefire + Kotlin Maven plugin in `pom.xml` |
| **Quick run command** | `set JAVA_HOME=D:\codex\Amenu\.tools\jdk21 && set PATH=%JAVA_HOME%\bin;%PATH% && mvn -q -Dtest=MenuRepositoryDslTest test` |
| **Full suite command** | `set JAVA_HOME=D:\codex\Amenu\.tools\jdk21 && set PATH=%JAVA_HOME%\bin;%PATH% && mvn test` |
| **Estimated runtime** | ~20-30 seconds for routine task-level checks; ~45 seconds full wave sweep |

---

## Sampling Rate

- **After every task commit:** Run the exact task-level command from the verification map below instead of the umbrella suite; target routine feedback under 30 seconds.
- **After every plan wave:** Run `set JAVA_HOME=D:\codex\Amenu\.tools\jdk21 && set PATH=%JAVA_HOME%\bin;%PATH% && mvn test`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds for task-level checks, 60 seconds for wave-level sweeps

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | P5-04 | unit | `mvn -q -Dtest=MenuRepositoryDslTest test` | Yes | pending |
| 05-01-02 | 01 | 1 | P5-02 | unit | `mvn -q -Dtest=PlaceholderPipelineTest test` | No - W0 | pending |
| 05-02-01 | 02 | 2 | P5-01 | unit/service | `mvn -q -Dtest=MenuDynamicProviderServiceTest test` | No - W0 | pending |
| 05-02-02 | 02 | 2 | P5-05 | compatibility/service | `mvn -q -Dtest=MenuServiceCompatibilityTest test` | Yes | pending |
| 05-03-01 | 03 | 3 | P5-03 | service | `mvn -q -Dtest=MenuDynamicRefreshTest test` | No - W0 | pending |
| 05-03-02 | 03 | 3 | P5-06 | manual smoke | `manual-only` | No | pending |

*Status: pending -> green -> red -> flaky*

---

## Wave 0 Requirements

- [ ] `src/test/kotlin/cc/keer/amenu/service/MenuDynamicProviderServiceTest.kt` - provider cache, invalidation, and provider result-state transitions
- [ ] `src/test/kotlin/cc/keer/amenu/service/PlaceholderPipelineTest.kt` - internal placeholders, PlaceholderAPI soft bridge absence/presence shim, and parameter ordering
- [ ] `src/test/kotlin/cc/keer/amenu/service/MenuDynamicRefreshTest.kt` - timed reevaluation, close/reload cleanup, and no reopen churn
- [ ] Manual PlaceholderAPI-enabled smoke recipe - verify `%player_name%`, cache invalidation, and one timed button update on Paper or Folia

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| PlaceholderAPI presence path on real server | P5-06 | Requires optional plugin installation and real server/runtime expansion behavior | Install PlaceholderAPI on Paper or Folia, open a menu using `%player_name%`, verify placeholder resolves, trigger a cache invalidation path, and confirm menu remains stable |
| Timed button visual update while menu stays open | P5-03 | Best validated against real inventory refresh timing and player-visible behavior | Open a reward-style menu, wait through one configured update interval, confirm icon/name/lore or hidden state changes without reopening the menu |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all missing references
- [ ] No watch-mode flags
- [ ] Feedback latency < 60s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
