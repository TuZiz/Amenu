package cc.keer.amenu.config

import cc.keer.amenu.AMenuPlugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
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
        assertEquals("GRAY_STAINED_GLASS_PANE", filler!!.icon.materialName)
        assertEquals(" ", filler.icon.name)

        assertEquals("GOLDEN_HOE", menu.buttonAt(0)?.icon?.materialName)
        assertEquals("RED_BED", menu.buttonAt(1)?.icon?.materialName)
        assertEquals("GRAY_STAINED_GLASS_PANE", menu.buttonAt(2)?.icon?.materialName)
        assertEquals("PLAYER_HEAD", menu.buttonAt(4)?.icon?.materialName)
        assertEquals("CHEST_MINECART", menu.buttonAt(8)?.icon?.materialName)
        assertEquals("COMPASS", menu.buttonAt(13)?.icon?.materialName)
        assertEquals("DIAMOND", menu.buttonAt(15)?.icon?.materialName)
        assertEquals("LAPIS_LAZULI", menu.buttonAt(19)?.icon?.materialName)
        assertEquals("WRITABLE_BOOK", menu.buttonAt(21)?.icon?.materialName)
        assertEquals("NAME_TAG", menu.buttonAt(25)?.icon?.materialName)
        assertEquals("GOLD_INGOT", menu.buttonAt(29)?.icon?.materialName)
        assertEquals("ENDER_PEARL", menu.buttonAt(31)?.icon?.materialName)
        assertEquals("SUNFLOWER", menu.buttonAt(37)?.icon?.materialName)
        assertEquals("MAP", menu.buttonAt(43)?.icon?.materialName)
        assertEquals("EMERALD", menu.buttonAt(49)?.icon?.materialName)
    }

    @Test
    fun parses_showcase_inline_input_and_binding_context() {
        val showcase = plugin.menuRepository.menu("showcase")
        assertNotNull(showcase)

        val historyButton = showcase!!.buttonAt(9)
        assertNotNull(historyButton)
        assertEquals("BOOKSHELF", historyButton!!.icon.materialName)
        assertTrue(historyButton.actions.any { it is MenuAction.Open && it.menuId == "history" })

        val boundButton = showcase.buttonAt(12)
        assertNotNull(boundButton)
        assertTrue(boundButton!!.conditions.any {
            it is MenuCondition.PlaceholderEquals && it.key == "binding-type" && it.value == "item"
        })
        assertEquals("COMPASS", boundButton.icon.materialName)
        assertTrue(boundButton.actions.any { it is MenuAction.Message && it.text == "bound-{binding-id}-{binding-action}" })

        assertEquals(1, showcase.bindings.size)
        val binding = showcase.bindings.single()
        assertEquals("browser-compass", binding.id)
        assertEquals(MenuBindingType.ITEM, binding.type)
        assertEquals("COMPASS", binding.materialName)
        assertTrue(binding.actions.contains(MenuBindingAction.RIGHT_CLICK_AIR))
    }

    @Test
    fun loads_bundled_runtime_and_admin_examples() {
        val menuIds = plugin.menuRepository.listMenuIds().toSet()
        assertTrue(menuIds.containsAll(setOf("menu", "pay", "showcase", "history", "admin", "runtime", "skin")))

        val historyMenu = plugin.menuRepository.menu("history")
        val adminMenu = plugin.menuRepository.menu("admin")
        val runtimeMenu = plugin.menuRepository.menu("runtime")
        val skinMenu = plugin.menuRepository.menu("skin")

        assertNotNull(historyMenu)
        assertNotNull(adminMenu)
        assertNotNull(runtimeMenu)
        assertNotNull(skinMenu)
        assertEquals(4L, historyMenu!!.pageRegions["showcase"]!!.asyncDelayTicks)
        assertTrue(adminMenu!!.buttonAt(12)!!.actions.any {
            it is MenuAction.ConsoleCommand && it.command == "amenu reload"
        })

        val runtimePromptAction = runtimeMenu!!.buttonAt(10)!!.actions.filterIsInstance<MenuAction.Prompt>().single()
        val runtimePrompt = runtimeMenu.prompts[runtimePromptAction.promptId]
        assertNotNull(runtimePrompt)
        assertEquals(PromptType.CHAT, runtimePrompt!!.type)
        assertTrue(runtimePrompt.cancelActions.any { it is MenuAction.Open && it.menuId == "runtime" })
        assertTrue(runtimeMenu.buttons.values.any { button ->
            button.actions.any { it is MenuAction.Open && it.menuId == "history" }
        })

        assertTrue(skinMenu!!.bindings.any { it.type == MenuBindingType.COMMAND && it.commandAlias == "skin" })
        assertTrue(skinMenu.buttonAt(12)!!.actions.any { it is MenuAction.PlayerCommand && it.command == "skins" })
        val skinPromptAction = skinMenu.buttonAt(14)!!.actions.filterIsInstance<MenuAction.Prompt>().single()
        val skinPrompt = skinMenu.prompts[skinPromptAction.promptId]
        assertNotNull(skinPrompt)
        assertEquals(PromptType.CHAT, skinPrompt!!.type)
        assertTrue(skinPrompt.submitActions.any { it is MenuAction.PlayerCommand && it.command == "skin set {input}" })
        assertTrue(skinPrompt.cancelActions.any { it is MenuAction.Open && it.menuId == "skin" })
    }

    @Test
    fun recursively_loads_subfolder_menus_and_exposes_unique_basename_aliases() {
        val pointsPay = File(plugin.dataFolder, "menus/points/pay.yml")
        pointsPay.parentFile.mkdirs()
        pointsPay.writeText(
            """
            title: "Points Pay"
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
                name: "Points Pay"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val pointsQuota = File(plugin.dataFolder, "menus/points/quota.yml")
        pointsQuota.writeText(
            """
            title: "Quota"
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
                name: "Quota"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val vipPay = File(plugin.dataFolder, "menus/vip/pay.yml")
        vipPay.parentFile.mkdirs()
        vipPay.writeText(
            """
            title: "Vip Pay"
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
                name: "Vip Pay"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val report = plugin.menuRepository.loadMenus()

        assertTrue(report.successful)
        assertTrue(plugin.menuRepository.listMenuIds().contains("points/pay"))
        assertTrue(plugin.menuRepository.listMenuIds().contains("points/quota"))
        assertTrue(plugin.menuRepository.listMenuIds().contains("vip/pay"))

        val pointsPayMenu = plugin.menuRepository.menu("points/pay")
        val pointsQuotaMenu = plugin.menuRepository.menu("points/quota")
        val vipPayMenu = plugin.menuRepository.menu("vip/pay")

        assertNotNull(pointsPayMenu)
        assertNotNull(pointsQuotaMenu)
        assertNotNull(vipPayMenu)
        assertEquals("EMERALD", pointsPayMenu!!.buttonAt(10)!!.icon.materialName)
        assertEquals("PAPER", pointsQuotaMenu!!.buttonAt(10)!!.icon.materialName)
        assertEquals("DIAMOND", vipPayMenu!!.buttonAt(10)!!.icon.materialName)
        assertEquals("pay", plugin.menuRepository.menu("pay")!!.id)
        assertSame(pointsQuotaMenu, plugin.menuRepository.menu("quota"))
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
              - "#RRRD####"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "D":
                update: 4
                display:
                  material: CLOCK
                  name: "Countdown %amenu_demo%"
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
        assertEquals(4L, dynamicMenu.buttons['D']!!.updateIntervalTicks)
        assertEquals("CLOCK", rewards.surface.loading!!.materialName)
        assertEquals("BARRIER", rewards.surface.empty!!.materialName)
        assertEquals("RED_STAINED_GLASS_PANE", rewards.surface.error!!.materialName)
        assertEquals("CLOCK", rewards.loadingIcon.materialName)
        assertEquals("BARRIER", rewards.emptyIcon.materialName)

        val bundledMenu = plugin.menuRepository.menu("menu")
        assertNotNull(bundledMenu)
        assertEquals("GOLDEN_HOE", bundledMenu!!.buttonAt(0)?.icon?.materialName)
    }

    @Test
    fun parses_shape_backtick_tokens_and_colon_action_aliases() {
        val file = File(plugin.dataFolder, "menus/shape-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            Title: "<gold>Friendly Menu</gold>"
            Shape:
              - "x#######`Help`"
              - "#`Storage``Shop`#####"
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
                  name: "<aqua>Head</aqua>"
              "Help":
                display:
                  mats: BOOK
                  name:
                    - "<yellow>Help Center</yellow>"
                actions:
                  all:
                    - "message: opened-help"
                    - "delay: 2"
              "Storage":
                display:
                  material: BARREL
                  shiny: true
                  name: "<gold>Storage</gold>"
                actions:
                  left:
                    - "command: pi"
              "Shop":
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
        assertTrue(menu.title.contains("Friendly"))
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

    @Test
    fun parses_condition_shorthand_for_buttons_and_states() {
        val file = File(plugin.dataFolder, "menus/condition-shorthand.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "Condition Shorthand"
            layout:
              - "#########"
              - "#A#B#####"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: FEATHER
                name: "Flight"
                conditions: "perm: cmi.command.fly"
                click:
                  - "message: owned"
              "B":
                material: COMPASS
                name: "Bound"
                states:
                  active:
                    conditions:
                      - "perm: amenu.admin"
                      - "placeholder: binding-type=item"
                    material: EMERALD
                    click:
                      - "message: active"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("condition-shorthand")

        assertNotNull(menu)

        val flightButton = menu!!.buttonAt(10)
        assertNotNull(flightButton)
        assertTrue(flightButton!!.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "cmi.command.fly"
        })

        val boundButton = menu.buttonAt(12)
        assertNotNull(boundButton)
        val activeState = boundButton!!.states.single()
        assertTrue(activeState.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "amenu.admin"
        })
        assertTrue(activeState.conditions.any {
            it is MenuCondition.PlaceholderEquals && it.key == "binding-type" && it.value == "item"
        })
    }

    @Test
    fun parses_icons_layers_legacy_condition_strings_and_nested_action_blocks() {
        val file = File(plugin.dataFolder, "menus/icons-layer-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "Icons Layer Demo"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: CHEST
                name: "Starter"
                actions:
                  all:
                    actions:
                      - "close"
                      - "delay: 1"
                      - "command: kit starter"
                icons:
                  - condition: "perm *starter.claimed"
                    display:
                      mats: AIR
                  - condition: "perm *starter.unlocked"
                    display:
                      mats: ENDER_CHEST
                      name: "Holiday"
                    actions:
                      all:
                        actions:
                          - "console: money give %player_name% 1888"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("icons-layer-demo")

        assertNotNull(menu)
        val button = menu!!.buttonAt(10)
        assertNotNull(button)
        assertEquals("CHEST", button!!.icon.materialName)
        assertTrue(button.actions.any { it is MenuAction.Close })
        assertTrue(button.actions.any { it is MenuAction.Delay && it.ticks == 1L })
        assertTrue(button.actions.any { it is MenuAction.PlayerCommand && it.command == "kit starter" })
        assertEquals(2, button.states.size)

        val firstState = button.states[0]
        assertTrue(firstState.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "starter.claimed"
        })
        assertEquals("AIR", firstState.icon!!.materialName)

        val secondState = button.states[1]
        assertTrue(secondState.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "starter.unlocked"
        })
        assertEquals("ENDER_CHEST", secondState.icon!!.materialName)
        assertTrue(secondState.actions!!.any {
            it is MenuAction.ConsoleCommand && it.command == "money give %player_name% 1888"
        })
    }

    @Test
    fun ignores_unplaced_named_buttons_without_failing_menu_load() {
        val file = File(plugin.dataFolder, "menus/unplaced-button-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "Unplaced Button Demo"
            shape:
              - "#########"
              - "#`Flight`#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "Flight":
                material: FEATHER
                name: "Flight"
              "Guide":
                material: PAPER
                name: "Guide"
                lore:
                  - "Disabled"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val report = plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("unplaced-button-demo")

        assertTrue(report.successful)
        assertNotNull(menu)
        assertEquals("FEATHER", menu!!.buttonAt(10)!!.icon.materialName)
        assertFalse(menu.buttons.values.any { it.icon.name == "Guide" })
    }

    @Test
    fun supports_explicit_button_symbol_alias_when_name_is_not_in_shape() {
        val file = File(plugin.dataFolder, "menus/button-symbol-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "Button Symbol Demo"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "Guide":
                symbol: "A"
                material: PAPER
                name: "Guide"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        val report = plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("button-symbol-demo")

        assertTrue(report.successful)
        assertNotNull(menu)
        assertEquals("PAPER", menu!!.buttonAt(10)!!.icon.materialName)
    }

    @Test
    fun parses_conditional_purchase_actions_and_short_aliases() {
        val file = File(plugin.dataFolder, "menus/conditional-action-demo.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "Conditional Action Demo"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: FEATHER
                name: "Flight"
                click:
                  - condition: "check papi *%playerpoints_points% >= *30"
                    actions:
                      - "take-point: 30"
                      - "console: lp user %player_name% permission set cmi.command.fly true"
                      - "title: &a&lFlight Activated||&7Purchase success"
                      - "tell: &aPurchase success"
                    deny:
                      - "title: &c&lPurchase failed||&7Not enough points"
                      - "tell: &cNot enough points"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("conditional-action-demo")

        assertNotNull(menu)
        val action = menu!!.buttonAt(10)!!.actions.single() as MenuAction.Conditional
        val condition = action.condition as MenuCondition.Comparison
        assertEquals("%playerpoints_points%", condition.left)
        assertEquals(ComparisonOperator.GREATER_THAN_OR_EQUAL, condition.operator)
        assertEquals("30", condition.right)
        assertTrue(action.successActions.any { it is MenuAction.TakePoint && it.amount == "30" })
        assertTrue(action.successActions.any {
            it is MenuAction.ConsoleCommand && it.command == "lp user %player_name% permission set cmi.command.fly true"
        })
        assertTrue(action.successActions.any {
            it is MenuAction.Title && it.title == "&a&lFlight Activated" && it.subtitle == "&7Purchase success"
        })
        assertTrue(action.successActions.any { it is MenuAction.Message && it.text == "&aPurchase success" })
        assertTrue(action.denyActions.any {
            it is MenuAction.Title && it.title == "&c&lPurchase failed" && it.subtitle == "&7Not enough points"
        })
        assertTrue(action.denyActions.any { it is MenuAction.Message && it.text == "&cNot enough points" })
    }
}
