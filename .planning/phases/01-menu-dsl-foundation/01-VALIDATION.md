---
phase: 01
slug: menu-dsl-foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-02
---

# Phase 01 - Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 6.x + MockBukkit |
| **Config file** | none - Wave 0 installs |
| **Quick run command** | `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~20 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test`
- **After every plan wave:** Run `mvn test`
- **Before `$gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 20 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | MENU-01 | unit/integration | `mvn -Dtest=MenuRepositoryDslTest#loads_core_layout_fill_and_buttons test` | 鉂?W0 | 猬?pending |
| 01-01-02 | 01 | 1 | MENU-02 | unit/integration | `mvn -Dtest=MenuRepositoryDslTest#parses_shorthand_head_click_and_inline_input test` | 鉂?W0 | 猬?pending |
| 01-02-01 | 02 | 2 | MENU-03 | integration | `mvn -Dtest=AMenuCommandTest#opens_default_and_named_menu test` | 鉂?W0 | 猬?pending |

*Status: 猬?pending 路 鉁?green 路 鉂?red 路 鈿狅笍 flaky*

---

## Wave 0 Requirements

- [ ] `src/test/kotlin/cc/keer/amenu/config/MenuRepositoryDslTest.kt` - stubs for MENU-01 and MENU-02
- [ ] `src/test/kotlin/cc/keer/amenu/command/AMenuCommandTest.kt` - stub for MENU-03
- [ ] JUnit Jupiter test-scope dependency in `pom.xml`
- [ ] MockBukkit test-scope dependency in `pom.xml`
- [ ] Java 21 available on active build path for Maven validation

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Verify README and bundled example convey general menu engine positioning rather than skin-plugin positioning | MENU-03 | Product wording and first-run comprehension are documentation-facing and easier to review manually than with code-only assertions | Open `README.md`, `plugin.yml`, and `src/main/resources/menus/main.yml`; confirm `/amenu` is the primary command and skin menus are described as examples only |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 20s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
