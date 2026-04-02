package cc.keer.amenu.platform

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class BukkitPlatformScheduler(private val plugin: JavaPlugin) : PlatformScheduler {

    override val isFolia: Boolean = false

    override fun isPlayerThread(player: Player): Boolean = Bukkit.isPrimaryThread()

    override fun executeFor(player: Player, task: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            task.run()
            return
        }

        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun executeGlobal(task: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            task.run()
            return
        }

        Bukkit.getScheduler().runTask(plugin, task)
    }

    override fun runLaterFor(player: Player, delayTicks: Long, task: Runnable): TaskHandle {
        val scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks.coerceAtLeast(0L))
        return BukkitTaskHandle(scheduledTask)
    }

    override fun runRepeatingFor(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle {
        val scheduledTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            task,
            delayTicks.coerceAtLeast(0L),
            periodTicks.coerceAtLeast(1L),
        )
        return BukkitTaskHandle(scheduledTask)
    }

    private class BukkitTaskHandle(private val task: BukkitTask) : TaskHandle {
        override fun cancel() {
            task.cancel()
        }
    }
}
