package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.PluginSettings
import cc.keer.amenu.config.MenuAction
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.config.SoundSpec
import cc.keer.amenu.gui.MenuHolder
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.util.ItemFactory
import cc.keer.amenu.util.TextFormatter
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.ArrayDeque
import java.util.UUID

class MenuService(
    private val plugin: AMenuPlugin,
    private var settings: PluginSettings,
    val repository: MenuRepository,
    private val platformScheduler: PlatformScheduler,
) {

    private lateinit var chatInputService: ChatInputService
    private val history = mutableMapOf<UUID, ArrayDeque<String>>()

    fun attachChatInputService(service: ChatInputService) {
        chatInputService = service
    }

    fun reload(newSettings: PluginSettings) {
        settings = newSettings
    }

    fun openDefaultMenu(player: Player) {
        runForPlayer(player) {
            clearHistory(player)
            openMenuInternal(player, settings.defaultMenuId, emptyMap(), NavigationMode.ROOT)
        }
    }

    fun openMenu(
        player: Player,
        menuId: String,
        placeholders: Map<String, String> = emptyMap(),
        navigation: NavigationMode = NavigationMode.NONE,
    ) {
        runForPlayer(player) {
            openMenuInternal(player, menuId, placeholders, navigation)
        }
    }

    fun handleClick(player: Player, menuId: String, slot: Int) {
        runForPlayer(player) {
            handleClickInternal(player, menuId, slot)
        }
    }

    fun executeActions(
        player: Player,
        menuId: String,
        actions: List<MenuAction>,
        placeholders: Map<String, String> = defaultPlaceholders(player),
    ) {
        runForPlayer(player) {
            actions.forEach { action ->
                executeAction(player, menuId, action, placeholders)
            }
        }
    }

    fun sendSystemMessage(
        receiver: CommandSender,
        key: String,
        placeholders: Map<String, String> = emptyMap(),
    ) {
        receiver.sendMessage(renderSystemText(key, placeholders))
    }

    fun renderSystemText(key: String, placeholders: Map<String, String> = emptyMap()): Component {
        return TextFormatter.component(settings.systemMessage(key, placeholders))
    }

    fun sendRawMessage(
        player: Player,
        text: String,
        placeholders: Map<String, String> = defaultPlaceholders(player),
    ) {
        player.sendMessage(renderComponent(text, placeholders))
    }

    private fun openMenuInternal(
        player: Player,
        menuId: String,
        placeholders: Map<String, String>,
        navigation: NavigationMode,
    ) {
        val menu = repository.menu(menuId) ?: run {
            sendSystemMessage(player, "menu-missing", mapOf("menu" to menuId))
            return
        }

        when (navigation) {
            NavigationMode.ROOT -> clearHistory(player)
            NavigationMode.PUSH_CURRENT -> currentMenuId(player)
                ?.takeIf { it != menuId }
                ?.let { current -> history.getOrPut(player.uniqueId) { ArrayDeque() }.addLast(current) }

            NavigationMode.NONE -> Unit
        }

        val resolvedPlaceholders = defaultPlaceholders(player) + placeholders
        val holder = MenuHolder(menu.id)
        val inventory = Bukkit.createInventory(holder, menu.size, renderComponent(menu.title, resolvedPlaceholders))
        holder.bind(inventory)

        for (slot in 0 until menu.size) {
            val button = menu.buttonAt(slot) ?: continue
            if (button.visiblePermission != null && !player.hasPermission(button.visiblePermission)) {
                continue
            }
            inventory.setItem(slot, ItemFactory.create(button.icon, resolvedPlaceholders))
        }

        player.openInventory(inventory)
    }

    private fun handleClickInternal(player: Player, menuId: String, slot: Int) {
        val menu = repository.menu(menuId) ?: return
        val button = menu.buttonAt(slot) ?: return
        if (button.visiblePermission != null && !player.hasPermission(button.visiblePermission)) {
            return
        }
        if (button.permission != null && !player.hasPermission(button.permission)) {
            if (button.denyActions.isNotEmpty()) {
                executeActions(player, menu.id, button.denyActions, defaultPlaceholders(player))
            } else {
                sendSystemMessage(player, "no-permission")
            }
            return
        }
        executeActions(player, menu.id, button.actions, defaultPlaceholders(player))
    }

    private fun executeAction(
        player: Player,
        menuId: String,
        action: MenuAction,
        placeholders: Map<String, String>,
    ) {
        when (action) {
            MenuAction.Close -> player.closeInventory()
            MenuAction.Back -> openBack(player, placeholders)
            MenuAction.Refresh -> openMenu(player, menuId, placeholders, NavigationMode.NONE)
            is MenuAction.Open -> openMenu(player, action.menuId, placeholders, NavigationMode.PUSH_CURRENT)
            is MenuAction.Prompt -> chatInputService.startPrompt(player, menuId, action.promptId, placeholders)
            is MenuAction.PlayerCommand -> player.performCommand(applyPlaceholders(action.command, placeholders))
            is MenuAction.ConsoleCommand -> plugin.server.dispatchCommand(
                plugin.server.consoleSender,
                applyPlaceholders(action.command, placeholders),
            )

            is MenuAction.Message -> sendRawMessage(player, action.text, placeholders)
            is MenuAction.Sound -> playSound(player, action.spec)
        }
    }

    private fun playSound(player: Player, spec: SoundSpec) {
        val sound = runCatching { Sound.valueOf(spec.soundName) }.getOrNull() ?: return
        player.playSound(player.location, sound, spec.volume, spec.pitch)
    }

    private fun renderComponent(text: String, placeholders: Map<String, String>): Component {
        return TextFormatter.component(applyPlaceholders(text, placeholders))
    }

    private fun openBack(player: Player, placeholders: Map<String, String>) {
        val stack = history[player.uniqueId]
        val previous = if (stack.isNullOrEmpty()) null else stack.removeLast()
        if (stack != null && stack.isEmpty()) {
            history.remove(player.uniqueId)
        }
        openMenuInternal(player, previous ?: settings.defaultMenuId, placeholders, NavigationMode.NONE)
    }

    private fun currentMenuId(player: Player): String? {
        return (player.openInventory.topInventory.holder as? MenuHolder)?.menuId
    }

    private fun clearHistory(player: Player) {
        history.remove(player.uniqueId)
    }

    private fun applyPlaceholders(text: String, placeholders: Map<String, String>): String {
        return placeholders.entries.fold(text) { current, (key, value) ->
            current.replace("{$key}", value)
        }
    }

    private fun defaultPlaceholders(player: Player): Map<String, String> {
        return mapOf("player" to player.name)
    }

    private inline fun runForPlayer(player: Player, crossinline action: () -> Unit) {
        if (platformScheduler.isPlayerThread(player)) {
            action()
            return
        }

        platformScheduler.executeFor(player, Runnable { action() })
    }
}

enum class NavigationMode {
    ROOT,
    PUSH_CURRENT,
    NONE,
}
