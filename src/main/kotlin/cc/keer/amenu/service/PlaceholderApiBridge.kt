package cc.keer.amenu.service

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

interface PlaceholderApiBridge {
    fun isAvailable(): Boolean

    fun render(player: Player, text: String): String

    companion object {
        fun disabled(): PlaceholderApiBridge {
            return object : PlaceholderApiBridge {
                override fun isAvailable(): Boolean = false

                override fun render(player: Player, text: String): String = text
            }
        }
    }
}

class BukkitPlaceholderApiBridge(
    private val plugin: JavaPlugin,
) : PlaceholderApiBridge {
    override fun isAvailable(): Boolean {
        return plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")
    }

    override fun render(player: Player, text: String): String {
        if (!isAvailable()) {
            return text
        }
        return runCatching { PlaceholderAPI.setPlaceholders(player, text) }
            .getOrDefault(text)
    }
}
