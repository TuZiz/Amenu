package cc.keer.amenu.util

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import java.lang.reflect.Method

object InventoryAccess {

    private val componentInventoryFactory = resolveComponentInventoryFactory()

    fun createInventory(
        holder: InventoryHolder,
        size: Int,
        title: Component,
    ): Inventory {
        return createInventory(holder, size, title, componentInventoryFactory)
    }

    internal fun createInventory(
        holder: InventoryHolder,
        size: Int,
        title: Component,
        componentFactory: Method?,
    ): Inventory {
        val componentInventory = componentFactory
            ?.runCatching { invoke(null, holder, size, title) as? Inventory }
            ?.getOrNull()
        if (componentInventory != null) {
            return componentInventory
        }

        return Bukkit.createInventory(holder, size, TextFormatter.legacyString(title))
    }

    private fun resolveComponentInventoryFactory(): Method? {
        return runCatching {
            Bukkit::class.java.getMethod(
                "createInventory",
                InventoryHolder::class.java,
                Int::class.javaPrimitiveType,
                Component::class.java,
            )
        }.getOrNull()
    }
}
