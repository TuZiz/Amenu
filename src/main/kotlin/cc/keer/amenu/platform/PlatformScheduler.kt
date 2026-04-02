package cc.keer.amenu.platform

import org.bukkit.entity.Player

interface PlatformScheduler {
    val isFolia: Boolean

    fun isPlayerThread(player: Player): Boolean

    fun executeFor(player: Player, task: Runnable)

    fun executeGlobal(task: Runnable)

    fun runLaterFor(player: Player, delayTicks: Long, task: Runnable): TaskHandle

    fun runRepeatingFor(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle
}
