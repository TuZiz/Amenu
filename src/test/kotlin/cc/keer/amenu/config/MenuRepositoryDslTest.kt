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
import java.io.File

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
    fun loads_reference_menu_layout_and_core_slots() {
        val menu = plugin.menuRepository.menu("menu")

        assertNotNull(menu)
        assertEquals(6, menu!!.rows)
        assertEquals(54, menu.size)
        assertTrue(menu.title.isNotBlank())

        val filler = menu.buttons['B']
        assertNotNull(filler)
        assertEquals("BLACK_STAINED_GLASS_PANE", filler!!.icon.materialName)
        assertEquals(" ", filler.icon.name)

        assertEquals("GOLDEN_SHOVEL", menu.buttonAt(0)?.icon?.materialName)
        assertEquals("RED_BED", menu.buttonAt(1)?.icon?.materialName)
        assertEquals("BEACON", menu.buttonAt(4)?.icon?.materialName)
        assertEquals("CHEST_MINECART", menu.buttonAt(7)?.icon?.materialName)
        assertEquals("BOOK", menu.buttonAt(8)?.icon?.materialName)
        assertEquals("EMERALD", menu.buttonAt(10)?.icon?.materialName)
        assertEquals("DIAMOND", menu.buttonAt(13)?.icon?.materialName)
        assertEquals("WRITABLE_BOOK", menu.buttonAt(16)?.icon?.materialName)
        assertEquals("MAP", menu.buttonAt(19)?.icon?.materialName)
        assertEquals("CLOCK", menu.buttonAt(21)?.icon?.materialName)
        assertEquals("NAME_TAG", menu.buttonAt(23)?.icon?.materialName)
        assertEquals("FIREWORK_STAR", menu.buttonAt(25)?.icon?.materialName)
        assertEquals("ENDER_CHEST", menu.buttonAt(28)?.icon?.materialName)
        assertEquals("ENDER_PEARL", menu.buttonAt(30)?.icon?.materialName)
        assertEquals("SUNFLOWER", menu.buttonAt(32)?.icon?.materialName)
        assertEquals("GOLD_INGOT", menu.buttonAt(34)?.icon?.materialName)
        assertEquals("PAPER", menu.buttonAt(37)?.icon?.materialName)
        assertEquals("REDSTONE", menu.buttonAt(40)?.icon?.materialName)
        assertEquals("WRITTEN_BOOK", menu.buttonAt(43)?.icon?.materialName)
        assertEquals("DARK_OAK_DOOR", menu.buttonAt(45)?.icon?.materialName)
        assertEquals("BARREL", menu.buttonAt(49)?.icon?.materialName)
        assertEquals("NETHER_STAR", menu.buttonAt(53)?.icon?.materialName)
    }

    @Test
    fun parses_showcase_inline_input_and_binding_context() {
        val showcase = plugin.menuRepository.menu("showcase")
        assertNotNull(showcase)

        val quickInputButton = showcase!!.buttonAt(10)
        assertNotNull(quickInputButton)
        assertEquals("WRITABLE_BOOK", quickInputButton!!.icon.materialName)
        assertTrue(quickInputButton.icon.glow)
        assertTrue(quickInputButton.actions.any { it is MenuAction.Close })

        val quickPromptAction = quickInputButton.actions.filterIsInstance<MenuAction.Prompt>().single()
        val quickPrompt = showcase.prompts[quickPromptAction.promptId]
        assertNotNull(quickPrompt)
        assertEquals(PromptType.CHAT, quickPrompt!!.type)
        assertTrue(quickPrompt.startMessages.isNotEmpty())
        assertTrue(quickPrompt.cancelActions.any { it is MenuAction.Message })
        assertTrue(quickPrompt.cancelActions.any { it is MenuAction.Open && it.menuId == "showcase" })
        assertTrue(quickPrompt.submitActions.any { it is MenuAction.Open && it.menuId == "runtime" })

        val boundButton = showcase.buttonAt(12)
        assertNotNull(boundButton)
        assertTrue(boundButton!!.conditions.any {
            it is MenuCondition.PlaceholderEquals && it.key == "binding-type" && it.value == "item"
        })

        assertEquals(1, showcase.bindings.size)
        val binding = showcase.bindings.single()
        assertEquals("browser-compass", binding.id)
        assertEquals(MenuBindingType.ITEM, binding.type)
        assertEquals("COMPASS", binding.materialName)
        assertTrue(binding.actions.contains(MenuBindingAction.RIGHT_CLICK_AIR))
    }

    @Test
    fun loads_bundled_runtime_and_admin_examples() {
        assertEquals(setOf("menu", "showcase", "history", "admin", "runtime"), plugin.menuRepository.listMenuIds().toSet())

        val history = plugin.menuRepository.menu("history")
        val admin = plugin.menuRepository.menu("admin")
        val runtime = plugin.menuRepository.menu("runtime")

        assertNotNull(history)
        assertNotNull(admin)
        assertNotNull(runtime)

        val showcaseRegion = history!!.pageRegions["showcase"]
        assertNotNull(showcaseRegion)
        assertEquals('I', showcaseRegion!!.symbol)
        assertEquals(8L, showcaseRegion.asyncDelayTicks)
        assertEquals(7, showcaseRegion.entries.size)
        assertTrue(history.buttonAt(10)!!.actions.any { it is MenuAction.Page && it.operation == PageOperation.PREVIOUS })
        assertTrue(history.buttonAt(16)!!.actions.any { it is MenuAction.Page && it.operation == PageOperation.NEXT })
        assertTrue(history.buttonAt(21)!!.actions.any { it is MenuAction.Page && it.operation == PageOperation.REFRESH })
        assertTrue(history.buttonAt(23)!!.actions.any { it is MenuAction.Open && it.menuId == "runtime" })
        assertTrue(history.buttonAt(25)!!.actions.any { it is MenuAction.Back })

        val compassButton = admin!!.buttonAt(10)
        assertNotNull(compassButton)
        assertEquals("amenu.admin", compassButton!!.permission)
        assertTrue(compassButton.denyActions.any { it is MenuAction.Message })
        assertTrue(compassButton.actions.any { it is MenuAction.PlayerCommand && it.command == "amenu give browser-compass" })

        val reloadButton = admin.buttonAt(12)
        assertNotNull(reloadButton)
        assertEquals("amenu.admin", reloadButton!!.permission)
        assertTrue(reloadButton.denyActions.any { it is MenuAction.Message })
        assertTrue(reloadButton.actions.any { it is MenuAction.ConsoleCommand && it.command == "amenu reload" })
        assertTrue(reloadButton.actions.any { it is MenuAction.Refresh })

        val notesButton = admin.buttonAt(16)
        assertNotNull(notesButton)
        assertTrue(notesButton!!.states.any { state ->
            state.conditions.any { it is MenuCondition.HasPermission && it.permission == "amenu.admin" }
        })

        val chatButton = runtime!!.buttonAt(10)
        val chatPromptAction = chatButton!!.actions.filterIsInstance<MenuAction.Prompt>().single()
        val chatPrompt = runtime.prompts[chatPromptAction.promptId]
        assertNotNull(chatPrompt)
        assertEquals(PromptType.CHAT, chatPrompt!!.type)
        assertTrue(chatPrompt.cancelActions.any { it is MenuAction.Open && it.menuId == "runtime" })
        assertEquals(1, runtime.prompts.size)
        assertTrue(runtime.buttons.values.any { button ->
            button.actions.any { it is MenuAction.Open && it.menuId == "history" }
        })
    }

    @Test
    fun parses_provider_cache_loading_empty_update_and_error_metadata_without_breaking_static_menus() {
        val file = File(plugin.dataFolder, "menus/provider-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "Provider Demo"
            layout:
              - "#########"
              - "#RRR#####"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            pages:
              rewards:
                symbol: "R"
                provider:
                  type: placeholder-list
                  params:
                    viewer: "%player_name%"
                    owner: "{player}"
                  cache:
                    ttl: 40
                update:
                  interval: 20
                loading:
                  material: CLOCK
                  name: "Loading {player}"
                empty:
                  material: BARRIER
                  name: "Nothing for %player_name%"
                error:
                  material: RED_STAINED_GLASS_PANE
                  name: "Error for {player}"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback entry"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        plugin.menuRepository.loadMenus()

        val dynamicMenu = plugin.menuRepository.menu("provider-demo")
        assertNotNull(dynamicMenu)

        val rewards = dynamicMenu!!.pageRegions["rewards"]
        assertNotNull(rewards)
        assertEquals("placeholder-list", rewards!!.provider!!.type)
        assertEquals("%player_name%", rewards.provider.params["viewer"])
        assertEquals("{player}", rewards.provider.params["owner"])
        assertEquals(40L, rewards.provider.cache!!.ttl)
        assertEquals(20L, rewards.provider.update!!.interval)
        assertEquals("CLOCK", rewards.surface.loading!!.materialName)
        assertEquals("BARRIER", rewards.surface.empty!!.materialName)
        assertEquals("RED_STAINED_GLASS_PANE", rewards.surface.error!!.materialName)
        assertEquals("CLOCK", rewards.loadingIcon.materialName)
        assertEquals("BARRIER", rewards.emptyIcon.materialName)

        val bundledMenu = plugin.menuRepository.menu("menu")
        assertNotNull(bundledMenu)
        assertEquals("GOLDEN_SHOVEL", bundledMenu!!.buttonAt(0)?.icon?.materialName)
    }

    @Test
    fun parses_shape_backtick_tokens_and_colon_action_aliases() {
        val file = File(plugin.dataFolder, "menus/shape-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            Title: "<gold>编辑友好菜单</gold>"
            Shape:
              - "x#######`帮助`"
              - "#`仓库``商店`#####"
            Fill:
              display:
                mats: GRAY_STAINED_GLASS_PANE
                name: " "
            Bindings:
              Item:
                - "material:clock id:quick-clock"
              command:
                - "menu"
            BUTTONS:
              "x":
                display:
                  material: PLAYER_HEAD
                  name: "<aqua>头像</aqua>"
              "帮助":
                display:
                  mats: BOOK
                  name:
                    - "<yellow>帮助中心</yellow>"
                actions:
                  all:
                    - "message: opened-help"
                    - "delay: 2"
              "仓库":
                display:
                  material: BARREL
                  shiny: true
                  name: "<gold>仓库</gold>"
                actions:
                  left:
                    - "command: pi"
              "商店":
                material: EMERALD
                click:
                  - "menu: menu"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("shape-demo")

        assertNotNull(menu)
        assertEquals(2, menu!!.rows)
        assertTrue(menu.title.contains("编辑友好"))
        assertEquals('x', menu.buttonAt(0)?.symbol)
        assertEquals("PLAYER_HEAD", menu.buttons['x']!!.icon.materialName)
        assertEquals("GRAY_STAINED_GLASS_PANE", menu.buttons['#']!!.icon.materialName)

        val helpSlot = 8
        assertNotNull(menu.buttonAt(helpSlot))
        assertEquals("BOOK", menu.buttonAt(helpSlot)!!.icon.materialName)
        assertTrue(menu.buttonAt(helpSlot)!!.actions.any { it is MenuAction.Message && it.text == "opened-help" })
        assertTrue(menu.buttonAt(helpSlot)!!.actions.any { it is MenuAction.Delay && it.ticks == 2L })

        val chestSlot = 10
        assertEquals("BARREL", menu.buttonAt(chestSlot)!!.icon.materialName)
        assertTrue(menu.buttonAt(chestSlot)!!.icon.glow)
        assertTrue(menu.buttonAt(chestSlot)!!.actions.any { it is MenuAction.PlayerCommand && it.command == "pi" })

        val shopSlot = 11
        assertEquals("EMERALD", menu.buttonAt(shopSlot)!!.icon.materialName)
        assertTrue(menu.buttonAt(shopSlot)!!.actions.any { it is MenuAction.Open && it.menuId == "menu" })

        assertTrue(menu.bindings.any { it.id == "quick-clock" && it.materialName == "clock" })
    }
}
