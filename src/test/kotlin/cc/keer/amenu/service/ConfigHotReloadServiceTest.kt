package cc.keer.amenu.service

import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

        advanceTicks(25)
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

        advanceTicks(40)
        openMenu("auto-reload")
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

        advanceTicks(25)
        openMenu("auto-safe")
        assertEquals("DIAMOND", currentItem('A')!!.type.name)

        menuFile.writeText("Title: broken\nShape:\n  - \"`未闭合\"\n", Charsets.UTF_8)

        advanceTicks(25)
        openMenu("auto-safe")
        assertNotNull(currentItem('A'))
        assertEquals("DIAMOND", currentItem('A')!!.type.name)
    }
}
