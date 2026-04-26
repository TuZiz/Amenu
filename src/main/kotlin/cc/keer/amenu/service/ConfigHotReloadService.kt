package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.TaskHandle
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class ConfigHotReloadService(
    private val plugin: AMenuPlugin,
    private val scheduler: PlatformScheduler,
    private val pollIntervalTicks: Long = 20L,
) {

    private val running = AtomicBoolean(false)
    private val reloadQueued = AtomicBoolean(false)

    @Volatile
    private var pollingTask: TaskHandle = TaskHandle.NOOP

    private var snapshot: Map<String, FileStamp> = emptyMap()

    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        snapshot = captureSnapshot()
        scheduleNextPoll()
    }

    fun stop() {
        running.set(false)
        pollingTask.cancel()
        pollingTask = TaskHandle.NOOP
        reloadQueued.set(false)
        snapshot = emptyMap()
    }

    private fun scheduleNextPoll() {
        if (!running.get()) {
            return
        }
        pollingTask = scheduler.runLaterAsync(pollIntervalTicks, Runnable {
            try {
                poll()
            } finally {
                scheduleNextPoll()
            }
        })
    }

    private fun poll() {
        val current = captureSnapshot()
        if (current != snapshot) {
            snapshot = current
            queueReload()
        }
    }

    private fun queueReload() {
        if (!reloadQueued.compareAndSet(false, true)) {
            return
        }
        scheduler.executeGlobal(Runnable {
            try {
                val report = plugin.reloadPlugin()
                plugin.handleReloadReport(report, initiator = "auto")
                snapshot = captureSnapshot()
            } finally {
                reloadQueued.set(false)
            }
        })
    }

    private fun captureSnapshot(): Map<String, FileStamp> {
        val files = buildList {
            add(File(plugin.dataFolder, "config.yml"))
            val menuFolder = File(plugin.dataFolder, "menus")
            if (menuFolder.exists()) {
                addAll(
                    menuFolder.listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
                        ?.sortedBy { it.name.lowercase() }
                        .orEmpty(),
                )
            }
        }
        return files.associate { file ->
            file.absolutePath to if (file.exists()) {
                FileStamp(file.lastModified(), file.length())
            } else {
                FileStamp.MISSING
            }
        }
    }

    private data class FileStamp(
        val lastModified: Long,
        val length: Long,
    ) {
        companion object {
            val MISSING = FileStamp(-1L, -1L)
        }
    }
}
