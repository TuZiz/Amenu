package cc.keer.amenu.util

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

object BindingItemAccess {

    private const val KEY = "binding-id"

    fun write(plugin: JavaPlugin, meta: ItemMeta, bindingId: String) {
        meta.persistentDataContainer.set(key(plugin), PersistentDataType.STRING, bindingId)
    }

    fun read(plugin: JavaPlugin, meta: ItemMeta?): String? {
        if (meta == null) {
            return null
        }
        return meta.persistentDataContainer.get(key(plugin), PersistentDataType.STRING)
    }

    private fun key(plugin: JavaPlugin): NamespacedKey {
        return NamespacedKey(plugin, KEY)
    }
}
