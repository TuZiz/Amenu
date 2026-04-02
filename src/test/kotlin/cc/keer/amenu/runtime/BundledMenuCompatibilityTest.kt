package cc.keer.amenu.runtime

import cc.keer.amenu.service.NavigationMode
import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BundledMenuCompatibilityTest : MenuPluginTestHarness() {

    @Test
    fun bundled_examples_open_through_the_plugin_entrypoints() {
        val bundledMenuIds = plugin.menuRepository.listMenuIds()

        assertEquals(setOf("main", "history", "admin", "runtime"), bundledMenuIds.toSet())
        assertEquals("main", plugin.settings.defaultMenuId)
        assertNotNull(plugin.menuRepository.menu("main"))
        assertNotNull(plugin.menuRepository.menu("history"))
        assertNotNull(plugin.menuRepository.menu("admin"))
        assertNotNull(plugin.menuRepository.menu("runtime"))

        assertTrue(server.dispatchCommand(player, "amenu"))
        assertEquals("main", currentMenuId())

        clickCurrent('H')
        assertEquals("history", currentMenuId())

        clickCurrent('R')
        assertEquals("runtime", currentMenuId())

        plugin.menuService.openMenu(player, "admin", navigation = NavigationMode.ROOT)
        assertEquals("admin", currentMenuId())

        plugin.menuService.openMenu(player, "main", navigation = NavigationMode.ROOT)
        assertEquals("main", currentMenuId())

        assertTrue(server.dispatchCommand(player, "amenu open history"))
        assertEquals("history", currentMenuId())

        plugin.menuService.openMenu(player, "runtime", navigation = NavigationMode.ROOT)
        assertEquals("runtime", currentMenuId())

        assertTrue(server.dispatchCommand(player, "skinmenu"))
        assertEquals("main", currentMenuId())
    }
}
