# AMenu Compatibility Smoke

This directory closes the compatibility proof boundary for Phase 3.

## Automated Proof

These checks are expected to run locally under Java 21 and do not require a live server:

```powershell
$env:JAVA_HOME = "$PWD\.tools\jdk21"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn "-Dtest=BundledMenuCompatibilityTest,AMenuCommandTest,MenuRepositoryDslTest" test
mvn package
jar tf target/amenu-1.0.0-SNAPSHOT-shaded.jar | Select-String 'net/kyori/adventure/text/minimessage/MiniMessage.class'
```

Automated proof covers:

- bundled menu regressions for `main`, `history`, `admin`, and `runtime`
- `/amenu`, `/amenu open <menuId>`, and `/amenu reload`
- Java 21 packaging of the shaded plugin jar
- presence of `MiniMessage` inside `target/amenu-1.0.0-SNAPSHOT-shaded.jar`

Automated proof does not cover live platform ownership rules or player interaction on Paper/Folia.

## Manual Smoke Boundary

Live smoke is still required on both Paper and Folia because MockBukkit does not prove:

- runtime bootstrap against a real server jar
- reflective scheduler selection on Folia
- player-bound prompt cleanup on live platform threads

Use the scripts in this directory after `mvn package` succeeds.

## Paper Smoke

```powershell
.smoke\paper-smoke.ps1 -ServerDir D:\servers\paper-smoke -ServerJar paper-1.20.6-151.jar
```

Expected Paper result:

- `target/amenu-1.0.0-SNAPSHOT-shaded.jar` is copied to `plugins\AMenu.jar`
- the server reaches `Done (` in the log
- the log contains `Enabling AMenu`
- the log does not contain `NoClassDefFoundError`
- the log does not contain scheduler reflection failures or prompt cleanup exceptions

## Folia Smoke

```powershell
.smoke\folia-smoke.ps1 -ServerDir D:\servers\folia-smoke -ServerJar folia-1.20.6-6.jar
```

Expected Folia result:

- `target/amenu-1.0.0-SNAPSHOT-shaded.jar` is copied to `plugins\AMenu.jar`
- the server reaches `Done (` in the log
- the log contains `Enabling AMenu`
- the log does not contain `NoClassDefFoundError`
- the log does not contain scheduler reflection failures or prompt cleanup exceptions

## Suggested Live Checks

After startup smoke, verify the real interaction boundary manually:

1. Open `/amenu` on Paper and confirm the bundled menus still navigate.
2. Start one prompt in `runtime.yml`, submit or cancel it, then reload/stop the server.
3. Repeat the same flow on Folia and confirm the server log stays clean.
