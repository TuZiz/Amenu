---
status: partial
phase: 03-platform-compatibility-layer
source: [03-VERIFICATION.md]
started: 2026-04-02T18:26:30+08:00
updated: 2026-04-02T18:26:30+08:00
---

## Current Test

Live platform verification for Phase 3 compatibility claims.

## Tests

### 1. Paper startup smoke
expected: Server reaches `Done (`; logs show `Loading/Enabling AMenu`; no `NoClassDefFoundError`, scheduler reflection failures, or prompt cleanup exceptions.
result: passed
evidence: Ran `.smoke/paper-smoke.ps1` against `D:\codex\Amenu\.smoke\paper-bootstrap` using `paper-1.20.6-151.jar` and Java 21. Script reported a stop-cleanup issue, but `logs/latest.log` shows `Loading server plugin AMenu`, `Enabling AMenu`, `Done (8.371s)!`, and no forbidden compatibility errors.

### 2. Folia startup smoke
expected: Server reaches `Done (`; logs show `Loading/Enabling AMenu`; no `NoClassDefFoundError`, scheduler reflection failures, or prompt cleanup exceptions.
result: passed
evidence: Ran `.smoke/folia-smoke.ps1` against `D:\codex\Amenu\.smoke\folia-bootstrap` using `folia-1.20.6-6.jar` and Java 21. Script reported a stop-cleanup issue, but `logs/latest.log` shows `Loading server plugin AMenu`, `Enabling AMenu`, `Done (3.208s)!`, and no forbidden compatibility errors.

### 3. Live `/amenu` navigation and prompt pass
expected: On a real Paper/Folia player session, `/amenu` opens, bundled menus navigate correctly, one prompt submit/cancel flow completes, and reload/stop leaves logs clean.
result: pending

## Summary

total: 3
passed: 2
issues: 0
pending: 1
skipped: 0
blocked: 0

## Gaps

- A real player interaction pass is still needed for `/amenu` navigation plus one prompt submit/cancel cycle on Paper or Folia.
