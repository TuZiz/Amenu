package cc.keer.amenu.config

import cc.keer.amenu.AMenuPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

class MenuRepositoryDslTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: AMenuPlugin

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(AMenuPlugin::class.java)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun loads_core_layout_fill_and_buttons() {
        val menu = plugin.menuRepository.menu("main")

        assertNotNull(menu)
        assertEquals(3, menu!!.rows)
        assertEquals(27, menu.size)
        assertEquals("##H#C#L##", menu.layout[1])
        assertTrue(menu.title.isNotBlank())

        val fillButton = menu.buttons['#']
        assertNotNull(fillButton)
        assertEquals("WHITE_STAINED_GLASS_PANE", fillButton!!.icon.materialName)
        assertEquals(" ", fillButton.icon.name)
        assertEquals('#', menu.buttonAt(0)?.symbol)

        val historyButton = menu.buttons['H']
        assertNotNull(historyButton)
        assertEquals('H', menu.buttonAt(11)?.symbol)

        val listButton = menu.buttons['L']
        assertNotNull(listButton)
        assertEquals('L', menu.buttonAt(15)?.symbol)
    }

    @Test
    fun parses_shorthand_head_click_and_inline_input() {
        val menu = plugin.menuRepository.menu("main")
        assertNotNull(menu)

        val changeButton = menu!!.buttons['C']
        assertNotNull(changeButton)
        assertTrue(changeButton!!.icon.texture?.isNotBlank() == true)
        assertTrue(changeButton.icon.glow)
        assertTrue(changeButton.actions.any { it is MenuAction.Close })
        assertTrue(changeButton.actions.any { it is MenuAction.Prompt && it.promptId == "button-c" })

        val prompt = menu.prompts["button-c"]
        assertNotNull(prompt)
        assertTrue(prompt!!.startMessages.isNotEmpty())
        assertTrue(prompt.cancelActions.any { it is MenuAction.Message })
        assertTrue(prompt.cancelActions.any { it is MenuAction.Back })
        assertTrue(prompt.submitActions.any { it is MenuAction.PlayerCommand && it.command == "skin set {input}" })

        val historyButton = menu.buttons['H']
        assertNotNull(historyButton)
        assertTrue(historyButton!!.actions.any { it is MenuAction.Open && it.menuId == "history" })
    }

    @Test
    fun loads_bundled_runtime_showcase_menus() {
        assertEquals(setOf("main", "history", "admin", "runtime"), plugin.menuRepository.listMenuIds().toSet())

        val history = plugin.menuRepository.menu("history")
        val admin = plugin.menuRepository.menu("admin")
        val runtime = plugin.menuRepository.menu("runtime")

        assertNotNull(history)
        assertNotNull(admin)
        assertNotNull(runtime)

        assertTrue(history!!.buttons['X']!!.actions.any { it is MenuAction.Back })
        assertTrue(history.buttons['R']!!.actions.any { it is MenuAction.Open && it.menuId == "runtime" })

        val reloadButton = admin!!.buttons['L']
        assertNotNull(reloadButton)
        assertEquals("amenu.admin", reloadButton!!.permission)
        assertTrue(reloadButton.denyActions.any { it is MenuAction.Message })
        assertTrue(reloadButton.actions.any { it is MenuAction.Refresh })

        val reusablePrompt = runtime!!.prompts["reusable"]
        assertNotNull(reusablePrompt)
        assertTrue(reusablePrompt!!.cancelActions.any { it is MenuAction.Open && it.menuId == "runtime" })

        val inlinePrompt = runtime.prompts["button-i"]
        assertNotNull(inlinePrompt)
        assertTrue(runtime.buttons['I']!!.actions.any { it is MenuAction.Prompt && it.promptId == "button-i" })
    }
}
