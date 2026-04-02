package cc.keer.amenu.platform

import cc.keer.amenu.support.MenuPluginTestHarness
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
}
