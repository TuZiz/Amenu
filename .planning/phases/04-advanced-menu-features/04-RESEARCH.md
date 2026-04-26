# Phase 4 Research: Advanced Menu Features

**Date:** 2026-04-02  
**Phase:** `04-advanced-menu-features`

## Research Goal

Turn the user's "menus should feel more modern" request into a concrete implementation direction that stays aligned with AMenu's existing strengths:
- low-config YAML
- bundled showcase menus
- runtime-safe execution
- Spigot/Paper/Folia-aware architecture

## Findings

### 1. Modernization should be capability-first, not syntax-first

Phase 1 already established the low-config DSL. The next visible leap is not a shorter button syntax; it is a stronger menu engine:
- paged menus instead of one-off static pages
- async-backed content instead of only bundled static items
- conditional visibility and render-state changes instead of duplicated menu files
- richer bindings and context variables instead of command-only opening paths

### 2. Pagination should be data-oriented

Neuron references from Easygui point toward list/pagination primitives rather than just "next/prev buttons":
- `ListDataSource`
- `normalizePagedSlots`
- async menu demo screens

Inference: AMenu should treat pagination as a render pattern driven by a list source and a slot template, not as hardcoded page switching logic.

### 3. Async menus must separate fetch from render

Easygui references and the current AMenu scheduler abstraction both support the same conclusion:
- data loading can happen off-thread
- Bukkit inventory mutation must return through the platform-safe runtime path

This is especially important because Phase 3 already introduced a compatibility layer. Phase 4 should build on it rather than bypass it.

### 4. Conditions should be declarative and shared

The strongest route for `ADV-02` is a small shared conditional/render evaluator that can be used by:
- button visibility
- item name/lore rendering
- action availability
- page state indicators

This avoids duplicating logic across parser, runtime service, and bundled example menus.

### 5. Showcase menus should become product labs

The bundled menu set has already shifted away from a skin-only story. Phase 4 should push that further:
- `main.yml` as a feature hub
- a pagination lab
- an async list lab
- a condition/binding lab
- the existing runtime input lab kept and integrated into the same story

## Recommended Slice

### Plan 04-01
- pagination runtime state
- slot templating for lists
- async data source hooks
- bundled pagination/async showcase

### Plan 04-02
- conditions and visible/render/action gating
- richer bindings/open triggers/context variables
- bundled condition/binding showcase

## Risks

- Hardcoding slot math directly into services will create Phase 4 sprawl quickly.
- Async rendering without a strict scheduler return path will reopen compatibility issues Phase 3 just closed.
- If conditions, bindings, and pagination each invent separate context systems, the DSL will become harder than TRMenu instead of easier.

## Research Summary

The best Phase 4 route is:
1. build a reusable paged/list runtime
2. add async data-backed rendering on top of that runtime
3. add a shared conditional/context evaluation layer
4. teach all of it through bundled feature-lab menus rather than domain-specific examples
