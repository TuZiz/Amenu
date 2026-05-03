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
    fun starts_with_released_feature_templates_without_auto_seeded_menu() {
        assertTrue(plugin.menuRepository.listMenuIds().isEmpty())
        assertFalse(File(plugin.dataFolder, "menus/menu.yml").isFile)
        assertTrue(releasedTemplateFiles().isNotEmpty())
    }

    @Test
    fun relative_menu_path_accepts_absolute_file_with_relative_base_folder() {
        val relativeMenuFolder = File("plugins/AMenu/menus")
        val absoluteMenuFile = File(relativeMenuFolder.absoluteFile, "nested/live.yml")

        assertEquals("nested/live.yml", normalizedRelativeMenuPath(relativeMenuFolder, absoluteMenuFile))
    }

    @Test
    fun parses_feature_template_refresh_and_binding_context() {
        val template = releasedTemplateFiles().first { file ->
            val text = file.readText(Charsets.UTF_8)
            text.contains("update:") && text.contains("bindings:")
        }
        val menuId = installTemplateMenu(template)
        val showcase = plugin.menuRepository.menu(menuId)
        assertNotNull(showcase)

        assertTrue(showcase!!.buttons.values.any { it.updateIntervalTicks != null })
        assertTrue(showcase.bindings.isNotEmpty())
    }

    @Test
    fun loads_feature_showcase_templates_after_copying_to_menus() {
        val installed = installAllReleasedTemplates()
        val menuIds = plugin.menuRepository.listMenuIds().toSet()

        assertTrue(installed.isNotEmpty())
        assertTrue(menuIds.containsAll(installed))
        installed.forEach { menuId ->
            val menu = plugin.menuRepository.menu(menuId)
            assertNotNull(menu)
            assertTrue(menu!!.rows in 1..6)
            assertTrue(menu.size == menu.rows * 9)
        }
    }

    private fun installTemplateMenu(template: File): String {
        val menu = File(plugin.dataFolder, "menus/${template.name}")
        require(template.isFile) { "Missing bundled template ${template.absolutePath}" }
        menu.parentFile.mkdirs()
        template.copyTo(menu, overwrite = true)
        val report = plugin.menuRepository.loadMenus()
        assertTrue(report.successful)
        return template.nameWithoutExtension.lowercase()
    }

    private fun installAllReleasedTemplates(): Set<String> {
        val templates = releasedTemplateFiles()
        templates.forEach { template ->
            val menu = File(plugin.dataFolder, "menus/${template.name}")
            menu.parentFile.mkdirs()
            template.copyTo(menu, overwrite = true)
        }
        val report = plugin.menuRepository.loadMenus()
        assertTrue(report.successful)
        return templates.map { it.nameWithoutExtension.lowercase() }.toSet()
    }

    private fun releasedTemplateFiles(): List<File> {
        val templates = File(plugin.dataFolder, "templates")
        return templates.listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    @Test
    fun parses_copy_ready_project_root_menus_without_content_hardcoding() {
        val installed = installProjectRootMenus()
        val menuIds = plugin.menuRepository.listMenuIds().toSet()

        assertTrue(installed.isNotEmpty())
        assertTrue(menuIds.containsAll(installed))
        installed.forEach { menuId ->
            val menu = plugin.menuRepository.menu(menuId)
            assertNotNull(menu)
            assertTrue(menu!!.buttons.isNotEmpty())
            assertTrue(menu.bindings.isNotEmpty())
        }
    }

    @Test
    fun copy_ready_project_root_menu_visible_messages_close_first() {
        val violations = projectRootMenuFiles()
            .flatMap { file -> visibleMessageCloseFirstViolations(file).map { "${file.name}:$it" } }

        assertTrue(violations.isEmpty(), violations.joinToString(System.lineSeparator()))
    }

    private fun installProjectRootMenus(): Set<String> {
        val sources = projectRootMenuFiles()
        sources.forEach { source ->
            val menu = File(plugin.dataFolder, "menus/${source.name}")
            menu.parentFile.mkdirs()
            source.copyTo(menu, overwrite = true)
        }
        val report = plugin.menuRepository.loadMenus()
        assertTrue(report.successful)
        return sources.map { it.nameWithoutExtension.lowercase() }.toSet()
    }

    private fun projectRootMenuFiles(): List<File> {
        return File(".").listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    private fun visibleMessageCloseFirstViolations(file: File): List<Int> {
        val lines = file.readLines(Charsets.UTF_8)
        val violations = mutableListOf<Int>()
        var index = 0
        while (index < lines.size) {
            val match = ACTION_ITEM_REGEX.matchEntire(lines[index])
            if (match == null) {
                index++
                continue
            }
            val indent = match.groupValues[1]
            val start = index
            val group = mutableListOf<String>()
            while (index < lines.size && lines[index].startsWith("$indent- ")) {
                group += lines[index]
                index++
            }
            val hasVisibleMessage = group.any { ACTION_VISIBLE_MESSAGE_REGEX.containsMatchIn(it) }
            val startsWithClose = ACTION_CLOSE_REGEX.matches(group.firstOrNull().orEmpty())
            if (hasVisibleMessage && !startsWithClose) {
                violations += start + 1
            }
        }
        return violations
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
        assertEquals("points/pay", plugin.menuRepository.menu("pay")!!.id)
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

        assertFalse(plugin.menuRepository.listMenuIds().contains("menu"))
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
        assertTrue(menu.buttonAt(chestSlot)!!.actions.any { action ->
            action is MenuAction.Conditional &&
                action.condition == MenuCondition.PlaceholderEquals("click-type", "left") &&
                action.successActions.any { it is MenuAction.PlayerCommand && it.command == "pi" }
        })

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
                name: "Permission Button"
                conditions: "perm: example.permission"
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

        val permissionButton = menu!!.buttonAt(10)
        assertNotNull(permissionButton)
        assertTrue(permissionButton!!.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "example.permission"
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
                name: "Primary"
                actions:
                  all:
                    actions:
                      - "close"
                      - "delay: 1"
                      - "command: example-action primary"
                icons:
                  - condition: "perm *example.state.hidden"
                    display:
                      mats: AIR
                  - condition: "perm *example.state.unlocked"
                    display:
                      mats: ENDER_CHEST
                      name: "Unlocked"
                    actions:
                      all:
                        actions:
                          - "console: example-console reward %player_name%"
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
        assertTrue(button.actions.any { it is MenuAction.PlayerCommand && it.command == "example-action primary" })
        assertEquals(2, button.states.size)

        val firstState = button.states[0]
        assertTrue(firstState.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "example.state.hidden"
        })
        assertEquals("AIR", firstState.icon!!.materialName)

        val secondState = button.states[1]
        assertTrue(secondState.conditions.any {
            it is MenuCondition.HasPermission && it.permission == "example.state.unlocked"
        })
        assertEquals("ENDER_CHEST", secondState.icon!!.materialName)
        assertTrue(secondState.actions!!.any {
            it is MenuAction.ConsoleCommand && it.command == "example-console reward %player_name%"
        })
    }

    @Test
    fun state_icon_name_override_without_lore_clears_base_lore() {
        val file = File(plugin.dataFolder, "menus/state-icon-override.yml")
        file.parentFile.mkdirs()
        file.writeText(
            """
            title: "State Icon Override"
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
                name: "Base"
                lore:
                  - "base-lore"
                icons:
                  - condition: "perm *example.state.active"
                    display:
                      mats: ENDER_CHEST
                      name: "Override"
            """.trimIndent(),
            Charsets.UTF_8,
        )

        plugin.menuRepository.loadMenus()
        val menu = plugin.menuRepository.menu("state-icon-override")

        assertNotNull(menu)
        val button = menu!!.buttonAt(10)
        assertNotNull(button)
        assertEquals(listOf("base-lore"), button!!.icon.lore)

        val state = button.states.single()
        assertEquals("ENDER_CHEST", state.icon!!.materialName)
        assertEquals("Override", state.icon!!.name)
        assertTrue(state.icon!!.lore.isEmpty())
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
              - "#`Primary`#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "Primary":
                material: FEATHER
                name: "Primary"
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
    fun parses_conditional_action_branches_and_short_aliases() {
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
                name: "Conditional"
                click:
                  - condition: "check papi *%playerpoints_points% >= *30"
                    actions:
                      - "take-point: 30"
                      - "console: example-console grant %player_name% example.permission"
                      - "title: &a&lAction Activated||&7Action success"
                      - "tell: &aAction success"
                    deny:
                      - "title: &c&lAction failed||&7Condition not met"
                      - "tell: &cCondition not met"
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
            it is MenuAction.ConsoleCommand && it.command == "example-console grant %player_name% example.permission"
        })
        assertTrue(action.successActions.any {
            it is MenuAction.Title && it.title == "&a&lAction Activated" && it.subtitle == "&7Action success"
        })
        assertTrue(action.successActions.any { it is MenuAction.Message && it.text == "&aAction success" })
        assertTrue(action.denyActions.any {
            it is MenuAction.Title && it.title == "&c&lAction failed" && it.subtitle == "&7Condition not met"
        })
        assertTrue(action.denyActions.any { it is MenuAction.Message && it.text == "&cCondition not met" })
    }

    private companion object {
        private val ACTION_ITEM_REGEX = Regex("^(\\s*)-\\s+.+")
        private val ACTION_VISIBLE_MESSAGE_REGEX = Regex("^\\s*-\\s+\"?(tell|title):")
        private val ACTION_CLOSE_REGEX = Regex("^\\s*-\\s+\"?close\"?\\s*$")
    }
}
