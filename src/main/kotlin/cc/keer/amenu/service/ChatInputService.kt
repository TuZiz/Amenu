package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.PluginSettings
import cc.keer.amenu.config.PromptDefinition
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.TaskHandle
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatInputService(
    private val plugin: AMenuPlugin,
    private var settings: PluginSettings,
    private val menuService: MenuService,
    private val platformScheduler: PlatformScheduler,
) : Listener {

    private val sessions = ConcurrentHashMap<UUID, ChatInputSession>()

    fun reload(newSettings: PluginSettings) {
        settings = newSettings
        cancelAll()
    }

    fun shutdown() {
        cancelAll()
    }

    fun startPrompt(
        player: Player,
        menuId: String,
        promptId: String,
        placeholders: Map<String, String>,
    ) {
        val menu = menuService.repository.menu(menuId) ?: run {
            menuService.sendSystemMessage(player, "menu-missing", mapOf("menu" to menuId))
            return
        }
        val prompt = menu.prompts[promptId] ?: run {
            menuService.sendSystemMessage(player, "prompt-missing", mapOf("prompt" to promptId))
            return
        }

        sessions.remove(player.uniqueId)?.let {
            it.timeoutTask.cancel()
            menuService.sendSystemMessage(player, "prompt-replaced")
        }

        val timeoutTask = platformScheduler.runLaterFor(
            player,
            settings.chatTimeoutSeconds * 20L,
            Runnable { expire(player.uniqueId) },
        )
        sessions[player.uniqueId] = ChatInputSession(
            menuId = menuId,
            prompt = prompt,
            placeholders = placeholders,
            timeoutTask = timeoutTask,
        )

        player.closeInventory()
        prompt.startMessages.forEach { line ->
            menuService.sendRawMessage(player, line, placeholders)
        }
    }

    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        val session = sessions.remove(event.player.uniqueId) ?: return
        session.timeoutTask.cancel()
        event.isCancelled = true

        val message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim()
        platformScheduler.executeFor(event.player, Runnable { complete(event.player, session, message) })
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        sessions.remove(event.player.uniqueId)?.timeoutTask?.cancel()
    }

    private fun complete(player: Player, session: ChatInputSession, input: String) {
        val cancelKeywords = settings.globalCancelKeywords + session.prompt.cancelKeywords
        if (cancelKeywords.contains(input.lowercase())) {
            menuService.executeActions(player, session.menuId, session.prompt.cancelActions, session.placeholders)
            return
        }

        val placeholders = session.placeholders + mapOf("input" to input)
        menuService.executeActions(player, session.menuId, session.prompt.submitActions, placeholders)
    }

    private fun expire(playerId: UUID) {
        val session = sessions.remove(playerId) ?: return
        session.timeoutTask.cancel()
        val player = plugin.server.getPlayer(playerId) ?: return
        menuService.sendSystemMessage(player, "prompt-timeout")
    }

    private fun cancelAll() {
        sessions.values.forEach { it.timeoutTask.cancel() }
        sessions.clear()
    }
}

private data class ChatInputSession(
    val menuId: String,
    val prompt: PromptDefinition,
    val placeholders: Map<String, String>,
    val timeoutTask: TaskHandle,
)
