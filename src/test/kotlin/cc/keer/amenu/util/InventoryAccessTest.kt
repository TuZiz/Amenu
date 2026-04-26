package cc.keer.amenu.util

import cc.keer.amenu.gui.MenuHolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit

class InventoryAccessTest {

    @Test
    fun falls_back_to_legacy_inventory_titles_when_component_factory_is_missing() {
        val server = MockBukkit.mock()
        try {
            val holder = MenuHolder("compat")
            val player = server.addPlayer()
            val title = TextFormatter.component("<gradient:#58A6FF:#F778BA><bold>Compatibility Title</bold></gradient>")

            val inventory = InventoryAccess.createInventory(holder, 27, title, null)
            player.openInventory(inventory)

            assertNotNull(inventory)
            assertEquals(holder, inventory.holder)
            assertEquals(TextFormatter.legacyString(title), player.openInventory.title)
        } finally {
            MockBukkit.unmock()
        }
    }
}
