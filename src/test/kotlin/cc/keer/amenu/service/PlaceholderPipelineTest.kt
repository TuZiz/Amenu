package cc.keer.amenu.service

import cc.keer.amenu.support.MenuPluginTestHarness
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceholderPipelineTest : MenuPluginTestHarness() {

    private val plainText = PlainTextComponentSerializer.plainText()

    @Test
    fun shared_pipeline_resolves_internal_then_optional_external_placeholders() {
        val disabledPipeline = PlaceholderPipeline(
            bridge = FakePlaceholderApiBridge(enabled = false),
        )
        assertEquals("Hello Tester", disabledPipeline.render(player, "Hello {player}"))
        assertEquals("Hello %player_name%", disabledPipeline.render(player, "Hello %player_name%"))

        val enabledPipeline = PlaceholderPipeline(
            bridge = FakePlaceholderApiBridge(enabled = true),
        )
        assertEquals("Hello Tester", enabledPipeline.render(player, "Hello %player_name%"))
        assertEquals("Tester / Tester", enabledPipeline.render(player, "{player} / %player_name%"))
    }

    @Test
    fun button_state_rendering_uses_the_shared_placeholder_pipeline() {
        writeMenu(
            "state-pipeline",
            """
            title: "State Pipeline"
            layout:
              - "#########"
              - "#IE######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "I":
                material: BARRIER
                name: "Internal base"
                states:
                  internal:
                    conditions:
                      placeholder-equals:
                        "{player}": "Tester"
                    material: EMERALD
                    name: "Internal {player}"
                    lore:
                      - "Bridge %player_name%"
              "E":
                material: REDSTONE
                name: "External base"
                states:
                  external:
                    conditions:
                      placeholder-equals:
                        "%player_name%": "Tester"
                    material: DIAMOND
                    name: "External %player_name%"
                    lore:
                      - "Viewer {player}"
            """.trimIndent(),
        )

        val pipeline = PlaceholderPipeline(
            bridge = FakePlaceholderApiBridge(enabled = true),
        )
        val service = MenuService(plugin, plugin.settings, plugin.menuRepository, plugin.platformScheduler, pipeline)

        service.openMenu(player, "state-pipeline", navigation = NavigationMode.ROOT)

        val menu = requireNotNull(plugin.menuRepository.menu("state-pipeline"))
        val internal = requireNotNull(player.openInventory.topInventory.getItem(menu.slotsFor('I').first()))
        val external = requireNotNull(player.openInventory.topInventory.getItem(menu.slotsFor('E').first()))

        assertEquals(Material.EMERALD, internal.type)
        assertEquals("Internal Tester", plainName(internal))
        assertEquals(listOf("Bridge Tester"), plainLore(internal))

        assertEquals(Material.DIAMOND, external.type)
        assertEquals("External Tester", plainName(external))
        assertEquals(listOf("Viewer Tester"), plainLore(external))
    }

    @Test
    fun opening_a_menu_only_renders_placeholder_text_once_per_surface() {
        writeMenu(
            "single-pass-open",
            """
            title: "%player_name%"
            layout:
              - "A        "
            buttons:
              "A":
                material: PAPER
                name: "%player_name%"
            """.trimIndent(),
        )

        val bridge = CountingPlaceholderApiBridge()
        val service = MenuService(
            plugin = plugin,
            settings = plugin.settings,
            repository = plugin.menuRepository,
            platformScheduler = plugin.platformScheduler,
            placeholderPipeline = PlaceholderPipeline(bridge),
        )

        service.openMenu(player, "single-pass-open", navigation = NavigationMode.ROOT)

        assertEquals(2, bridge.renderCalls)
    }

    private fun plainName(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        val componentGetter = meta.javaClass.methods.firstOrNull { it.name == "displayName" && it.parameterCount == 0 }
        val componentName = componentGetter?.invoke(meta) as? Component
        return componentName?.let(plainText::serialize)
    }

    private fun plainLore(item: ItemStack): List<String> {
        val meta = item.itemMeta ?: return emptyList()
        val loreGetter = meta.javaClass.methods.firstOrNull { it.name == "lore" && it.parameterCount == 0 }
        val lore = loreGetter?.invoke(meta) as? List<*>
        return lore.orEmpty().mapNotNull { line -> (line as? Component)?.let(plainText::serialize) }
    }

    private class FakePlaceholderApiBridge(
        private val enabled: Boolean,
    ) : PlaceholderApiBridge {
        override fun isAvailable(): Boolean = enabled

        override fun render(player: Player, text: String): String {
            if (!enabled) {
                return text
            }
            return text.replace("%player_name%", player.name)
        }
    }

    private class CountingPlaceholderApiBridge : PlaceholderApiBridge {
        var renderCalls: Int = 0

        override fun isAvailable(): Boolean = true

        override fun render(player: Player, text: String): String {
            renderCalls++
            return text.replace("%player_name%", player.name)
        }
    }
}
