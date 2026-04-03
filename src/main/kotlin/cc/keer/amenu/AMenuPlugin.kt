package cc.keer.amenu

import cc.keer.amenu.command.AMenuCommand
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.gui.MenuBindingListener
import cc.keer.amenu.gui.MenuListener
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.PlatformSchedulerFactory
import cc.keer.amenu.service.ChatInputService
import cc.keer.amenu.service.MenuService
import cc.keer.amenu.service.provider.MenuProviderRegistry
import cc.keer.amenu.service.provider.ProviderCache
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

open class AMenuPlugin : JavaPlugin() {

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

    override fun onEnable() {
        bootstrapFiles()
        settings = PluginSettings.from(config)
        menuRepository = MenuRepository(this)
        menuRepository.loadMenus()
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
    }

    override fun onDisable() {
        if (::chatInputService.isInitialized) {
            chatInputService.shutdown()
        }
    }

    fun reloadPlugin() {
        reloadConfig()
        settings = PluginSettings.from(config)
        menuRepository.loadMenus()
        menuService.reload(settings)
        chatInputService.reload(settings)
    }

    fun syncBundledDefaults() {
        saveResource("config.yml", true)
        saveBundledResource("menus/menu.yml", true)
        saveBundledResource("menus/showcase.yml", true)
        saveBundledResource("menus/history.yml", true)
        saveBundledResource("menus/admin.yml", true)
        saveBundledResource("menus/runtime.yml", true)
        reloadPlugin()
    }

    private fun bootstrapFiles() {
        saveDefaultConfig()
        saveBundledResource("menus/menu.yml", false)
        saveBundledResource("menus/showcase.yml", false)
        saveBundledResource("menus/history.yml", false)
        saveBundledResource("menus/admin.yml", false)
        saveBundledResource("menus/runtime.yml", false)
    }

    private fun saveBundledResource(path: String, overwrite: Boolean) {
        val target = File(dataFolder, path)
        if (overwrite || !target.exists()) {
            saveResource(path, overwrite)
        }
    }
}
