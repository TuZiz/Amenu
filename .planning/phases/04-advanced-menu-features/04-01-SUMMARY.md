---
phase: 04-advanced-menu-features
plan: 01
status: completed
completed_at: 2026-04-02
---

# 04-01 Summary

## Outcome

Phase 4 wave 1 is now implemented.

AMenu can render paged list regions from a single YAML menu file through a new `pages` DSL layer. The runtime now keeps per-player page state, supports page actions such as next/previous/refresh, and safely re-renders delayed list-backed content without duplicating menus per page.

## Delivered

- Added `pages` region models and page actions in `MenuModels.kt`
- Added `pages` parsing, page-entry parsing, and page-action parsing in `MenuRepository.kt`
- Added runtime page state, per-region render resolution, delayed list refresh, and page click handling in `MenuService.kt`
- Added delayed scheduler support to `PlatformScheduler.kt` implementations
- Upgraded bundled menus:
  - `main.yml` now points to a pagination showcase
  - `history.yml` now demonstrates paged cards and delayed list refresh
  - `runtime.yml` now links the input lab to the pagination lab
- Updated README with the new pagination DSL and placeholders

## Verification

- `mvn "-Dtest=MenuRepositoryDslTest" test`
- `mvn "-Dtest=MenuRuntimeActionTest" test`
- `mvn "-Dtest=BundledMenuCompatibilityTest" test`

All three passed under the repo-local Java 21 toolchain.

## Notes

- This wave deliberately shipped delayed list-backed rendering with a safe main-thread return path. It is sufficient to showcase modern data-shaped menus without overcommitting the engine to a premature provider API.
- Phase 4 wave 2 remains open for conditions, state-driven render overrides, and richer non-command bindings.
