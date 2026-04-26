package cc.keer.amenu.runtime

import cc.keer.amenu.service.NavigationMode
import cc.keer.amenu.support.MenuPluginTestHarness
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.BindingItemAccess
import cc.keer.amenu.util.TextFormatter
import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BundledMenuCompatibilityTest : MenuPluginTestHarness() {

    private fun grantOpenPermission() {
        grantPermission("amenu.open")
    }

    private fun clickMenuSlot(slot: Int) {
        plugin.menuService.handleClick(player, "menu", slot)
    }

    @Test
    fun bundled_examples_open_through_the_plugin_entrypoints() {
        grantOpenPermission()
        val menuIds = plugin.menuRepository.listMenuIds().toSet()
        assertTrue(menuIds.containsAll(setOf("menu", "pay", "showcase", "history", "admin", "runtime", "skin")))
        assertNotNull(plugin.menuRepository.menu("skin"))

        assertTrue(server.dispatchCommand(player, "amenu"))
        assertEquals("menu", currentMenuId())

        assertTrue(server.dispatchCommand(player, "amenu open showcase"))
        assertEquals("showcase", currentMenuId())

        clickCurrentSlot(9)
        assertEquals("history", currentMenuId())

        plugin.menuService.openMenu(player, "runtime", navigation = NavigationMode.ROOT)
        assertEquals("runtime", currentMenuId())
        clickCurrentSlot(10)
        assertNull(currentMenuId())
        assertNotNull(nextPlainMessage())
        assertNotNull(nextPlainMessage())

        val skinBinding = PlayerCommandPreprocessEvent(player, "/skin")
        server.pluginManager.callEvent(skinBinding)
        assertTrue(skinBinding.isCancelled)
        assertEquals("skin", currentMenuId())
    }

    @Test
    fun showcase_history_and_runtime_examples_keep_navigation_and_prompt_entrypoints_alive() {
        grantOpenPermission()
        assertTrue(server.dispatchCommand(player, "amenu open showcase"))
        assertEquals("showcase", currentMenuId())

        clickCurrentSlot(9)
        assertEquals("history", currentMenuId())

        advanceTicks(8)
        clickCurrent('I')
        assertTrue(nextPlainMessage().orEmpty().contains("async-refresh"))

        clickCurrentSlot(23)
        assertEquals("runtime", currentMenuId())

        clickCurrentSlot(10)
        assertNull(currentMenuId())
        assertNotNull(nextPlainMessage())
        assertNotNull(nextPlainMessage())

        assertTrue(server.dispatchCommand(player, "amenu open showcase"))
        assertEquals("showcase", currentMenuId())

        clickCurrentSlot(11)
        assertEquals("runtime", currentMenuId())

        assertTrue(server.dispatchCommand(player, "amenu open showcase"))
        assertEquals("showcase", currentMenuId())

        clickCurrentSlot(9)
        assertEquals("history", currentMenuId())
    }

    @Test
    fun binding_item_opens_showcase_and_reveals_binding_feedback() {
        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta!!
        BindingItemAccess.write(plugin, meta, "browser-compass")
        AdventureAccess.applyDisplayName(meta, TextFormatter.component("<aqua><bold>AMenu Browser Compass</bold></aqua>"))
        compass.itemMeta = meta

        interactWith(compass, Action.RIGHT_CLICK_AIR)
        assertEquals("showcase", currentMenuId())
        assertNotNull(player.openInventory.topInventory.getItem(12))

        clickCurrentSlot(12)
        val feedback = nextPlainMessage().orEmpty()
        assertTrue(feedback.contains("browser-compass"))
        assertTrue(feedback.contains("right_click_air"))
    }

    @Test
    fun admin_example_preserves_permission_feedback_and_runtime_notes() {
        grantOpenPermission()
        plugin.menuService.openMenu(player, "admin", navigation = NavigationMode.ROOT)
        assertEquals("admin", currentMenuId())

        clickCurrentSlot(10)
        assertTrue(nextPlainMessage().orEmpty().contains("amenu.admin"))

        clickCurrentSlot(12)
        assertTrue(nextPlainMessage().orEmpty().contains("amenu.admin"))

        plugin.menuService.openMenu(player, "admin", navigation = NavigationMode.ROOT)
        clickCurrentSlot(16)
        assertNotNull(nextPlainMessage())

        grantPermission("amenu.admin")
        plugin.menuService.openMenu(player, "admin", navigation = NavigationMode.ROOT)
        clickCurrentSlot(10)
        val grantedMessage = nextPlainMessage().orEmpty()
        assertTrue(grantedMessage.contains("browser-compass"))
        assertEquals(Material.COMPASS, player.inventory.itemInMainHand.type)

        plugin.menuService.openMenu(player, "admin", navigation = NavigationMode.ROOT)
        clickCurrentSlot(12)
        assertEquals("admin", currentMenuId())
        assertTrue(nextConsoleMessage().orEmpty().contains("AMenu"))
    }

    @Test
    fun bundled_phase5_showcase_files_and_timed_admin_state_are_present() {
        val menuFile = File(plugin.dataFolder, "menus/menu.yml")
        val showcaseFile = File(plugin.dataFolder, "menus/showcase.yml")
        val historyFile = File(plugin.dataFolder, "menus/history.yml")
        val payFile = File(plugin.dataFolder, "menus/pay.yml")
        val runtimeFile = File(plugin.dataFolder, "menus/runtime.yml")
        val skinFile = File(plugin.dataFolder, "menus/skin.yml")

        val menuText = menuFile.readText(Charsets.UTF_8)
        val showcaseText = showcaseFile.readText(Charsets.UTF_8)
        val historyText = historyFile.readText(Charsets.UTF_8)
        val payText = payFile.readText(Charsets.UTF_8)
        val runtimeText = runtimeFile.readText(Charsets.UTF_8)
        val skinText = skinFile.readText(Charsets.UTF_8)
        assertTrue(menuText.contains("menu: pay"))
        assertTrue(menuText.contains("menu: showcase"))
        assertTrue(showcaseText.contains("%player_name%"))
        assertTrue(showcaseText.contains("interval: 20"))
        assertTrue(historyText.contains("loading:"))
        assertTrue(historyText.contains("empty:"))
        assertTrue(historyText.contains("error:"))
        assertTrue(payText.contains("command: cz points wechat {input}"))
        assertTrue(runtimeText.contains("menu: history"))
        assertTrue(skinText.contains("skin set {input}"))

        plugin.menuService.openMenu(player, "admin", navigation = NavigationMode.ROOT)
        val sameInventory = player.openInventory.topInventory
        val before = player.openInventory.topInventory.contents.toList()

        grantPermission("amenu.admin")
        clickCurrentSlot(14)

        assertSame(sameInventory, player.openInventory.topInventory)
        assertTrue(before != player.openInventory.topInventory.contents.toList())
    }

    @Test
    fun bundled_test_menu_navigation_and_files_are_available() {
        grantOpenPermission()
        assertTrue(server.dispatchCommand(player, "amenu"))
        assertEquals("menu", currentMenuId())

        clickMenuSlot(15)
        advanceTicks(1)
        assertEquals("pay", currentMenuId())
        assertEquals(Material.EMERALD_BLOCK, player.openInventory.topInventory.getItem(12)?.type)

        plugin.menuService.openMenu(player, "menu", navigation = NavigationMode.ROOT)
        clickMenuSlot(25)
        advanceTicks(1)
        assertEquals("showcase", currentMenuId())
        assertEquals(Material.BOOKSHELF, player.openInventory.topInventory.getItem(9)?.type)

        val menuIds = plugin.menuRepository.listMenuIds().toSet()
        assertTrue(menuIds.containsAll(setOf("menu", "pay", "showcase", "history", "admin", "runtime", "skin")))
    }
}
