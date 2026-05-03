package cc.keer.amenu.gui

import cc.keer.amenu.service.MenuService
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerQuitEvent

class MenuListener(
    private val menuService: MenuService,
) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? MenuHolder ?: return
        event.isCancelled = true

        if (event.clickedInventory != event.view.topInventory) {
            return
        }

        val player = event.whoClicked as? Player ?: return
        menuService.handleClick(player, holder.menuId, event.slot, event.click)
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder as? MenuHolder ?: return
        if (event.rawSlots.any { slot -> slot < holder.inventory.size }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.view.topInventory.holder as? MenuHolder ?: return
        val player = event.player as? Player ?: return
        menuService.handleInventoryClosed(player, holder.menuId)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        menuService.handlePlayerQuit(event.player)
    }
}
