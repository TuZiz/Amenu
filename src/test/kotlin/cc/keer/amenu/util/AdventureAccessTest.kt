package cc.keer.amenu.util

import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import net.kyori.adventure.text.format.TextDecoration

class AdventureAccessTest {

    @Test
    fun text_formatter_disables_default_italic_for_custom_components() {
        val miniMessage = TextFormatter.component("<green><bold>Menu Name</bold></green>")
        val legacy = TextFormatter.component("&aLegacy Name")

        assertFalse(miniMessage.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE)
        assertFalse(legacy.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE)
    }

    @Test
    fun falls_back_to_legacy_item_meta_when_component_meta_methods_are_missing() {
        val server = MockBukkit.mock()
        try {
            val item = org.bukkit.inventory.ItemStack(Material.PAPER)
            val meta = item.itemMeta!!
            val title = TextFormatter.component("<green><bold>Compat Name</bold></green>")
            val lore = listOf(
                TextFormatter.component("<gray>Compat Lore</gray>"),
                TextFormatter.component("&aLegacy Lore"),
            )

            AdventureAccess.applyDisplayName(meta, title, null)
            AdventureAccess.applyLore(meta, lore, null)

            assertEquals(TextFormatter.legacyString(title), meta.displayName)
            assertEquals(lore.map(TextFormatter::legacyString), meta.lore)
        } finally {
            MockBukkit.unmock()
        }
    }
}
