package cc.keer.amenu.service

import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MenuServiceCompatibilityTest : MenuPluginTestHarness() {

    @BeforeEach
    fun installCompatibilityMenus() {
        writeMenu(
            "main",
            """
            title: "Main"
            layout:
              - "#########"
              - "#N#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "N":
                material: BOOK
                name: "Next"
                click:
                  - "[open compat-next]"
            """,
        )
        writeMenu(
            "compat-next",
            """
            title: "Compat Next"
            layout:
              - "#########"
              - "#########"
              - "#########"
            fill:
              material: BLACK_STAINED_GLASS_PANE
              name: " "
            """,
        )
        plugin.config.set("menus.default", "main")
        plugin.saveConfig()
        plugin.reloadPlugin()
    }

    @Test
    fun open_default_menu_still_succeeds_after_scheduler_handoff() {
        plugin.menuService.openDefaultMenu(player)

        assertEquals("main", currentMenuId())
    }

    @Test
    fun click_actions_continue_to_open_secondary_menus() {
        plugin.menuService.openDefaultMenu(player)
        clickCurrent('N')

        assertEquals("compat-next", currentMenuId())
    }
}
