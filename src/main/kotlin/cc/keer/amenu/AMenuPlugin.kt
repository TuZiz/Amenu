package cc.keer.amenu

import cc.keer.amenu.command.AMenuCommand
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.gui.MenuBindingListener
import cc.keer.amenu.gui.MenuListener
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.PlatformSchedulerFactory
import cc.keer.amenu.service.ChatInputService
import cc.keer.amenu.service.ConfigHotReloadService
import cc.keer.amenu.service.MenuFileChanges
import cc.keer.amenu.service.MenuService
import cc.keer.amenu.service.provider.MenuProviderRegistry
import cc.keer.amenu.service.provider.ProviderCache
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.JarURLConnection
import java.util.jar.JarFile

open class AMenuPlugin : JavaPlugin() {

    private val bundledMenusMarkerName = ".bundled-menus-initialized"

    lateinit var settings: PluginSettings
        private set

    lateinit var menuRepository: MenuRepository
        private set

    lateinit var menuService: MenuService
        private set

    lateinit var chatInputService: ChatInputService
        private set

    lateinit var platformScheduler: PlatformScheduler
        private set

    lateinit var menuProviderRegistry: MenuProviderRegistry
        private set

    lateinit var providerCache: ProviderCache
        private set

    lateinit var configHotReloadService: ConfigHotReloadService
        private set

    override fun onEnable() {
        bootstrapFiles()
        settings = PluginSettings.from(config)
        menuRepository = MenuRepository(this)
        val initialReport = menuRepository.loadMenus()
        platformScheduler = PlatformSchedulerFactory.create(this)
        menuProviderRegistry = MenuProviderRegistry.createBuiltins(platformScheduler)
        providerCache = ProviderCache()
        menuService = MenuService(
            plugin = this,
            settings = settings,
            repository = menuRepository,
            platformScheduler = platformScheduler,
            providerRegistry = menuProviderRegistry,
            providerCache = providerCache,
        )
        chatInputService = ChatInputService(this, settings, menuService, platformScheduler)
        menuService.attachChatInputService(chatInputService)

        server.pluginManager.registerEvents(MenuListener(menuService), this)
        server.pluginManager.registerEvents(MenuBindingListener(this, menuRepository, menuService), this)
        server.pluginManager.registerEvents(chatInputService, this)

        val command = AMenuCommand(this)
        getCommand("amenu")?.setExecutor(command)
        getCommand("amenu")?.tabCompleter = command

        configHotReloadService = ConfigHotReloadService(this, platformScheduler)
        configHotReloadService.start()
        handleReloadReport(
            ReloadReport(
                menuReport = initialReport,
                configReloaded = true,
            ),
            initiator = "startup",
        )
    }

    override fun onDisable() {
        if (::menuService.isInitialized) {
            menuService.shutdown()
        }
        if (::chatInputService.isInitialized) {
            chatInputService.shutdown()
        }
        if (::configHotReloadService.isInitialized) {
            configHotReloadService.stop()
        }
    }

    fun reloadPlugin(): ReloadReport {
        reloadConfig()
        settings = PluginSettings.from(config)
        val menuReport = menuRepository.loadMenus()
        menuService.reload(settings)
        chatInputService.reload(settings)
        return ReloadReport(
            menuReport = menuReport,
            configReloaded = true,
        )
    }

    fun reloadChangedFiles(changes: MenuFileChanges): ReloadReport {
        if (changes.isEmpty) {
            return ReloadReport(
                menuReport = cc.keer.amenu.config.MenuLoadReport(
                    scannedFiles = 0,
                    loadedMenus = 0,
                    applied = true,
                    errors = emptyList(),
                ),
                configReloaded = false,
            )
        }

        var configReloaded = false
        if (changes.configChanged) {
            reloadConfig()
            settings = PluginSettings.from(config)
            if (::menuService.isInitialized) {
                menuService.reloadSettings(settings)
            }
            if (::chatInputService.isInitialized) {
                chatInputService.reload(settings)
            }
            configReloaded = true
        }

        val menuReport = if (changes.upsertedMenus.isNotEmpty() || changes.deletedMenus.isNotEmpty()) {
            menuRepository.applyFileChanges(changes.upsertedMenus, changes.deletedMenus).also { report ->
                if (report.applied && ::menuService.isInitialized) {
                    menuService.handleMenuDefinitionsChanged(
                        changedMenuIds = report.changedMenuIds,
                        reopenMenuIds = report.reopenMenuIds,
                        removedMenuIds = report.removedMenuIds,
                    )
                }
            }
        } else {
            cc.keer.amenu.config.MenuLoadReport(
                scannedFiles = 0,
                loadedMenus = 0,
                applied = true,
                errors = emptyList(),
            )
        }

        return ReloadReport(
            menuReport = menuReport,
            configReloaded = configReloaded,
        )
    }

    fun handleReloadReport(
        report: ReloadReport,
        initiator: String,
    ) {
        if (report.menuReport.successful) {
            logger.info("AMenu reload ($initiator) completed: ${report.menuReport.loadedMenus}/${report.menuReport.scannedFiles} menu files loaded.")
            return
        }

        val summary = buildString {
            append("AMenu reload (")
            append(initiator)
            append(") found ")
            append(report.menuReport.errors.size)
            append(" menu error(s)")
            if (!report.menuReport.applied) {
                append(" and preserved the previous loaded menus")
            }
            append('.')
        }
        logger.severe(summary)
        report.menuReport.errors.forEach { error ->
            logger.severe(" - ${error.fileName}: ${error.message}")
        }

        if (::menuService.isInitialized) {
            val adminMessage = buildString {
                append("<red>检测到菜单配置错误，已保留旧菜单。</red>")
                append(" <gray>共 ")
                append(report.menuReport.errors.size)
                append(" 个错误，请查看控制台。</gray>")
            }
            server.onlinePlayers
                .filter { it.hasPermission("amenu.admin") }
                .forEach { player -> menuService.sendRawMessage(player, adminMessage) }
        }
    }

    private fun bootstrapFiles() {
        val configFile = File(dataFolder, "config.yml")
        val menuFolder = File(dataFolder, "menus")
        val firstStartup = !configFile.exists() && !menuFolder.exists()
        saveDefaultConfig()
        val marker = File(dataFolder, bundledMenusMarkerName)
        if (marker.exists()) {
            return
        }
        if (!firstStartup) {
            marker.parentFile?.mkdirs()
            marker.writeText("initialized=true" + System.lineSeparator(), Charsets.UTF_8)
            return
        }

        val bundledResources = bundledMenuResources()
        bundledResources.forEach { path ->
            saveBundledResource(path, false)
        }
        marker.parentFile?.mkdirs()
        marker.writeText("initialized=true" + System.lineSeparator(), Charsets.UTF_8)
    }

    private fun saveBundledResource(path: String, overwrite: Boolean) {
        val target = File(dataFolder, path)
        if (overwrite || !target.exists()) {
            saveResource(path, overwrite)
        }
    }

    private fun bundledMenuResources(): List<String> {
        val fromCodeSource = bundledMenuResourcesFromCodeSource()
        if (fromCodeSource.isNotEmpty()) {
            return fromCodeSource
        }
        return bundledMenuResourcesFromClassLoader()
    }

    private fun bundledMenuResourcesFromCodeSource(): List<String> {
        val location = runCatching {
            File(javaClass.protectionDomain.codeSource.location.toURI())
        }.getOrNull() ?: return emptyList()

        return when {
            location.isFile -> JarFile(location).use { jar ->
                jar.entries().asSequence()
                    .map { it.name }
                    .filter(::isBundledMenuResource)
                    .sorted()
                    .toList()
            }

            location.isDirectory -> {
                bundledResourceRoots().flatMap { rootName ->
                    val root = File(location, rootName)
                    if (!root.exists()) {
                        emptyList()
                    } else {
                        root.walkTopDown()
                            .filter { it.isFile && it.extension.equals("yml", ignoreCase = true) }
                            .map { "$rootName/" + it.relativeTo(root).invariantSeparatorsPath }
                            .toList()
                    }
                }.sorted()
            }

            else -> emptyList()
        }
    }

    private fun bundledMenuResourcesFromClassLoader(): List<String> {
        return bundledResourceRoots().flatMap { rootName ->
            val url = javaClass.classLoader.getResource(rootName) ?: return@flatMap emptyList()
            when (url.protocol) {
                "file" -> {
                    val root = File(url.toURI())
                    root.walkTopDown()
                        .filter { it.isFile && it.extension.equals("yml", ignoreCase = true) }
                        .map { "$rootName/" + it.relativeTo(root).invariantSeparatorsPath }
                        .toList()
                }

                "jar" -> {
                    val connection = url.openConnection() as? JarURLConnection ?: return@flatMap emptyList()
                    connection.jarFile.entries().asSequence()
                        .map { it.name }
                        .filter(::isBundledMenuResource)
                        .toList()
                }

                else -> emptyList()
            }
        }.distinct().sorted()
    }

    private fun isBundledMenuResource(path: String): Boolean {
        return bundledResourceRoots().any { root -> path.startsWith("$root/") } && path.endsWith(".yml", ignoreCase = true)
    }

    private fun bundledResourceRoots(): List<String> {
        return listOf("templates")
    }
}

data class ReloadReport(
    val menuReport: cc.keer.amenu.config.MenuLoadReport,
    val configReloaded: Boolean,
)
