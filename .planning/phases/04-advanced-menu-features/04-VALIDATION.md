---
phase: 04
slug: advanced-menu-features
status: ready
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-02
---

# Phase 04 - Validation Strategy

> Per-phase validation contract for advanced menu capability work.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit Jupiter 6.0.3 + MockBukkit 4.108.0 |
| **Quick run command** | `mvn "-Dtest=MenuRepositoryDslTest,MenuRuntimeActionTest,ChatInputServiceTest" test` |
| **Full suite command** | `mvn test` |
| **Packaging command** | `mvn package` |
| **Estimated runtime** | ~30-45 seconds |

---

## Sampling Rate

- **After every task commit:** Run the task-specific focused command
- **After every plan completes:** Run `mvn test`
- **Before any Phase 4 verification closeout:** `mvn test` and `mvn package` must both be green
- **Max feedback latency:** 45 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Requirement | Test Type | Automated Command | Status |
|---------|------|-------------|-----------|-------------------|--------|
| 04-01-01 | 01 | ADV-01 | parser/integration | `mvn "-Dtest=MenuRepositoryDslTest" test` | pending |
| 04-01-02 | 01 | ADV-01 | runtime/integration | `mvn "-Dtest=MenuRuntimeActionTest" test` | pending |
| 04-01-03 | 01 | ADV-01 | showcase/integration | `mvn "-Dtest=BundledMenuCompatibilityTest" test` | pending |
| 04-02-01 | 02 | ADV-02 | parser/integration | `mvn "-Dtest=MenuRepositoryDslTest" test` | pending |
| 04-02-02 | 02 | ADV-02, ADV-03 | runtime/integration | `mvn "-Dtest=MenuRuntimeActionTest,AMenuCommandTest" test` | pending |
| 04-02-03 | 02 | ADV-02, ADV-03 | showcase/docs | `mvn test` | pending |

---

## Wave 0 Requirements

Existing infrastructure is sufficient:
- Java 21 build path already exists in-repo
- MockBukkit harness and bundled-menu regression suite already exist
- Phase 3 compatibility abstraction is already in place for safe async return paths

No extra Wave 0 bootstrap plan is required before Phase 4 execution.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Paged menu navigation feels correct in a live client | ADV-01 | MockBukkit does not prove real inventory UX feel | Open the bundled pagination lab in-game, flip pages, and confirm slot stability and button affordances |
| Async menu refresh feels stable under real server timing | ADV-01 | MockBukkit covers logic, not player-perceived pacing | Open the bundled async lab, wait for refresh, and confirm no flicker or thread warnings |
| Conditions and richer bindings behave intuitively in live play | ADV-02, ADV-03 | Human review is better for UX clarity than assertions alone | Trigger the bundled condition/binding showcase with different permissions and contexts, then confirm the rendered result matches expectations |

---

## Validation Sign-Off

- [ ] Every task has an explicit automated verify command
- [ ] No 3 consecutive tasks without automated feedback
- [x] Wave 0 coverage is already satisfied
- [ ] Final `mvn test` is green after execution
- [ ] Final `mvn package` is green after execution
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-04-02
