package cc.keer.amenu.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.util.UUID

class PromptAnvilHolder(
    val playerId: UUID,
) : InventoryHolder {

    private var inventory: Inventory? = null

    fun bind(inventory: Inventory) {
        this.inventory = inventory
    }

    override fun getInventory(): Inventory {
        return requireNotNull(inventory) { "Prompt anvil holder has not been bound yet." }
    }
}
