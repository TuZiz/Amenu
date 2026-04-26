# Phase 3: Platform Compatibility Layer - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `03-CONTEXT.md`; this log preserves the default choices carried forward during context capture.

**Date:** 2026-04-02
**Phase:** 03-platform-compatibility-layer
**Areas discussed:** platform baseline, scheduler boundary, verification strategy, product boundary

---

## Platform baseline

| Option | Description | Selected |
|--------|-------------|----------|
| Spigot and Paper only | Keep Folia deferred again and only harden classic server behavior | |
| Spigot, Paper, and explicit Folia path | Treat Folia as a real supported mode in this phase, but only for compatibility work | X |
| Paper-first only | Ignore Spigot and defer both Spigot/Folia nuance | |

**User's choice:** Auto-carried recommended default: Spigot, Paper, and an explicit Folia compatibility path.
**Notes:** This matches the roadmap and the user's repeated "兼容 Spigot/Folia" direction.

---

## Scheduler boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Keep direct `server.scheduler` calls | Patch only the hottest callsites and leave the rest as-is | |
| Introduce a focused compatibility scheduler abstraction | Centralize sync handoff, delays, and player/menu execution semantics under one layer | X |
| Broad platform framework refactor | Reorganize large parts of runtime architecture immediately | |

**User's choice:** Auto-carried recommended default: introduce a focused compatibility scheduler abstraction.
**Notes:** This keeps the scope inside Phase 3 and preserves the existing service split from Phase 2.

---

## Verification strategy

| Option | Description | Selected |
|--------|-------------|----------|
| MockBukkit only | Rely entirely on unit/integration mocks | |
| MockBukkit plus Paper/Folia smoke | Keep fast regressions and add live-platform smoke evidence for compatibility claims | X |
| Manual testing only | Use docs and ad hoc servers instead of automation | |

**User's choice:** Auto-carried recommended default: MockBukkit plus Paper/Folia smoke.
**Notes:** This is the smallest credible path to prove compatibility without overbuilding infrastructure.

---

## Product boundary during compatibility work

| Option | Description | Selected |
|--------|-------------|----------|
| Let examples drive product wording | Keep using skin-focused wording because examples still use skin commands | |
| Preserve general menu-engine positioning | Compatibility work must not let bundled examples redefine the product | X |
| Rebrand Phase 3 around platform-specific skin support | Turn compatibility into skin-plugin hardening | |

**User's choice:** Auto-carried recommended default: preserve the general menu-engine positioning.
**Notes:** This carries forward locked Phase 1 and Phase 2 decisions.

---

## the agent's Discretion

- Exact class names and package layout for the scheduler abstraction
- Whether startup diagnostics land in code, docs, or both
- The precise split between automated smoke checks and documented manual follow-up

## Deferred Ideas

- Pagination and async data menus belong to Phase 4
- Conditions and richer bindings belong to Phase 4
- Any GUI editor remains out of scope
