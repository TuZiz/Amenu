# Requirements: AMenu

**Defined:** 2026-04-02  
**Core Value:** Let server owners build modern Minecraft menus with shorter, clearer, and more maintainable configuration than TRMenu-style setups.

## v1 Requirements

### Menu Core

- [x] **MENU-01**: Server owners can define menu title, layout, button mapping, and default fill through YAML.
- [x] **MENU-02**: Server owners can use short button syntax such as `head`, `click`, and inline `input`.
- [x] **MENU-03**: Players can open the default menu or a named bundled menu through a general command surface.

### Runtime

- [x] **RUN-01**: Buttons support close, back, refresh, and open-other-menu behavior.
- [x] **RUN-02**: Buttons support player commands, console commands, messages, and sounds in action chains.
- [x] **RUN-03**: Action chains run in declaration order and support permission denial feedback.

### Input Flow

- [x] **INP-01**: Server owners can define reusable prompts and button-local inline input flows.
- [x] **INP-02**: Submitted input can be injected into follow-up actions through `{input}`.
- [x] **INP-03**: Players can cancel the current input flow with cancel keywords.
- [x] **INP-04**: Prompt sessions time out safely and notify the player.

### Compatibility

- [x] **COMP-01**: Runtime menu behavior is compatible with Spigot/Paper main-thread GUI and command boundaries.
- [x] **COMP-02**: The plugin introduces a Folia-compatible scheduling abstraction instead of assuming a single main thread.
- [x] **COMP-03**: Bundled example menus remain examples and do not redefine the product as a skin-only plugin.

## v2 Requirements

- **ADV-01**: Support pagination and async data-backed menus.
- **ADV-02**: Support conditions, placeholder-driven visibility, and richer state-based rendering.
- **ADV-03**: Support more bindings such as event-driven opening, item interaction opening, and contextual variables.

## v3 Requirements

- **P5-01**: Menus can resolve page regions through registered providers with viewer/menu/surface-scoped cache keys, TTL caching, and explicit invalidation on refresh.
- [x] **P5-02**: One shared placeholder pipeline resolves internal `{placeholder}` values across titles, lore, actions, provider params, bindings, and button-state evaluation, while `%placeholder%` resolution remains an optional PlaceholderAPI bridge.
- **P5-03**: Buttons can reevaluate state on a configured interval without reopening the menu, and refresh tasks clean up on menu close, reload, quit, and menu replacement.
- [x] **P5-04**: The DSL parses provider, cache, loading/empty/error, and update metadata without breaking existing static menus or requiring provider opt-in.
- **P5-05**: Async provider execution returns through the platform-safe runtime path before inventory mutation or visible rerender on Bukkit/Paper/Folia.
- **P5-06**: Missing PlaceholderAPI, empty provider results, provider errors, and cache-expired reloads degrade safely through explicit fallback states, with a documented PlaceholderAPI-enabled smoke path.

## Out of Scope

| Feature | Reason |
|---------|--------|
| A single business-specific plugin | AMenu is positioned as a general menu engine |
| A web visual editor | The first milestone focuses on a stable server-side engine and DSL |
| Deep coupling to one business plugin API | Lower priority than the general menu foundation |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| MENU-01 | Phase 1 | Complete |
| MENU-02 | Phase 1 | Complete |
| MENU-03 | Phase 1 | Complete |
| RUN-01 | Phase 2 | Complete |
| RUN-02 | Phase 2 | Complete |
| RUN-03 | Phase 2 | Complete |
| INP-01 | Phase 2 | Complete |
| INP-02 | Phase 2 | Complete |
| INP-03 | Phase 2 | Complete |
| INP-04 | Phase 2 | Complete |
| COMP-01 | Phase 3 | Human verification pending |
| COMP-02 | Phase 3 | Human verification pending |
| COMP-03 | Phase 3 | Human verification pending |
| ADV-01 | Phase 4 | Complete |
| ADV-02 | Phase 4 | Complete |
| ADV-03 | Phase 4 | Complete |
| P5-01 | Phase 5 | Planned |
| P5-02 | Phase 5 | Complete |
| P5-03 | Phase 5 | Planned |
| P5-04 | Phase 5 | Complete |
| P5-05 | Phase 5 | Planned |
| P5-06 | Phase 5 | Planned |

---
*Last updated: 2026-04-03 after Phase 5 plan 01 execution*
