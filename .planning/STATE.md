---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: Executing Phase 05
last_updated: "2026-04-03T09:56:56.358Z"
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 11
  completed_plans: 10
---

# STATE

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Let server owners build modern Minecraft menus with shorter, clearer, and more maintainable configuration than TRMenu-style setups.  
**Current focus:** Phase 05 — page-provider-placeholderapi

## Status

- Project initialized as greenfield
- Git repository initialized
- Phase 1 implemented and verified
- Phase 2 implemented and verified
- Phase 3 implemented and automation-verified
- Default DSL, README, plugin metadata, and `/amenu` entrypoint are aligned
- Runtime regressions now cover navigation, permission denial, prompt lifecycle behavior, and bundled compatibility flows
- Compatibility closeout now includes Java 21 packaging proof, explicit MiniMessage shading checks, and documented Paper/Folia smoke scripts
- Live verification exposed an inventory-title signature mismatch on one server runtime; the menu open path now falls back safely when the `Component` inventory overload is unavailable
- Phase 4 context is now captured for modernization work: pagination, async lists, conditions, richer bindings, and stronger showcase menus
- Phase 4 planning is now complete with separate execution slices for pagination/async menus and conditions/richer bindings
- Phase 4 wave 1 is now implemented: menus can declare paged `pages` regions, page navigation actions, delayed list-backed rendering, and a bundled pagination showcase
- Phase 4 wave 2 is now implemented: menus can declare reusable conditions, button states, and item-triggered bindings with contextual placeholders
- Phase 5 context is now captured for registered providers, PlaceholderAPI bridging, cache-aware refresh behavior, and stateful button rendering
- Phase 5 plan 01 is now implemented: provider/cache/update DSL metadata and one shared placeholder pipeline are in place, with PlaceholderAPI kept optional and non-fatal
- Phase 5 plan 02 is now implemented: page regions render through a registered provider runtime with viewer/menu/surface cache scoping, stale-last-good fallback, and scheduler-safe rerender

## Current Position

Phase: 05 (page-provider-placeholderapi) — EXECUTING
Plan: 3 of 3

- Completed: Phase 1 - Menu DSL Foundation
- Completed: Phase 2 - Runtime Interaction Layer
- In verification: Phase 3 - Platform Compatibility Layer
- Implemented: Phase 4 - Advanced Menu Features
- In progress: Phase 5 - Dynamic data and placeholder pipeline
- Next logical steps: execute the timed reevaluation and PlaceholderAPI smoke plan while preserving the outstanding Phase 3 live UAT boundary

## Next

1. Execute `05-03-PLAN.md` to add timed button reevaluation, cleanup, and PlaceholderAPI-enabled smoke verification on top of the provider runtime
2. Preserve the outstanding Phase 3 live UAT boundary until one real `/amenu` Paper or Folia interaction pass is complete
3. Close Phase 5 only after the timed refresh lifecycle and optional PlaceholderAPI smoke path are both documented and verified

## Decisions

- Made the shaded artifact classifier explicit so MiniMessage packaging proof is intentional and stable.
- Published compatibility with an explicit automated-vs-manual proof boundary so Paper/Folia claims stay testable without overclaiming live coverage.
- Paper and Folia startup smoke both reached `Loading/Enabling AMenu` and `Done`, but a live player interaction pass is still pending.
- Real server testing uncovered a missing `Bukkit.createInventory(..., Component)` signature on one runtime; the rebuilt jar now uses reflection-first dispatch with a legacy-title fallback and is awaiting in-game confirmation.
- The next feature-track request was explicitly about "more modern menus", so Phase 4 discussion has been opened without pretending Phase 3 human UAT is complete.
- Phase 4 has now been split into two executable plan slices: pagination/async list modernization first, then conditions and richer bindings.
- Phase 4 wave 1 ships a single-file paginated region DSL (`pages`) so showcase menus no longer need one YAML file per page.
- Phase 4 wave 2 adds a shared condition model plus item bindings, so menus can react to permissions and contextual openings without cloning whole files.
- [Phase 05]: Keep MenuRepository as the single parser seam for provider/cache/update/loading-empty-error metadata.
- [Phase 05]: Resolve AMenu {..} placeholders before delegating %..% to PlaceholderAPI so internal placeholders stay canonical.
- [Phase 05]: Evaluate button-state conditions and rendered text through the same PlaceholderPipeline used by actions and bindings.
- [Phase 05-page-provider-placeholderapi]: Use a central ProviderCache keyed by viewerId + menuId + surfaceId + providerType so refresh can invalidate one surface without cross-player leakage.
- [Phase 05-page-provider-placeholderapi]: Register built-in entries and placeholder-state providers, with legacy-static aliases routed through entries to preserve Phase 4 page behavior.
- [Phase 05-page-provider-placeholderapi]: Pre-sync dynamic page surfaces before slot rendering so provider-backed placeholders can drive button state selection in the same render pass.

## Issues

- Global shell execution defaulted to Java 25, so Maven verification for this phase must use the repo-local Java 21 toolchain under `.tools/jdk21`.
- The smoke scripts currently treat slow server shutdown as a hard failure even when startup verification already passed; log review was needed to confirm successful enablement.
- Phase 3 human UAT is still outstanding, so compatibility claims remain automation-backed plus startup smoke until one real in-game pass is completed.

## Accumulated Context

### Roadmap Evolution

- Phase 5 added: dynamic data and placeholder pipeline with pluggable page providers, PlaceholderAPI support, cache, and refresh strategy
- Phase 5 context captured: registered providers, PlaceholderAPI soft-bridge, player-scoped cache, timed reevaluation, and reward-style dynamic button states

## Session

- Last completed plan: `05-02-PLAN.md`
- Stopped at: Completed 05-02-PLAN.md

---
*Last updated: 2026-04-03 after Phase 5 plan 02 execution*
