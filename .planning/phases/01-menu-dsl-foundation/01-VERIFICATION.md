---
phase: 01
slug: menu-dsl-foundation
status: passed
verified: 2026-04-02
requirements: [MENU-01, MENU-02, MENU-03]
---

# Phase 01 Verification

## Verdict

Phase 1 is complete. The codebase now has:

- a shorter canonical YAML DSL for the default bundled menu
- a single primary `/amenu` command surface with one compatibility alias
- automated regression tests covering DSL parsing and named-menu opening

No Phase 2, Phase 3, or Phase 4 scope was required to satisfy the Phase 1 goal.

## Goal Check

### 1. Shorter YAML DSL

Passed.

Evidence:

- `src/main/resources/menus/main.yml` now uses the compact layout:
  - `#########`
  - `##H#C#L##`
  - `#########`
- The default example demonstrates only:
  - `layout`
  - `fill`
  - `buttons`
  - `head`
  - `click`
  - inline `input`
- The previous first-screen admin/refresh showcase is no longer part of `main.yml`.

### 2. Unified `/amenu` entrypoint

Passed.

Evidence:

- `src/main/resources/plugin.yml` declares `amenu` as the primary command.
- `src/main/resources/plugin.yml` keeps only `aliases: [skinmenu]`.
- `src/main/kotlin/cc/keer/amenu/command/AMenuCommand.kt` still exposes:
  - default open
  - `open <menuId>`
  - `reload`
- `README.md` documents `/amenu`, `/amenu open <menuId>`, and `/amenu reload`.

### 3. Bundled example remains runnable

Passed.

Evidence:

- `AMenuPlugin` still bootstraps bundled menu resources on enable.
- `MenuRepository` still parses `main.yml` through the current YAML path.
- `AMenuCommandTest` opens both the default menu and the `history` menu through MockBukkit.

## Requirement Coverage

### MENU-01

Passed.

Covered by:

- `MenuRepository.kt`
- `menus/main.yml`
- `MenuRepositoryDslTest.loads_core_layout_fill_and_buttons`

Verified behavior:

- title, layout, rows, fill button, and button mapping are loaded from YAML

### MENU-02

Passed.

Covered by:

- `MenuRepository.kt`
- `menus/main.yml`
- `MenuRepositoryDslTest.parses_shorthand_head_click_and_inline_input`

Verified behavior:

- `head` shorthand resolves to texture
- `click` action list is parsed
- inline `input` creates prompt registration and prompt action wiring

### MENU-03

Passed.

Covered by:

- `AMenuCommand.kt`
- `plugin.yml`
- `AMenuCommandTest.opens_default_and_named_menu`

Verified behavior:

- `/amenu` opens the default bundled menu
- `/amenu open history` opens the named bundled menu

## Scope Guardrail Check

Passed.

The Phase 1 completion does not rely on any of the following as required deliverables:

- pagination
- async menu data sources
- condition system
- variable/binding system
- formal Folia scheduler abstraction

Existing advanced fields such as `templates`, `prompts`, `permission`, `visible-permission`, and `deny-actions` remain supported, but they are not being counted as new Phase 1 scope.

## Automated Evidence

Executed under Java 21:

- `mvn -Dtest=MenuRepositoryDslTest,AMenuCommandTest test`
- `mvn test`
- `mvn package`

Observed result:

- all targeted tests passed
- full test suite passed
- package build succeeded

## Notes

- MockBukkit `4.108.0` required aligning the Paper API to the version declared in the MockBukkit manifest: `1.21.11-R0.1-SNAPSHOT`.
- `AMenuPlugin` had to be marked `open` because MockBukkit proxies the plugin class through ByteBuddy.
- The workspace still has no Git commits; verification is based on the current filesystem state and successful Maven execution.

## Human Verification

None required for Phase 1 gate.

