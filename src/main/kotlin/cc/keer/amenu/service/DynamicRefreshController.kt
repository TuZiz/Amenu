package cc.keer.amenu.service

import cc.keer.amenu.config.MenuDefinition
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.TaskHandle
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DynamicRefreshController(
    private val platformScheduler: PlatformScheduler,
) {

    private data class RefreshKey(
        val viewerId: UUID,
        val menuId: String,
        val targetType: RefreshTargetType,
        val targetId: String,
    )

    private data class RefreshRegistration(
        val intervalTicks: Long,
        val handle: TaskHandle,
    )

    private val registrations = ConcurrentHashMap<RefreshKey, RefreshRegistration>()

    fun sync(
        player: Player,
        menu: MenuDefinition,
        onRefreshSurface: (String) -> Unit,
        onRefreshButton: (Char) -> Unit,
    ) {
        val viewerId = player.uniqueId
        val desiredSurfaces = menu.pageRegions.values
            .mapNotNull { region ->
                val interval = region.provider?.update?.interval?.takeIf { it > 0L } ?: return@mapNotNull null
                RefreshKey(viewerId, menu.id, RefreshTargetType.SURFACE, region.id) to interval
            }
        val desiredButtons = menu.buttons.values
            .mapNotNull { button ->
                val interval = button.updateIntervalTicks?.takeIf { it > 0L } ?: return@mapNotNull null
                RefreshKey(viewerId, menu.id, RefreshTargetType.BUTTON, button.symbol.toString()) to interval
            }
        val desired = (desiredSurfaces + desiredButtons)
            .toMap()

        registrations.keys
            .filter { key -> key.viewerId == viewerId && (key.menuId != menu.id || key !in desired.keys) }
            .forEach(::cancel)

        desired.forEach { (key, interval) ->
            val existing = registrations[key]
            if (existing != null && existing.intervalTicks == interval) {
                return@forEach
            }
            cancel(key)
            val handle = platformScheduler.runRepeatingFor(
                player,
                interval,
                interval,
                Runnable {
                    when (key.targetType) {
                        RefreshTargetType.SURFACE -> onRefreshSurface(key.targetId)
                        RefreshTargetType.BUTTON -> key.targetId.firstOrNull()?.let(onRefreshButton)
                    }
                },
            )
            registrations[key] = RefreshRegistration(intervalTicks = interval, handle = handle)
        }
    }

    fun cancelMenu(viewerId: UUID, menuId: String) {
        registrations.keys
            .filter { key -> key.viewerId == viewerId && key.menuId == menuId }
            .forEach(::cancel)
    }

    fun cancelAllForPlayer(viewerId: UUID) {
        registrations.keys
            .filter { key -> key.viewerId == viewerId }
            .forEach(::cancel)
    }

    fun cancelAll() {
        registrations.keys.toList().forEach(::cancel)
    }

    private fun cancel(key: RefreshKey) {
        registrations.remove(key)?.handle?.cancel()
    }

    private enum class RefreshTargetType {
        SURFACE,
        BUTTON,
    }
}
