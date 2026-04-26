package cc.keer.amenu.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class MenuHolder(
    val menuId: String,
) : InventoryHolder {

    private var inventory: Inventory? = null

    fun bind(inventory: Inventory) {
        this.inventory = inventory
    }

    override fun getInventory(): Inventory {
        return requireNotNull(inventory) { "Menu holder has not been bound yet." }
    }
}
