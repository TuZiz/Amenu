package cc.keer.amenu

import cc.keer.amenu.command.AMenuCommand
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.gui.MenuBindingListener
import cc.keer.amenu.gui.MenuListener
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.PlatformSchedulerFactory
import cc.keer.amenu.service.ChatInputService
import cc.keer.amenu.service.ConfigHotReloadService
import cc.keer.amenu.service.MenuService
import cc.keer.amenu.service.provider.MenuProviderRegistry
import cc.keer.amenu.service.provider.ProviderCache
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

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

        bundledMenuResources().forEach { path ->
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
        return listOf(
            "menus/menu.yml",
            "menus/pay.yml",
            "menus/showcase.yml",
            "menus/history.yml",
            "menus/admin.yml",
            "menus/runtime.yml",
            "menus/skin.yml",
        )
    }
}

data class ReloadReport(
    val menuReport: cc.keer.amenu.config.MenuLoadReport,
    val configReloaded: Boolean,
)
