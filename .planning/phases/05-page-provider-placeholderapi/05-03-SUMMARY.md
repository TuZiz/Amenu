# 05-03 Summary

## Outcome

Wave 3 implementation is complete on the automated side.

- Added `DynamicRefreshController` and wired it into `MenuService` so update-driven surfaces can rerender the current inventory in place instead of reopening menus.
- Added lifecycle cleanup for inventory close, player quit, reload, and plugin disable.
- Preserved stale-last-good fallback for timed/provider refresh, while manual `page refresh` now drops stale data first so loading states are visible again.
- Refreshed bundled `showcase.yml`, `history.yml`, and `admin.yml` to demonstrate PlaceholderAPI-safe text, provider fallback surfaces, in-place refresh, and permission/state switching.
- Hardened same-menu reopen behavior by suppressing close-cleanup during inventory replacement, so binding/context placeholders are not lost mid-transition.

## Files

- `src/main/kotlin/cc/keer/amenu/service/DynamicRefreshController.kt`
- `src/main/kotlin/cc/keer/amenu/service/MenuService.kt`
- `src/main/kotlin/cc/keer/amenu/gui/MenuListener.kt`
- `src/main/kotlin/cc/keer/amenu/AMenuPlugin.kt`
- `src/main/kotlin/cc/keer/amenu/service/provider/ProviderCache.kt`
- `src/main/resources/menus/showcase.yml`
- `src/main/resources/menus/history.yml`
- `src/main/resources/menus/admin.yml`
- `src/test/kotlin/cc/keer/amenu/service/MenuDynamicRefreshTest.kt`
- `src/test/kotlin/cc/keer/amenu/runtime/BundledMenuCompatibilityTest.kt`

## Verification

Automated verification passed:

```powershell
$env:JAVA_HOME='D:\codex\Amenu\.tools\jdk21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -q "-Dtest=MenuDynamicRefreshTest,MenuRuntimeActionTest,BundledMenuCompatibilityTest" test
```

## Checkpoint

Human verification is still required for the live PlaceholderAPI smoke gate from `05-03-PLAN.md`:

1. Install PlaceholderAPI on a Paper or Folia test server with the latest AMenu jar.
2. Run `/amenu open showcase` and verify `%player_name%` resolves.
3. Keep the menu open, run `op <playerName>`, wait about 1 second, and verify the admin/timed state changes without reopening the inventory.
4. Trigger the bundled refresh action and verify only the target surface rerenders.
5. On Folia, confirm there are no scheduler/thread violations in logs.

Resume signal: reply with `approved` if the smoke passes, or send the failing step numbers plus observed behavior.
