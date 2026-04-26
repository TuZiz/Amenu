# Phase 4: Advanced Menu Features - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md.

**Date:** 2026-04-02
**Phase:** 04-advanced-menu-features
**Mode:** auto-routed from `$gsd-do` based on the user's request to continue the workflow and make menus more modern
**Areas discussed:** modernization direction, showcase strategy, advanced capability boundary, async/state guardrails

---

## Route Selection

| Option | Description | Selected |
|--------|-------------|----------|
| Finish only Phase 3 UAT | Continue purely on compatibility closeout | |
| Start Phase 4 discussion | Lock the next feature direction for "more modern" menus | X |
| Add a backlog note only | Capture idea without entering phase workflow | |

**User's intent:** Continue the process and push menu capabilities toward a more modern product surface.
**Notes:** Phase 3 human verification remains pending, but the user's request clearly points at the next feature phase rather than another compatibility-only pass.

---

## Modernization Direction

| Option | Description | Selected |
|--------|-------------|----------|
| Add coherent advanced systems | Pagination, async lists, conditions, bindings, and stronger showcase menus | X |
| Add miscellaneous one-off buttons | Continue feature growth without a phase theme | |
| Re-open DSL simplification only | Stay focused on shorter YAML with no major new systems | |

**Notes:** "Modern" is interpreted as product capability plus better teaching surface, not just prettier wording.

---

## Showcase Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Feature-hub product showcase | Use bundled menus to teach navigation, inputs, pagination, async data, and conditions | X |
| Business-specific examples | Keep default menus centered on one domain use case such as skins | |
| Minimal examples only | Avoid richer bundled menus and leave advanced features mostly undocumented | |

**Notes:** The current direction already moved bundled menus toward feature labs; Phase 4 should continue that path.

---

## Architecture Guardrail

| Option | Description | Selected |
|--------|-------------|----------|
| Keep parser/runtime/state separated | Advanced features must not collapse YAML parsing and runtime logic together | X |
| Hardcode advanced flows in services | Move faster now, refactor later | |

**Notes:** Existing project memory and neuron preflight both flagged hardcoded slot/runtime sprawl as a known risk once pagination and async features arrive.

---

## Async And Compatibility Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Async fetch, sync render | Data can load off-thread, but inventory state must return through platform-safe execution | X |
| Free-form async GUI mutation | Let async tasks update menus directly | |

**Notes:** This follows the current Phase 3 compatibility abstraction and keeps Folia-safe planning plausible.

---

## Deferred During Discussion

- Do not choose final DSL key names yet.
- Do not decide exact plan slicing yet.
- Do not claim new advanced features are live-verified on Paper/Folia until Phase 4 verification exists.

