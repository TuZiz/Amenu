# Roadmap: AMenu

## Overview

AMenu first establishes a shorter canonical menu DSL and a unified command entrypoint, then hardens the runtime interaction layer, then moves into Spigot/Paper/Folia compatibility, and finally expands into advanced features that go beyond TRMenu. Skin menus remain bundled examples only; the product itself is a general Minecraft menu engine.

## Phases

- [x] **Phase 1: Menu DSL Foundation** - Canonical DSL, `/amenu`, and first-screen product alignment
- [x] **Phase 2: Runtime Interaction Layer** - Runtime actions, prompt lifecycle, back stack, refresh, and permission feedback
- [x] **Phase 3: Platform Compatibility Layer** - Spigot/Paper compatibility hardening and Folia scheduling abstraction
- [ ] **Phase 4: Advanced Menu Features** - Pagination, conditions, async lists, and richer bindings

## Phase Details

### Phase 1: Menu DSL Foundation

**Goal**: Establish a shorter YAML DSL, a unified `/amenu` entrypoint, and clear general menu-plugin positioning.  
**Depends on**: Nothing  
**Requirements**: MENU-01, MENU-02, MENU-03  
**Status**: Complete (2026-04-02)

Plans:
- [x] 01-01: Canonical DSL path, Java 21 harness, and parser regression baseline
- [x] 01-02: Public command surface, plugin metadata, and README alignment

### Phase 2: Runtime Interaction Layer

**Goal**: Turn runtime interaction behavior into a tested and teachable contract rather than a parser-only feature set.  
**Depends on**: Phase 1  
**Requirements**: RUN-01, RUN-02, RUN-03, INP-01, INP-02, INP-03, INP-04  
**Status**: Complete (2026-04-02)

Plans:
- [x] 02-01: Runtime regression harness, action-chain verification, and prompt lifecycle verification
- [x] 02-02: Bundled runtime showcase menus and runtime interaction docs

### Phase 3: Platform Compatibility Layer

**Goal**: Harden the plugin for Spigot/Paper and introduce a maintainable Folia compatibility abstraction.  
**Depends on**: Phase 2  
**Requirements**: COMP-01, COMP-02, COMP-03
**Status**: Complete (2026-04-02)

Plans:
- [x] 03-01: Platform scheduler abstraction and main-thread/region-safe execution model
- [x] 03-02: Compatibility verification for bundled examples and input flows

### Phase 4: Advanced Menu Features

**Goal**: Add advanced capabilities after the general runtime and compatibility foundation is stable.  
**Depends on**: Phase 3  
**Requirements**: ADV-01, ADV-02, ADV-03

Plans:
- [ ] 04-01: Pagination and async data menus
- [ ] 04-02: Conditions and advanced bindings

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Menu DSL Foundation | 2/2 | Complete | 2026-04-02 |
| 2. Runtime Interaction Layer | 2/2 | Complete | 2026-04-02 |
| 3. Platform Compatibility Layer | 2/2 | Complete | 2026-04-02 |
| 4. Advanced Menu Features | 0/2 | Not started | - |

---
*Last updated: 2026-04-02 after Phase 3 completion*
