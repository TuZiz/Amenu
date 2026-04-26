package cc.keer.amenu.platform

import cc.keer.amenu.support.MenuPluginTestHarness
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class PlatformSchedulerFactoryTest : MenuPluginTestHarness() {

    @Test
    fun mockbukkit_runtime_uses_bukkit_scheduler() {
        val scheduler = PlatformSchedulerFactory.create(plugin)

        assertTrue(scheduler is BukkitPlatformScheduler)
        assertFalse(scheduler.isFolia)
        assertTrue(scheduler.isPlayerThread(player))

        var executed = false
        scheduler.executeFor(player, Runnable { executed = true })

        assertTrue(executed)
    }

    @Test
    fun task_handles_can_cancel_delayed_and_repeating_work() {
        val scheduler = PlatformSchedulerFactory.create(plugin)
        val delayedRuns = AtomicInteger(0)
        val repeatingRuns = AtomicInteger(0)

        val delayedHandle = scheduler.runLaterFor(player, 5L, Runnable { delayedRuns.incrementAndGet() })
        delayedHandle.cancel()
        advanceTicks(6L)
        assertEquals(0, delayedRuns.get())

        val repeatingHandle = scheduler.runRepeatingFor(player, 1L, 1L, Runnable { repeatingRuns.incrementAndGet() })
        advanceTicks(2L)
        assertTrue(repeatingRuns.get() > 0)

        val runsBeforeCancel = repeatingRuns.get()
        repeatingHandle.cancel()
        advanceTicks(3L)
        assertEquals(runsBeforeCancel, repeatingRuns.get())

        TaskHandle.NOOP.cancel()
    }

    @Test
    fun reflective_folia_task_handle_prefers_public_scheduled_task_interface() {
        val task = HiddenScheduledTask()
        val handleClass = Class.forName("cc.keer.amenu.platform.ReflectiveTaskHandle")
        val constructor = handleClass.getDeclaredConstructor(Any::class.java)
        constructor.isAccessible = true
        val handle = constructor.newInstance(task) as TaskHandle

        handle.cancel()

        assertTrue(task.cancelled)
    }

    private class HiddenScheduledTask : ScheduledTask {
        var cancelled = false

        override fun getOwningPlugin() = throw UnsupportedOperationException()

        override fun isRepeatingTask(): Boolean = false

        override fun cancel(): ScheduledTask.CancelledState {
            cancelled = true
            return ScheduledTask.CancelledState.CANCELLED_BY_CALLER
        }

        override fun getExecutionState(): ScheduledTask.ExecutionState = ScheduledTask.ExecutionState.IDLE
    }
}
