package cc.keer.amenu.platform

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

object PlatformSchedulerFactory {

    fun create(plugin: JavaPlugin): PlatformScheduler {
        return if (supportsFoliaRuntime(plugin)) {
            FoliaPlatformScheduler(plugin)
        } else {
            BukkitPlatformScheduler(plugin)
        }
    }

    private fun supportsFoliaRuntime(plugin: JavaPlugin): Boolean {
        if (!FoliaPlatformScheduler.isSupported()) {
            return false
        }

        if (plugin.server.javaClass.name.contains("mockbukkit", ignoreCase = true)) {
            return false
        }

        return runCatching {
            val globalScheduler = Bukkit::class.java.getMethod("getGlobalRegionScheduler").invoke(null) ?: return false
            val playerSchedulerType = Player::class.java.methods.firstOrNull { method ->
                method.name == "getScheduler" && method.parameterCount == 0
            }?.returnType ?: return false

            val hasGlobalMethods = globalScheduler.javaClass.methods.any { method ->
                (method.name == "execute" && method.parameterCount == 2) ||
                    (method.name == "run" && method.parameterCount == 2)
            }
            val hasPlayerMethods =
                playerSchedulerType.methods.any { method -> method.name == "run" && method.parameterCount == 3 } &&
                    playerSchedulerType.methods.any { method -> method.name == "runDelayed" && method.parameterCount == 4 } &&
                    playerSchedulerType.methods.any { method -> method.name == "runAtFixedRate" && method.parameterCount == 5 }
            val hasRegionOwnershipChecks = Bukkit::class.java.methods.any { method ->
                method.name == "isOwnedByCurrentRegion"
            }

            hasGlobalMethods && hasPlayerMethods && hasRegionOwnershipChecks
        }.getOrDefault(false)
    }
}
