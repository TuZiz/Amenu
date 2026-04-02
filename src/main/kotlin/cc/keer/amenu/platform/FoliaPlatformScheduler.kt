package cc.keer.amenu.platform

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

class FoliaPlatformScheduler(private val plugin: JavaPlugin) : PlatformScheduler {

    private val fallback = BukkitPlatformScheduler(plugin)
    private val bridge = ReflectiveFoliaBridge(plugin)

    override val isFolia: Boolean = true

    override fun isPlayerThread(player: Player): Boolean = bridge.isPlayerThread(player)

    override fun executeFor(player: Player, task: Runnable) {
        if (bridge.isPlayerThread(player)) {
            task.run()
            return
        }

        if (!bridge.executeForNow(player, task)) {
            fallback.executeFor(player, task)
        }
    }

    override fun executeGlobal(task: Runnable) {
        if (!bridge.executeGlobal(task)) {
            fallback.executeGlobal(task)
        }
    }

    override fun runLaterFor(player: Player, delayTicks: Long, task: Runnable): TaskHandle {
        return if (delayTicks <= 0L) {
            bridge.runNowFor(player, task) ?: fallback.runLaterFor(player, 0L, task)
        } else {
            bridge.runDelayedFor(player, delayTicks, task) ?: fallback.runLaterFor(player, delayTicks, task)
        }
    }

    override fun runRepeatingFor(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle {
        return bridge.runRepeatingFor(
            player = player,
            delayTicks = delayTicks.coerceAtLeast(0L),
            periodTicks = periodTicks.coerceAtLeast(1L),
            task = task,
        ) ?: RecursiveRepeatingTask(
            player = player,
            initialDelayTicks = delayTicks.coerceAtLeast(0L),
            periodTicks = periodTicks.coerceAtLeast(1L),
            task = task,
        ).also(RecursiveRepeatingTask::start)
    }

    private inner class RecursiveRepeatingTask(
        private val player: Player,
        private val initialDelayTicks: Long,
        private val periodTicks: Long,
        private val task: Runnable,
    ) : TaskHandle {
        private val cancelled = AtomicBoolean(false)

        @Volatile
        private var currentHandle: TaskHandle = TaskHandle.NOOP

        fun start() {
            schedule(initialDelayTicks)
        }

        override fun cancel() {
            cancelled.set(true)
            currentHandle.cancel()
        }

        private fun schedule(delayTicks: Long) {
            if (cancelled.get()) {
                return
            }

            currentHandle = runLaterFor(player, delayTicks, Runnable {
                if (cancelled.get()) {
                    return@Runnable
                }

                task.run()
                schedule(periodTicks)
            })
        }
    }

    companion object {
        fun isSupported(): Boolean = Bukkit::class.java.methods.any { it.name == "getGlobalRegionScheduler" }
    }
}

private class ReflectiveFoliaBridge(private val plugin: JavaPlugin) {

    private val bukkitClass = Bukkit::class.java

    fun isPlayerThread(player: Player): Boolean {
        findMethodByParameter(bukkitClass, "isOwnedByCurrentRegion", player.javaClass)?.let { method ->
            return (method.invoke(null, player) as? Boolean) ?: Bukkit.isPrimaryThread()
        }

        val location = player.location
        findMethodByParameter(bukkitClass, "isOwnedByCurrentRegion", location.javaClass)?.let { method ->
            return (method.invoke(null, location) as? Boolean) ?: Bukkit.isPrimaryThread()
        }

        val worldChunkMethod = bukkitClass.methods.firstOrNull { method ->
            method.name == "isOwnedByCurrentRegion" &&
                method.parameterCount == 3 &&
                method.parameterTypes[0].isAssignableFrom(player.world.javaClass) &&
                method.parameterTypes[1] == Integer.TYPE &&
                method.parameterTypes[2] == Integer.TYPE
        }
        if (worldChunkMethod != null) {
            val chunkX = location.blockX shr 4
            val chunkZ = location.blockZ shr 4
            return (worldChunkMethod.invoke(null, player.world, chunkX, chunkZ) as? Boolean)
                ?: Bukkit.isPrimaryThread()
        }

        return Bukkit.isPrimaryThread()
    }

    fun executeGlobal(task: Runnable): Boolean {
        val scheduler = globalScheduler() ?: return false

        findMethod(scheduler.javaClass, "execute", 2)?.let { method ->
            method.invoke(scheduler, plugin, task)
            return true
        }

        findMethod(scheduler.javaClass, "run", 2)?.let { method ->
            method.invoke(scheduler, plugin, Consumer<Any?> { task.run() })
            return true
        }

        return false
    }

    fun executeForNow(player: Player, task: Runnable): Boolean = runNowFor(player, task) != null

    fun runNowFor(player: Player, task: Runnable): TaskHandle? {
        val scheduler = playerScheduler(player) ?: return null
        val retiredTask = Runnable {}

        findMethod(scheduler.javaClass, "run", 3)?.let { method ->
            val scheduledTask = method.invoke(
                scheduler,
                plugin,
                Consumer<Any?> { task.run() },
                retiredTask,
            )
            return ReflectiveTaskHandle(scheduledTask)
        }

        findMethod(scheduler.javaClass, "execute", 4)?.let { method ->
            method.invoke(scheduler, plugin, task, retiredTask, 1L)
            return TaskHandle.NOOP
        }

        return null
    }

    fun runDelayedFor(player: Player, delayTicks: Long, task: Runnable): TaskHandle? {
        val scheduler = playerScheduler(player) ?: return null
        val retiredTask = Runnable {}

        findMethod(scheduler.javaClass, "runDelayed", 4)?.let { method ->
            val scheduledTask = method.invoke(
                scheduler,
                plugin,
                Consumer<Any?> { task.run() },
                retiredTask,
                delayTicks.coerceAtLeast(1L),
            )
            return ReflectiveTaskHandle(scheduledTask)
        }

        findMethod(scheduler.javaClass, "execute", 4)?.let { method ->
            method.invoke(scheduler, plugin, task, retiredTask, delayTicks.coerceAtLeast(1L))
            return TaskHandle.NOOP
        }

        return null
    }

    fun runRepeatingFor(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle? {
        val scheduler = playerScheduler(player) ?: return null
        val retiredTask = Runnable {}

        findMethod(scheduler.javaClass, "runAtFixedRate", 5)?.let { method ->
            val scheduledTask = method.invoke(
                scheduler,
                plugin,
                Consumer<Any?> { task.run() },
                retiredTask,
                delayTicks,
                periodTicks,
            )
            return ReflectiveTaskHandle(scheduledTask)
        }

        return null
    }

    private fun globalScheduler(): Any? = findMethod(bukkitClass, "getGlobalRegionScheduler", 0)?.invoke(null)

    private fun playerScheduler(player: Player): Any? = findMethod(player.javaClass, "getScheduler", 0)?.invoke(player)

    private fun findMethod(type: Class<*>, name: String, parameterCount: Int): Method? {
        return type.methods.firstOrNull { method ->
            method.name == name && method.parameterCount == parameterCount
        }
    }

    private fun findMethodByParameter(type: Class<*>, name: String, argumentType: Class<*>): Method? {
        return type.methods.firstOrNull { method ->
            method.name == name &&
                method.parameterCount == 1 &&
                method.parameterTypes[0].isAssignableFrom(argumentType)
        }
    }
}

private class ReflectiveTaskHandle(private val scheduledTask: Any?) : TaskHandle {
    private val cancelMethod = scheduledTask?.javaClass?.methods?.firstOrNull { method ->
        method.name == "cancel" && method.parameterCount == 0
    }

    override fun cancel() {
        cancelMethod?.invoke(scheduledTask)
    }
}
