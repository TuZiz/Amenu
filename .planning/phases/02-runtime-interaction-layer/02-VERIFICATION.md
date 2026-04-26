---
phase: 02
slug: runtime-interaction-layer
status: passed
verified: 2026-04-02
requirements: [RUN-01, RUN-02, RUN-03, INP-01, INP-02, INP-03, INP-04]
---

# Phase 02 Verification

## Verdict

Phase 2 is complete. The codebase now has:

- automated runtime regressions for navigation, action-chain side effects, permission denial, and chat-input lifecycle behavior
- bundled showcase menus that expose runtime interactions without bloating `main.yml`
- a parser fix for inline action descriptors such as `[open history]` and `[prompt reusable]`

No Phase 3 platform abstraction or Phase 4 feature expansion was required to satisfy the Phase 2 goal.

## Goal Check

### 1. Runtime interaction behavior is executable and regression-protected

Passed.

Evidence:

- `src/test/kotlin/cc/keer/amenu/runtime/MenuRuntimeActionTest.kt` verifies:
  - open/back/refresh/close navigation flow
  - player/console/message/sound side effects
  - action-chain order for observable command side effects
  - permission denial with local `deny-actions` and fallback system messaging
- `src/test/kotlin/cc/keer/amenu/service/ChatInputServiceTest.kt` verifies:
  - reusable prompts
  - inline input
  - `{input}` injection into submit actions
  - cancel flow
  - prompt replacement
  - timeout cleanup

### 2. Bundled examples expose runtime behavior without expanding the canonical path

Passed.

Evidence:

- `src/main/resources/menus/main.yml` remains the shortest canonical menu example.
- `src/main/resources/menus/history.yml` now routes to the runtime showcase and preserves back-stack behavior.
- `src/main/resources/menus/admin.yml` demonstrates `permission`, local `deny-actions`, and `refresh`.
- `src/main/resources/menus/runtime.yml` demonstrates reusable prompts, inline input, cancel flow, and timeout guidance.
- `README.md` includes a dedicated `Runtime interactions` section that points operators to the showcase files.

### 3. Inline bracket action syntax now works as documented

Passed.

Evidence:

- `src/main/kotlin/cc/keer/amenu/config/MenuRepository.kt` now parses both `[type value]` and `[type] value` descriptor forms.
- `MenuRepositoryDslTest` now verifies that the bundled `main.yml`, `history.yml`, and `runtime.yml` files resolve `MenuAction.Open` and `MenuAction.Prompt` correctly.

## Requirement Coverage

### RUN-01

Passed.

Covered by:

- `MenuRuntimeActionTest.click_path_handles_open_back_refresh_and_close`
- `history.yml`
- `runtime.yml`

Verified behavior:

- open other menu
- back to previous menu
- refresh current menu
- close current menu

### RUN-02

Passed.

Covered by:

- `MenuRuntimeActionTest.action_chain_executes_player_console_message_and_sound_in_order`

Verified behavior:

- player command execution
- console command execution
- message side effects
- sound side effects

### RUN-03

Passed.

Covered by:

- `MenuRuntimeActionTest.permission_denial_prefers_deny_actions_before_fallback_message`
- `admin.yml`

Verified behavior:

- action chains preserve intended order for observable side effects
- permission denial prefers button-local `deny-actions`
- fallback system message still works when no local denial chain exists

### INP-01

Passed.

Covered by:

- `ChatInputServiceTest.reusable_prompt_and_inline_input_both_start_sessions`
- `runtime.yml`

Verified behavior:

- reusable prompt definitions work
- inline button `input` definitions are auto-registered and runnable

### INP-02

Passed.

Covered by:

- `ChatInputServiceTest.submit_injects_input_into_follow_up_actions`

Verified behavior:

- submitted chat text is injected into later actions through `{input}`

### INP-03

Passed.

Covered by:

- `ChatInputServiceTest.cancel_and_replace_paths_emit_expected_feedback`
- `runtime.yml`

Verified behavior:

- cancel keywords trigger cancel actions
- a second prompt replaces the active session cleanly

### INP-04

Passed.

Covered by:

- `ChatInputServiceTest.timeout_cleans_up_session_and_notifies_player`
- `runtime.yml`

Verified behavior:

- timeout removes the active prompt session
- timeout feedback is delivered safely

## Scope Guardrail Check

Passed.

The Phase 2 completion does not rely on:

- Folia scheduler abstraction
- async data-backed pagination
- condition system
- variable/binding expansion
- new runtime action categories

These remain Phase 3 or Phase 4 work.

## Automated Evidence

Executed under Java 21:

- `mvn "-Dtest=MenuRuntimeActionTest,ChatInputServiceTest" test`
- `mvn test`
- `mvn package`

Observed result:

- targeted runtime tests passed
- full suite of 11 tests passed
- package build succeeded

## Notes

- The runtime parser bug for inline bracket descriptors was discovered during Phase 2 execution and fixed inside Phase 2 because it directly blocked runtime navigation and prompt behavior.
- The shaded build still emits the existing overlap warning for `META-INF/MANIFEST.MF`, but packaging succeeds and this warning is unchanged from earlier packaging.
- The workspace still has no Git commits; verification is based on the current filesystem state and successful Maven execution.

## Human Verification

Completed by repository inspection:

- `README.md` runtime interactions section
- `src/main/resources/menus/history.yml`
- `src/main/resources/menus/admin.yml`
- `src/main/resources/menus/runtime.yml`

