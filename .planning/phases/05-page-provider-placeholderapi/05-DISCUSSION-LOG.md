# Phase 5: 动态数据与占位符管线：支持可插拔 page provider、PlaceholderAPI、缓存与刷新策略 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md; this log preserves the alternatives considered.

**Date:** 2026-04-03
**Phase:** 05-动态数据与占位符管线：支持可插拔 page provider、PlaceholderAPI、缓存与刷新策略
**Areas discussed:** provider model, placeholder pipeline, cache and refresh, stateful reward-button behavior, failure and degradation

---

## Provider Model

| Option | Description | Selected |
|--------|-------------|----------|
| Registered provider abstraction + small built-in provider set | Extensible runtime contract with a minimal stable starter set; avoids hardcoding each new dynamic source | Yes |
| Built-in special cases only | Faster short-term delivery but grows ad hoc branches in parser and runtime | |
| Script-first provider engine | Maximum flexibility but expands scope and complexity too early | |

**User's choice:** Use the recommended registered provider abstraction plus a small built-in provider set.
**Notes:** The goal is to unlock real dynamic menus without exploding DSL complexity or forcing scripting in Phase 5.

---

## Placeholder Pipeline

| Option | Description | Selected |
|--------|-------------|----------|
| Internal placeholders remain canonical + PlaceholderAPI soft bridge | Preserve AMenu-native `{key}` style and optionally resolve `%placeholder%` when PlaceholderAPI is installed | Yes |
| PlaceholderAPI as required core dependency | Simplifies some integrations but makes the engine depend on an external plugin | |
| Keep only internal placeholders | Safer boundary but fails the desired external-variable use case | |

**User's choice:** Use the recommended internal-placeholder-first pipeline with PlaceholderAPI as a soft dependency bridge.
**Notes:** The pasted gift-button example specifically motivates support for external placeholder variables such as `%player_name%`.

---

## Cache And Refresh

| Option | Description | Selected |
|--------|-------------|----------|
| Player-scoped cache with TTL + explicit refresh invalidation + periodic re-evaluation | Safe viewer isolation with practical dynamic refresh controls | Yes |
| No cache, always re-evaluate | Simpler semantics but expensive and noisy for repeated opens and open-menu refreshes | |
| Global shared cache by menu/provider | Efficient in some cases but risks leaking viewer-specific state | |

**User's choice:** Use the recommended player-scoped cache plus TTL, explicit invalidation, and periodic re-evaluation.
**Notes:** This directly supports menus that need to update state while open without recomputing everything every tick.

---

## Stateful Reward-Button Behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Support dynamic button states, timed reevaluation, hidden/changed claimed states, and normal action chains | Covers the practical reward/gift-card scenario without cloning menus | Yes |
| Support provider-backed pages only; leave buttons mostly static | Narrower scope, but misses the concrete use case the user wants | |
| Full syntax-level clone of another plugin | Covers the example literally, but over-constrains AMenu's canonical DSL | |

**User's choice:** Support the behavior class fully, without locking to one-for-one external DSL syntax compatibility.
**Notes:** The user supplied a concrete gift/reward example with update interval, permission-gated icons, and action switching.

---

## Failure And Degradation

| Option | Description | Selected |
|--------|-------------|----------|
| Safe degradation with explicit loading, empty, and error states | Keeps dynamic menus diagnosable and platform-safe when dependencies or providers fail | Yes |
| Silent fallback to blank slots/null rendering | Smaller surface area, but harder to diagnose and teach | |
| Hard fail menu loading on any provider bridge issue | Stricter, but too disruptive for optional integrations like PlaceholderAPI | |

**User's choice:** Use the recommended safe degradation model with explicit loading, empty, and error states.
**Notes:** Missing PlaceholderAPI or provider errors should not crash menu rendering or break ordinary static menus.

---

## the agent's Discretion

- Exact DSL key names for timed updates and provider declarations
- Whether dynamic button rendering extends `states` directly or introduces a companion structure
- The minimum built-in provider set needed to prove the abstraction

## Deferred Ideas

- Full one-to-one DSL compatibility with another menu plugin
- Script-first provider engines
- Business-plugin-specific reward or economy integrations
