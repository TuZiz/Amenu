# Phase 2: Runtime Interaction Layer - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md.

**Date:** 2026-04-02
**Phase:** 02-runtime-interaction-layer
**Mode:** auto-carried-forward from verified Phase 1 outputs and current project direction
**Areas discussed:** runtime boundary, input lifecycle, navigation semantics, permission feedback, example strategy

---

## Runtime Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Stabilize existing runtime capabilities | 收敛已有动作链、输入流、权限反馈与导航语义 | ✓ |
| Add new action families | 在本阶段新增更多动作类型 | |
| Expand into advanced features | 把分页/条件/异步列表并入本阶段 | |

**User's choice:** Continue with the recommended path from Phase 1 completion.
**Notes:** Phase 2 被自动解释为“把已有运行时层做稳”，不是扩 scope。

---

## Input Lifecycle

| Option | Description | Selected |
|--------|-------------|----------|
| Treat input lifecycle as first-class runtime behavior | 重点收敛提交、取消、替换、超时与 `{input}` 注入 | ✓ |
| Discuss syntax again | 重新讨论 prompts / inline input 的 DSL 写法 | |

**User's choice:** Continue with the recommended next step.
**Notes:** Phase 1 已锁定 DSL，Phase 2 只继续讨论运行时生命周期。

---

## Navigation And Permission

| Option | Description | Selected |
|--------|-------------|----------|
| Formalize back/refresh/deny semantics | 用测试与示例菜单验证返回栈、刷新、权限拒绝反馈 | ✓ |
| Keep them as implicit implementation details | 不专门在本阶段验证 | |

**User's choice:** Continue with the recommended path.
**Notes:** 当前项目记忆里已沉淀 `paper-menu-navigation-and-permission-pattern`，因此延续该方向。

---

## Example Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Keep main.yml minimal and move runtime demos elsewhere | 默认示例继续最短，运行时演示放到 history/admin 或新示例 | ✓ |
| Put runtime showcase back into main.yml | 让首屏重新变长 | |

**User's choice:** Continue with the recommended path.
**Notes:** 这与用户之前明确指出“默认示例不要复杂”保持一致。

---

## the agent's Discretion

- Runtime feature demos should live in `history.yml`, `admin.yml`, or a dedicated runtime example.
- Tests can be split by runtime concern as long as MockBukkit remains the baseline.

## Deferred Ideas

- Pagination and async list menus
- Condition system and variable/binding expansion
- Formal Folia scheduler abstraction

