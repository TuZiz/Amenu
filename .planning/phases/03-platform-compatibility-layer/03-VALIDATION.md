---
phase: 03
slug: platform-compatibility-layer
status: ready
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-02
---

# Phase 03 - Validation Strategy

> Per-phase validation contract for compatibility execution and feedback sampling.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 6.0.3 + MockBukkit 4.108.0 + PowerShell smoke scripts |
| **Config file** | none - Maven Surefire in `pom.xml` |
| **Quick run command** | `mvn "-Dtest=PlatformSchedulerFactoryTest,MenuServiceCompatibilityTest,ChatInputServiceCompatibilityTest,BundledMenuCompatibilityTest,AMenuCommandTest" test` |
| **Full suite command** | `mvn test` |
| **Packaging command** | `mvn package` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn "-Dtest=PlatformSchedulerFactoryTest,MenuServiceCompatibilityTest,ChatInputServiceCompatibilityTest,BundledMenuCompatibilityTest,AMenuCommandTest" test`
- **After every plan wave:** Run `mvn test`
- **Before `$gsd-verify-work`:** `mvn test` and `mvn package` must both be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 03-01-01 | 01 | 1 | COMP-02 | integration | `mvn "-Dtest=PlatformSchedulerFactoryTest" test` | created in task | pending |
| 03-01-02 | 01 | 1 | COMP-01 | integration | `mvn "-Dtest=MenuServiceCompatibilityTest,ChatInputServiceCompatibilityTest" test` | created in task | pending |
| 03-01-03 | 01 | 1 | COMP-01, COMP-02 | integration | `mvn "-Dtest=PlatformSchedulerFactoryTest,MenuServiceCompatibilityTest,ChatInputServiceCompatibilityTest" test` | created in task | pending |
| 03-02-01 | 02 | 2 | COMP-01, COMP-03 | integration | `mvn "-Dtest=BundledMenuCompatibilityTest,AMenuCommandTest,MenuRepositoryDslTest" test` | created in task | pending |
| 03-02-02 | 02 | 2 | COMP-02 | packaging | `mvn package` | yes | pending |
| 03-02-03 | 02 | 2 | COMP-03 | docs | `mvn test` | yes | pending |

---

## Wave 0 Requirements

Existing infrastructure covers all Phase 3 requirements:
- Java 21 build/test packaging path already exists in this workspace
- MockBukkit harness already exists at `src/test/kotlin/cc/keer/amenu/support/MenuPluginTestHarness.kt`
- New regression files and smoke assets are created inside their owning tasks before each task-level verify command runs

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Open `/amenu`, navigate to a secondary menu, and close it on Paper | COMP-01 | MockBukkit does not prove a live server inventory lifecycle | Start a Paper server with Java 21, install the shaded jar, run `/amenu`, click one navigation path, and confirm no inventory/thread exceptions appear in the log |
| Start a prompt, cancel it, and reload/stop the plugin on Folia | COMP-01, COMP-02 | Real player/region scheduler ownership is not reproduced by MockBukkit | Start a Folia server with Java 21, run `/amenu`, trigger a prompt, type `cancel`, then run reload or stop; confirm no prompt-task leakage or scheduler ownership errors appear |
| Confirm README and bundled examples still present AMenu as a general menu engine | COMP-03 | Product wording and example framing are easier to assess by human review than by assertions alone | Read `README.md`, `plugin.yml`, `main.yml`, `history.yml`, `admin.yml`, and `runtime.yml`; verify skin menus are described as examples and compatibility work is not framed as a skin-plugin rewrite |

---

## Validation Sign-Off

- [ ] All tasks have automated verify steps or explicit manual-only follow-up
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 coverage is satisfied by existing infrastructure plus task-owned file creation
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-04-02
