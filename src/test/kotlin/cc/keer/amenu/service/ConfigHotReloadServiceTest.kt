package cc.keer.amenu.service

import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ConfigHotReloadServiceTest : MenuPluginTestHarness() {

    @Test
    fun saving_menu_file_is_reloaded_automatically() {
        plugin.configHotReloadService.start()
        val menuFile = File(plugin.dataFolder, "menus/auto-reload.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Auto One"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: PAPER
                name: "First"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )

        waitForMenuButtonMaterial("auto-reload", 'A', "PAPER")
        openMenu("auto-reload")
        assertEquals("PAPER", currentItem('A')!!.type.name)

        menuFile.writeText(
            """
            title: "Auto Two"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: EMERALD
                name: "Second"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        menuFile.setLastModified(System.currentTimeMillis() + 2_000)

        waitForMenuButtonMaterial("auto-reload", 'A', "EMERALD")
        openMenu("auto-reload")
        assertEquals("EMERALD", currentItem('A')!!.type.name)
    }

    @Test
    fun changing_only_button_content_repaints_existing_inventory_without_reopen() {
        plugin.configHotReloadService.start()
        val menuFile = File(plugin.dataFolder, "menus/paint-only.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Paint Only"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: PAPER
                name: "First"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )

        waitForMenuButtonMaterial("paint-only", 'A', "PAPER")
        openMenu("paint-only")
        val sameInventory = player.openInventory.topInventory
        assertEquals("PAPER", currentItem('A')!!.type.name)

        menuFile.writeText(
            """
            title: "Paint Only"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: EMERALD
                name: "Second"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        menuFile.setLastModified(System.currentTimeMillis() + 2_000)

        waitForMenuButtonMaterial("paint-only", 'A', "EMERALD")
        assertSame(sameInventory, player.openInventory.topInventory)
        assertEquals("EMERALD", currentItem('A')!!.type.name)
    }

    @Test
    fun invalid_saved_menu_keeps_previous_loaded_version() {
        plugin.configHotReloadService.start()
        val menuFile = File(plugin.dataFolder, "menus/auto-safe.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Safe Menu"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: DIAMOND
                name: "Stable"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )

        waitForMenuButtonMaterial("auto-safe", 'A', "DIAMOND")
        openMenu("auto-safe")
        assertEquals("DIAMOND", currentItem('A')!!.type.name)

        menuFile.writeText("Title: broken\nShape:\n  - \"`未闭合\"\n", Charsets.UTF_8)

        advanceTicks(25)
        openMenu("auto-safe")
        assertNotNull(currentItem('A'))
        assertEquals("DIAMOND", currentItem('A')!!.type.name)
    }

    @Test
    fun invalid_reload_batch_keeps_previous_loaded_menus_atomically() {
        val stableFile = File(plugin.dataFolder, "menus/batch-stable.yml")
        stableFile.parentFile.mkdirs()
        stableFile.writeText(
            """
            title: "Batch Stable"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: PAPER
                name: "Stable"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )

        val initialReport = plugin.menuRepository.applyFileChanges(setOf(stableFile), emptySet())
        assertTrue(initialReport.applied)
        assertEquals("PAPER", menuButtonMaterial("batch-stable", 'A'))
        val invalidFile = File(plugin.dataFolder, "menus/batch-invalid.yml")
        plugin.menuService.openMenu(player, "batch-stable")
        val sameInventory = player.openInventory.topInventory

        stableFile.writeText(
            """
            title: "Batch Stable"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: EMERALD
                name: "Changed"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        invalidFile.writeText("Title: broken\nShape:\n  - \"`未闭合\"\n", Charsets.UTF_8)
        val report = plugin.menuRepository.applyFileChanges(setOf(stableFile, invalidFile), emptySet())

        assertFalse(report.applied)
        assertTrue(report.errors.isNotEmpty())
        assertSame(sameInventory, player.openInventory.topInventory)
        assertEquals("PAPER", menuButtonMaterial("batch-stable", 'A'))
        assertNull(plugin.menuRepository.menu("batch-invalid"))
    }

    @Test
    fun template_folder_menus_are_loaded_and_unloaded_like_normal_menus() {
        plugin.configHotReloadService.start()
        val menuFile = File(plugin.dataFolder, "menus/templates/live.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Template Live"
            Shape:
              - "#########"
              - "#A#######"
              - "#########"
            buttons:
              "A":
                material: PAPER
                name: "Loaded"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )

        waitForMenuButtonMaterial("live", 'A', "PAPER")
        openMenu("live")
        assertEquals("templates/live", currentMenuId())
        assertEquals("PAPER", currentItem('A')!!.type.name)

        assertTrue(menuFile.delete(), "Menu file should be deletable")
        assertFalse(menuFile.exists())
        advanceTicks(60)

        assertNull(plugin.menuRepository.menu("live"))
        assertNull(currentMenuId())
    }

    @Test
    fun updated_open_menu_reopens_when_shape_height_changes() {
        plugin.configHotReloadService.start()
        val menuFile = File(plugin.dataFolder, "menus/shape-hot.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Shape Hot"
            Shape:
              - "#A#######"
            buttons:
              "A":
                material: PAPER
                name: "Short"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )

        waitForMenuButtonMaterial("shape-hot", 'A', "PAPER")
        openMenu("shape-hot")
        assertEquals(9, player.openInventory.topInventory.size)
        assertEquals("PAPER", currentItem('A')!!.type.name)

        menuFile.writeText(
            """
            title: "Shape Hot"
            Shape:
              - "#########"
              - "#A#######"
              - "#########"
            buttons:
              "A":
                material: EMERALD
                name: "Tall"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        menuFile.setLastModified(System.currentTimeMillis() + 2_000)

        waitForMenuButtonMaterial("shape-hot", 'A', "EMERALD")
        assertEquals("shape-hot", currentMenuId())
        assertEquals(27, player.openInventory.topInventory.size)
        assertEquals("EMERALD", currentItem('A')!!.type.name)
    }

    private fun waitForMenuButtonMaterial(
        menuId: String,
        symbol: Char,
        material: String,
        maxTicks: Int = 160,
    ) {
        var elapsed = 0
        while (elapsed <= maxTicks) {
            if (menuButtonMaterial(menuId, symbol) == material) {
                return
            }
            advanceTicks(5)
            elapsed += 5
        }
        assertEquals(material, menuButtonMaterial(menuId, symbol))
    }

    private fun menuButtonMaterial(menuId: String, symbol: Char): String? {
        val menu = plugin.menuRepository.menu(menuId) ?: return null
        val slot = menu.slotsFor(symbol).firstOrNull() ?: return null
        return menu.buttonAt(slot)?.icon?.materialName
    }
}
