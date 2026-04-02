package cc.keer.amenu

import cc.keer.amenu.command.AMenuCommand
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.gui.MenuListener
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.PlatformSchedulerFactory
import cc.keer.amenu.service.ChatInputService
import cc.keer.amenu.service.MenuService
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

    override fun onEnable() {
        bootstrapFiles()
        settings = PluginSettings.from(config)
        menuRepository = MenuRepository(this)
        menuRepository.loadMenus()
        platformScheduler = PlatformSchedulerFactory.create(this)
        menuService = MenuService(this, settings, menuRepository, platformScheduler)
        chatInputService = ChatInputService(this, settings, menuService, platformScheduler)
        menuService.attachChatInputService(chatInputService)

        server.pluginManager.registerEvents(MenuListener(menuService), this)
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

    private fun bootstrapFiles() {
        saveDefaultConfig()
        saveBundledResource("menus/main.yml")
        saveBundledResource("menus/history.yml")
        saveBundledResource("menus/admin.yml")
        saveBundledResource("menus/runtime.yml")
    }

    private fun saveBundledResource(path: String) {
        val target = File(dataFolder, path)
        if (!target.exists()) {
            saveResource(path, false)
        }
    }
}
