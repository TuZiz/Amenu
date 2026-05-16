package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.config.menuFolderFor
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
    private val queueLock = Any()

    @Volatile
    private var pollingTask: TaskHandle = TaskHandle.NOOP

    private var snapshot: Map<String, FileStamp> = emptyMap()
    private var pendingChanges: MenuFileChanges = MenuFileChanges.EMPTY

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
        synchronized(queueLock) {
            pendingChanges = MenuFileChanges.EMPTY
        }
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
            val changes = diffSnapshots(snapshot, current)
            snapshot = current
            queueReload(changes)
        }
    }

    private fun queueReload(changes: MenuFileChanges) {
        synchronized(queueLock) {
            pendingChanges = pendingChanges.merge(changes)
        }
        if (!reloadQueued.compareAndSet(false, true)) {
            return
        }
        val pending = synchronized(queueLock) {
            val next = pendingChanges
            pendingChanges = MenuFileChanges.EMPTY
            next
        }

        java.util.concurrent.CompletableFuture
            .supplyAsync { PreparedHotReload(plugin.prepareChangedFilesReload(pending), captureSnapshot()) }
            .whenComplete { prepared, throwable ->
                scheduler.executeGlobal(Runnable {
                    try {
                        if (throwable != null) {
                            plugin.logger.severe("AMenu auto reload failed while parsing files asynchronously: ${throwable.message}")
                        } else {
                            val report = plugin.commitChangedFilesReload(prepared!!.reload)
                            plugin.handleReloadReport(report, initiator = "auto")
                            snapshot = prepared.snapshot
                        }
                    } finally {
                        reloadQueued.set(false)
                        val hasPendingChanges = synchronized(queueLock) { !pendingChanges.isEmpty }
                        if (hasPendingChanges) {
                            queueReload(MenuFileChanges.EMPTY)
                        }
                    }
                })
            }
    }

    private fun captureSnapshot(): Map<String, FileStamp> {
        val files = buildList {
            add(File(plugin.dataFolder, "config.yml").absoluteFile)
            val menuFolder = menuFolderFor(plugin.dataFolder)
            if (menuFolder.exists()) {
                addAll(
                    menuFolder.walkTopDown()
                        .filter { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
                        .sortedBy { it.absolutePath.lowercase() }
                        .toList(),
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

    private fun diffSnapshots(
        previous: Map<String, FileStamp>,
        current: Map<String, FileStamp>,
    ): MenuFileChanges {
        val configPath = File(plugin.dataFolder, "config.yml").absoluteFile.absolutePath
        val menuFolder = menuFolderFor(plugin.dataFolder)
        var configChanged = false
        val upsertedMenus = linkedSetOf<File>()
        val deletedMenus = linkedSetOf<File>()

        (previous.keys + current.keys).forEach { path ->
            if (previous[path] == current[path]) {
                return@forEach
            }
            if (path == configPath) {
                configChanged = true
                return@forEach
            }
            val file = File(path)
            if (!isMenuPath(menuFolder, file)) {
                return@forEach
            }
            if (current.containsKey(path)) {
                upsertedMenus += file
            } else {
                deletedMenus += file
            }
        }

        return MenuFileChanges(
            configChanged = configChanged,
            upsertedMenus = upsertedMenus,
            deletedMenus = deletedMenus,
        )
    }

    private fun isMenuPath(menuFolder: File, file: File): Boolean {
        if (!file.extension.equals("yml", ignoreCase = true)) {
            return false
        }
        val menuRoot = menuFolder.toPath().normalize()
        val target = file.absoluteFile.toPath().normalize()
        return target.startsWith(menuRoot)
    }

    private data class FileStamp(
        val lastModified: Long,
        val length: Long,
    ) {
        companion object {
            val MISSING = FileStamp(-1L, -1L)
        }
    }

    private data class PreparedHotReload(
        val reload: cc.keer.amenu.PreparedChangedReload,
        val snapshot: Map<String, FileStamp>,
    )
}

data class MenuFileChanges(
    val configChanged: Boolean,
    val upsertedMenus: Set<File>,
    val deletedMenus: Set<File>,
) {
    val isEmpty: Boolean
        get() = !configChanged && upsertedMenus.isEmpty() && deletedMenus.isEmpty()

    fun merge(other: MenuFileChanges): MenuFileChanges {
        if (isEmpty) {
            return other
        }
        if (other.isEmpty) {
            return this
        }
        val deleted = deletedMenus.toMutableSet()
        val upserted = upsertedMenus.toMutableSet()
        other.deletedMenus.forEach { file ->
            upserted.remove(file)
            deleted += file
        }
        other.upsertedMenus.forEach { file ->
            deleted.remove(file)
            upserted += file
        }
        return MenuFileChanges(
            configChanged = configChanged || other.configChanged,
            upsertedMenus = upserted,
            deletedMenus = deleted,
        )
    }

    companion object {
        val EMPTY = MenuFileChanges(
            configChanged = false,
            upsertedMenus = emptySet(),
            deletedMenus = emptySet(),
        )
    }
}
