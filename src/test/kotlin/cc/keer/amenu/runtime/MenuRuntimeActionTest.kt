package cc.keer.amenu.runtime

import cc.keer.amenu.config.MenuAction
import cc.keer.amenu.support.MenuPluginTestHarness
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.TextFormatter
import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MenuRuntimeActionTest : MenuPluginTestHarness() {

    @BeforeEach
    fun installRuntimeMenus() {
        writeMenu(
            "runtime-root",
            """
            title: "Runtime Root"
            layout:
              - "#########"
              - "#<LLL>N##"
              - "#O#F#D#N#"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            pages:
              runtime-list:
                symbol: "L"
                async-delay: 4
                loading:
                  material: CLOCK
                  name: "Loading"
                entries:
                  alpha:
                    material: BOOK
                    name: "Alpha"
                    click:
                      - "[message] picked-alpha-page-{page}"
                  beta:
                    material: PAPER
                    name: "Beta"
                    click:
                      - "[message] picked-beta-page-{page}"
                  gamma:
                    material: COMPASS
                    name: "Gamma"
                    click:
                      - "[message] picked-gamma-page-{page}"
                  delta:
                    material: REDSTONE
                    name: "Delta"
                    click:
                      - "[message] picked-delta-page-{page}"
            buttons:
              "<":
                material: ARROW
                name: "Previous"
                click:
                  - "[page previous runtime-list]"
              ">":
                material: SPECTRAL_ARROW
                name: "Next"
                click:
                  - "[page next runtime-list]"
              "O":
                material: BOOK
                name: "Open next"
                click:
                  - "[open runtime-second]"
              "F":
                material: CLOCK
                name: "Refresh list"
                click:
                  - "[page refresh runtime-list]"
              "D":
                material: REDSTONE
                permission: amenu.denied
                deny-actions:
                  - "[message] denied-local"
                click:
                  - "[message] should-not-run"
              "N":
                material: BARRIER
                permission: amenu.fallback
                click:
                  - "[message] should-not-run"
            """,
        )
        writeMenu(
            "runtime-second",
            """
            title: "Runtime Second"
            layout:
              - "#########"
              - "#P#B#C###"
              - "#########"
            fill:
              material: BLACK_STAINED_GLASS_PANE
              name: " "
            buttons:
              "P":
                material: PAPER
                name: "Chain"
                click:
                  - "message: chain-start"
                  - "player: record-player alpha"
                  - "console: record-console beta"
                  - "delay: 2"
                  - "player: record-player gamma"
                  - "sound: ENTITY_EXPERIENCE_ORB_PICKUP:1:1.1"
              "B":
                material: ARROW
                name: "Back"
                click:
                  - "[back]"
              "C":
                material: BARRIER
                name: "Close"
                click:
                  - "[close]"
            """,
        )
        writeMenu(
            "context-menu",
            """
            title: "Context Menu"
            layout:
              - "#########"
              - "#B#S#A###"
              - "#########"
            bindings:
              browser:
                type: ITEM
                material: COMPASS
                name: "<aqua><bold>Context Browser</bold></aqua>"
                actions:
                  - RIGHT_CLICK_AIR
                placeholders:
                  binding-source: runtime-test
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "B":
                material: BARRIER
                name: "Bound only"
                conditions:
                  placeholder-equals:
                    binding-type: item
                click:
                  - "[message] bound-{binding-id}-{binding-action}"
              "S":
                material: BOOK
                name: "Static"
                click:
                  - "[message] static"
                states:
                  bound:
                    conditions:
                      placeholder-equals:
                        binding-type: item
                    material: COMPASS
                    name: "Bound state"
                    click:
                      - "[message] source-{binding-source}"
              "A":
                material: REDSTONE
                name: "Viewer"
                states:
                  admin:
                    conditions:
                      has-permission: amenu.admin
                    material: EMERALD
                    name: "Admin"
            """,
        )
    }

    @Test
    fun click_path_handles_open_back_refresh_and_close() {
        val rootMenu = requireNotNull(plugin.menuRepository.menu("runtime-root"))
        val secondMenu = requireNotNull(plugin.menuRepository.menu("runtime-second"))

        openMenu("runtime-root")
        assertEquals("runtime-root", currentMenuId())
        assertEquals(MenuAction.Open("runtime-second"), rootMenu.buttons.getValue('O').actions.single())
        assertEquals(
            MenuAction.Message("picked-alpha-page-{page}"),
            rootMenu.pageRegions.getValue("runtime-list").entries.first().actions.single(),
        )

        advanceTicks(8)
        assertEquals(Material.BOOK, currentItem('L')?.type)
        clickCurrent('L')
        assertEquals("picked-alpha-page-1", nextPlainMessage())

        clickCurrent('>')
        clickCurrent('L')
        assertEquals("picked-delta-page-2", nextPlainMessage())

        clickCurrent('<')
        val refreshedInventory = player.openInventory.topInventory
        clickCurrent('F')
        assertSame(refreshedInventory, player.openInventory.topInventory)
        clickCurrent('L')
        assertNull(nextPlainMessage())

        plugin.menuService.executeActions(player, "runtime-root", rootMenu.buttons.getValue('O').actions)
        assertEquals("runtime-second", currentMenuId())

        plugin.menuService.executeActions(player, "runtime-second", secondMenu.buttons.getValue('B').actions)
        assertEquals("runtime-root", currentMenuId())

        plugin.menuService.executeActions(player, "runtime-root", rootMenu.buttons.getValue('O').actions)
        assertEquals("runtime-second", currentMenuId())

        plugin.menuService.executeActions(player, "runtime-second", secondMenu.buttons.getValue('C').actions)
        assertNull(currentMenuId())
    }

    @Test
    fun action_chain_executes_player_console_message_and_sound_in_order() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-player", records)
        registerRecordingCommand("record-console", records)
        val secondMenu = requireNotNull(plugin.menuRepository.menu("runtime-second"))

        plugin.menuService.executeActions(player, "runtime-second", secondMenu.buttons.getValue('P').actions)

        assertEquals("chain-start", nextPlainMessage())
        assertEquals(
            listOf(
                "Tester|record-player|alpha",
                "CONSOLE|record-console|beta",
            ),
            records,
        )

        advanceTicks(2)

        assertEquals(
            listOf(
                "Tester|record-player|alpha",
                "CONSOLE|record-console|beta",
                "Tester|record-player|gamma",
            ),
            records,
        )
        assertTrue(heardSoundNames().isNotEmpty())
    }

    @Test
    fun permission_denial_prefers_deny_actions_before_fallback_message() {
        denyPermission("amenu.denied")
        denyPermission("amenu.fallback")

        openMenu("runtime-root")

        clickCurrent('D')
        assertEquals("denied-local", nextPlainMessage())

        clickCurrent('N')
        assertEquals("[AMenu] 你没有权限执行这个操作。", nextPlainMessage())
        assertNull(nextPlainMessage())
    }

    @Test
    fun conditions_and_item_bindings_drive_contextual_rendering() {
        openMenu("context-menu")
        assertNull(currentItem('B'))
        assertEquals(Material.BOOK, currentItem('S')!!.type)
        assertEquals(Material.REDSTONE, currentItem('A')!!.type)

        grantPermission("amenu.admin")
        openMenu("context-menu")
        assertEquals(Material.EMERALD, currentItem('A')!!.type)

        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta!!
        AdventureAccess.applyDisplayName(meta, TextFormatter.component("<aqua><bold>Context Browser</bold></aqua>"))
        compass.itemMeta = meta

        interactWith(compass, Action.RIGHT_CLICK_AIR)
        assertEquals("context-menu", currentMenuId())
        assertEquals(Material.COMPASS, currentItem('S')!!.type)
        assertEquals(Material.BARRIER, currentItem('B')!!.type)

        clickCurrent('S')
        assertEquals("source-runtime-test", nextPlainMessage())

        clickCurrent('B')
        assertEquals("bound-browser-right_click_air", nextPlainMessage())
    }

    @Test
    fun conditional_purchase_actions_support_check_papi_take_point_title_and_tell_aliases() {
        writeMenu(
            "purchase-menu",
            """
            title: "Purchase Menu"
            layout:
              - "#########"
              - "#F#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "F":
                material: FEATHER
                name: "Flight"
                click:
                  - condition: "check papi *{coins} >= *30"
                    actions:
                      - "take-point: 30"
                      - "console: lp user %player_name% permission set cmi.command.fly true"
                      - "title: &a&l飞行权限激活||&7购买成功"
                      - "tell: &a飞行权限购买成功"
                      - "close"
                    deny:
                      - "tell: &c星辰不足"
            """,
        )

        val records = mutableListOf<String>()
        registerRecordingCommand("points", records)
        registerRecordingCommand("lp", records)

        plugin.menuService.openMenu(
            player,
            "purchase-menu",
            placeholders = mapOf("coins" to "40"),
            navigation = cc.keer.amenu.service.NavigationMode.ROOT,
        )
        clickCurrent('F')

        assertEquals(
            listOf(
                "CONSOLE|points|take Tester 30 -s",
                "CONSOLE|lp|user %player_name% permission set cmi.command.fly true",
            ),
            records,
        )
        assertEquals("飞行权限购买成功", nextPlainMessage())
        assertNull(currentMenuId())

        records.clear()
        plugin.menuService.openMenu(
            player,
            "purchase-menu",
            placeholders = mapOf("coins" to "10"),
            navigation = cc.keer.amenu.service.NavigationMode.ROOT,
        )
        clickCurrent('F')

        assertTrue(records.isEmpty())
        assertEquals("星辰不足", nextPlainMessage())
        assertEquals("purchase-menu", currentMenuId())
    }
    @Test
    fun take_point_falls_back_to_legacy_playerpoints_command_when_points_alias_is_missing() {
        writeMenu(
            "purchase-menu-legacy",
            """
            title: "Purchase Menu"
            layout:
              - "#########"
              - "#F#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "F":
                material: FEATHER
                name: "Flight"
                click:
                  - condition: "check papi *{coins} >= *30"
                    actions:
                      - "take-point: 30"
            """,
        )

        val records = mutableListOf<String>()
        registerRecordingCommand("playerpoints", records)

        plugin.menuService.openMenu(
            player,
            "purchase-menu-legacy",
            placeholders = mapOf("coins" to "40"),
            navigation = cc.keer.amenu.service.NavigationMode.ROOT,
        )
        clickCurrent('F')

        assertEquals(listOf("CONSOLE|playerpoints|take Tester 30 -s"), records)
    }
}
