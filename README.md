# AMenu

AMenu is a general-purpose Minecraft menu plugin for Spigot, Paper, and Folia-aware execution. It is not a skin-only plugin. `skin menus are bundled examples only`.

## Commands

- `/amenu`
- `/amenu open <menuId>`
- `/amenu reload`
- Compatibility alias: `/skinmenu`

## Canonical DSL

The shortest recommended path is:

- `layout`
- `fill`
- `buttons`
- `head`
- `click`
- inline `input`

Advanced layers such as `templates`, `prompts`, `permission`, `visible-permission`, and `deny-actions` are still supported, but they are optional expansion layers rather than the default writing style.

```yaml
title: "<gradient:#58A6FF:#F778BA><bold>Server Skin Center</bold></gradient>"

layout:
  - "#########"
  - "##H#C#L##"
  - "#########"

fill:
  material: WHITE_STAINED_GLASS_PANE
  name: " "

buttons:
  "C":
    head: "base64..."
    glow: true
    name: "<aqua><bold>Change your skin</bold></aqua>"
    click:
      - "[close]"
    input:
      start:
        - "<yellow>Type a premium player ID.</yellow>"
      cancel:
        - "[message] <yellow>Cancelled.</yellow>"
        - "[back]"
      submit:
        - "[player] skin set {input}"
        - "[back]"

  "H":
    head: "base64..."
    name: "<light_purple><bold>History / public skins</bold></light_purple>"
    click:
      - "[open history]"

  "L":
    material: PAPER
    name: "<green><bold>Open skin list</bold></green>"
    click:
      - "[close]"
      - "[player] skins"
```

## Runtime interactions

`main.yml` stays intentionally short. Richer runtime behavior is documented through the bundled secondary examples:

- `history.yml` shows secondary navigation and back-stack behavior.
- `admin.yml` shows permission checks, local `deny-actions`, and refresh behavior.
- `runtime.yml` shows reusable prompts, inline `input`, cancel flow, and timeout guidance.

This split keeps the first configuration example short while still making the runtime interaction layer discoverable.

## Compatibility

AMenu's Phase 3 compatibility contract is:

- `Spigot`: supported by the same shaded plugin jar and command surface.
- `Paper`: covered by automated regressions plus live startup smoke.
- `Folia`: supported through a Folia-aware scheduler layer, with automated regression coverage and a documented live smoke boundary.

The proof boundary is explicit:

- Automated proof: `BundledMenuCompatibilityTest`, `AMenuCommandTest`, `MenuRepositoryDslTest`, `mvn package`, and shaded jar inspection for `MiniMessage.class`.
- Manual smoke: Paper/Folia server startup plus one real `/amenu` prompt/navigation pass using `.smoke/README.md`, `.smoke/paper-smoke.ps1`, and `.smoke/folia-smoke.ps1`.

`main.yml` remains the shortest canonical example for the product. `history.yml`, `admin.yml`, and `runtime.yml` remain bundled examples that demonstrate navigation, permissions, refresh, and prompt flows without redefining AMenu as a skin-only plugin.

## Current capabilities

- Inventory GUI menus
- `[close]`, `[open menuId]`, `[back]`, `[refresh]`
- `[player]`, `[console]`, `[message]`, `[sound]`
- Reusable `prompts` and inline button `input`
- `permission`, `visible-permission`, `deny-actions`
- Bundled examples: `main`, `history`, `admin`, `runtime`

## Build

Java 21 is required. The current `pom.xml` enforces `[21,22)`, so a default Java 25 runtime will fail validation.

```powershell
mvn package
```

The packaged jar is written to `target/`.
