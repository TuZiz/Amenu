package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.PluginSettings
import cc.keer.amenu.config.PromptDefinition
import cc.keer.amenu.config.PromptType
import cc.keer.amenu.gui.PromptAnvilHolder
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.TaskHandle
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.TextFormatter
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.conversations.Conversation
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerChatEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ChatInputService(
    private val plugin: AMenuPlugin,
    private var settings: PluginSettings,
    private val menuService: MenuService,
    private val platformScheduler: PlatformScheduler,
    private val placeholderPipeline: PlaceholderPipeline = menuService.placeholderPipeline,
) : Listener {

    private val sessions = ConcurrentHashMap<UUID, InputSession>()

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

        replaceExistingSession(player)

        val timeoutTask = platformScheduler.runLaterFor(
            player,
            (prompt.timeoutSeconds ?: settings.chatTimeoutSeconds) * 20L,
            Runnable { expire(player.uniqueId) },
        )
        val session = InputSession(
            menuId = menuId,
            prompt = prompt,
            placeholders = placeholders,
            timeoutTask = timeoutTask,
        )
        sessions[player.uniqueId] = session

        player.closeInventory()
        prompt.startMessages.forEach { line ->
            menuService.sendRawMessage(player, line, placeholders)
        }

        when (prompt.type) {
            PromptType.CHAT -> if (supportsConversationCapture()) {
                openChatPrompt(player, session)
            }

            PromptType.SIGN -> openSignPrompt(player, session)
            PromptType.ANVIL -> openAnvilPrompt(player, session)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onLegacyAsyncChat(event: AsyncPlayerChatEvent) {
        val session = acquireChatSession(event.player) ?: return
        session.timeoutTask.cancel()
        event.isCancelled = true
        platformScheduler.executeFor(event.player, Runnable { handleSubmittedText(event.player, session, event.message.trim()) })
    }

    @Suppress("DEPRECATION")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onSyncLegacyChat(event: PlayerChatEvent) {
        val session = acquireChatSession(event.player) ?: return
        session.timeoutTask.cancel()
        event.isCancelled = true
        handleSubmittedText(event.player, session, event.message.trim())
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val holder = event.inventory.holder as? PromptAnvilHolder ?: return
        val player = event.view.player as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        val runtime = session.runtime as? InputRuntime.Anvil ?: return
        if (runtime.holder !== holder) {
            return
        }

        val renameText = normalizeAnvilText(
            extractRenameText(event.view)
                ?.takeIf { it.isNotBlank() }
                ?: renderPromptText(player, session.prompt.anvilText.orEmpty(), session.placeholders),
            runtime,
        )
        runtime.currentText = renameText

        applyAnvilViewState(event.view)
        event.result = null
        event.inventory.setItem(1, ItemStack(Material.AIR))
        event.inventory.setItem(2, ItemStack(Material.AIR))
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder as? PromptAnvilHolder ?: return
        val player = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        val runtime = session.runtime as? InputRuntime.Anvil ?: return
        if (runtime.holder !== holder) {
            return
        }
        event.isCancelled = true
        event.currentItem = ItemStack(Material.AIR)
        player.setItemOnCursor(ItemStack(Material.AIR))
        if (event.clickedInventory == event.view.topInventory) {
            event.view.topInventory.setItem(1, ItemStack(Material.AIR))
            event.view.topInventory.setItem(2, ItemStack(Material.AIR))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val holder = event.view.topInventory.holder as? PromptAnvilHolder ?: return
        val player = event.whoClicked as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        val runtime = session.runtime as? InputRuntime.Anvil ?: return
        if (runtime.holder !== holder) {
            return
        }
        if (event.rawSlots.any { it < event.view.topInventory.size }) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? PromptAnvilHolder ?: return
        val player = event.player as? Player ?: return
        val session = sessions[player.uniqueId] ?: return
        val runtime = session.runtime as? InputRuntime.Anvil ?: return
        if (runtime.holder !== holder) {
            return
        }
        if (runtime.submitting) {
            return
        }

        sessions.remove(player.uniqueId, session)
        session.dispose()
        val finalText = extractRenameText(event.view)
            ?.takeIf { it.isNotBlank() }
            ?.let { normalizeAnvilText(it, runtime) }
            ?: runtime.currentText
        if (finalText.isNotBlank() && finalText != runtime.initialText) {
            handleSubmittedText(player, session, finalText)
            return
        }
        menuService.executeActions(player, session.menuId, session.prompt.cancelActions, session.placeholders)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        sessions.remove(event.player.uniqueId)?.dispose()
    }

    @EventHandler
    fun onPlayerKick(event: PlayerKickEvent) {
        sessions.remove(event.player.uniqueId)?.dispose()
    }

    private fun replaceExistingSession(player: Player) {
        sessions.remove(player.uniqueId)?.let {
            it.dispose()
            menuService.sendSystemMessage(player, "prompt-replaced")
        }
    }

    private fun acquireChatSession(player: Player): InputSession? {
        val session = sessions[player.uniqueId] ?: return null
        if (session.prompt.type != PromptType.CHAT) {
            return null
        }
        if (!sessions.remove(player.uniqueId, session)) {
            return null
        }
        return session
    }

    private fun handleSubmittedText(
        player: Player,
        session: InputSession,
        input: String,
        extraPlaceholders: Map<String, String> = emptyMap(),
    ) {
        val normalized = input.trim()
        if (session.prompt.type == PromptType.SIGN) {
            plugin.logger.info("[AMenu] Processing sign input for ${player.name}: '$normalized'")
        }
        val cancelKeywords = settings.globalCancelKeywords + session.prompt.cancelKeywords
        if (cancelKeywords.contains(normalized.lowercase())) {
            if (session.prompt.type == PromptType.SIGN) {
                plugin.logger.info("[AMenu] Sign input cancelled by keyword for ${player.name}.")
            }
            menuService.executeActions(player, session.menuId, session.prompt.cancelActions, session.placeholders)
            return
        }

        val valid = matchesPromptValidation(session.prompt, normalized)
        if (session.prompt.type == PromptType.SIGN) {
            plugin.logger.info("[AMenu] Sign input validation result for ${player.name}: valid=$valid")
        }
        if (!valid) {
            val placeholders = session.placeholders +
                extraPlaceholders +
                mapOf("input" to normalized)
            val actions = session.prompt.invalidActions.ifEmpty { session.prompt.cancelActions }
            menuService.executeActions(player, session.menuId, actions, placeholders)
            return
        }

        val placeholders = session.placeholders +
            extraPlaceholders +
            mapOf("input" to normalized)
        menuService.executeActions(player, session.menuId, session.prompt.submitActions, placeholders)
    }

    private fun openSignPrompt(player: Player, session: InputSession) {
        plugin.logger.warning(
            "[AMenu] SIGN prompt is disabled because safe virtual sign input requires an optional packet adapter. " +
                "Use CHAT or ANVIL prompt instead. No world block was modified for ${player.name}.",
        )
        failUnsupportedPrompt(player, session, "sign")
    }

    private fun openChatPrompt(player: Player, session: InputSession) {
        val conversation = ConversationFactory(plugin)
            .withModality(false)
            .withLocalEcho(false)
            .withTimeout((session.prompt.timeoutSeconds ?: settings.chatTimeoutSeconds).toInt())
            .withFirstPrompt(ChatCapturePrompt(this, player.uniqueId, session))
            .buildConversation(player)
        session.runtime = InputRuntime.Chat(conversation)
        conversation.begin()
    }

    private fun openAnvilPrompt(player: Player, session: InputSession) {
        val holder = PromptAnvilHolder(player.uniqueId)
        val initialText = renderPromptText(player, session.prompt.anvilText.orEmpty(), session.placeholders)
        val runtime = InputRuntime.Anvil(holder, initialText, initialText)
        session.runtime = runtime
        plugin.logger.info("[AMenu] Opening anvil prompt for ${player.name} with initialText='${initialText}'")
        platformScheduler.runLaterFor(player, 1L, Runnable {
            val current = sessions[player.uniqueId]
            if (current !== session) {
                return@Runnable
            }

            val inventory = runCatching {
                val title = renderPromptText(player, session.prompt.anvilTitle.orEmpty(), session.placeholders)
                if (title.isBlank()) {
                    Bukkit.createInventory(holder, InventoryType.ANVIL)
                } else {
                    Bukkit.createInventory(holder, InventoryType.ANVIL, title)
                }
            }.getOrElse {
                failUnsupportedPrompt(player, session, "anvil")
                return@Runnable
            }
            holder.bind(inventory)
            inventory.setItem(0, createAnvilInput(initialText))
            inventory.setItem(1, ItemStack(Material.AIR))
            inventory.setItem(2, ItemStack(Material.AIR))
            player.openInventory(inventory)
            applyAnvilViewState(player.openInventory)
            runtime.pollTask = platformScheduler.runRepeatingFor(player, 1L, 1L, Runnable {
                pollAnvilInput(player.uniqueId, session)
            })
        })
    }

    private fun createAnvilInput(initialText: String): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta ?: return item
        val rendered = if (initialText.isBlank()) "\u200B" else initialText
        AdventureAccess.applyDisplayName(meta, TextFormatter.component(rendered))
        item.itemMeta = meta
        return item
    }

    private fun createAnvilResult(text: String): ItemStack {
        val item = ItemStack(Material.NAME_TAG)
        val meta = item.itemMeta ?: return item
        val rendered = if (text.isBlank()) "<gray>点击提交</gray>" else text
        AdventureAccess.applyDisplayName(meta, TextFormatter.component(rendered))
        item.itemMeta = meta
        return item
    }

    private fun failUnsupportedPrompt(player: Player, session: InputSession, type: String) {
        sessions.remove(player.uniqueId, session)
        session.dispose()
        val chineseType = when (type.lowercase()) {
            "chat" -> "聊天"
            "sign" -> "告示牌"
            "anvil" -> "铁砧"
            else -> type.uppercase()
        }
        menuService.sendRawMessage(player, "<red>${chineseType}输入在当前服务端上不可用。</red>")
    }

    private fun renderPromptText(player: Player, text: String, placeholders: Map<String, String>): String {
        return placeholderPipeline.render(player, text, placeholders)
    }

    private fun applyAnvilViewState(view: org.bukkit.inventory.InventoryView) {
        runCatching {
            view.javaClass.methods.firstOrNull { it.name == "setRepairCost" && it.parameterCount == 1 }
                ?.invoke(view, 0)
            view.javaClass.methods.firstOrNull { it.name == "setMaximumRepairCost" && it.parameterCount == 1 }
                ?.invoke(view, 0)
        }
    }

    private fun extractRenameText(view: org.bukkit.inventory.InventoryView): String? {
        val method = view.javaClass.methods.firstOrNull { it.name == "getRenameText" && it.parameterCount == 0 } ?: return null
        return runCatching { method.invoke(view) as? String }.getOrNull()
    }

    private fun findTemporaryLocation(player: Player) = sequence {
        val base = player.location.block.location
        val topY = (player.world.maxHeight - 2).toDouble()
        yield(base.clone().apply { y = topY })
        yield(base.clone().add(1.0, 0.0, 0.0).apply { y = topY })
        yield(base.clone().add(-1.0, 0.0, 0.0).apply { y = topY })
        yield(base.clone().add(0.0, 0.0, 1.0).apply { y = topY })
        yield(base.clone().add(0.0, 0.0, -1.0).apply { y = topY })
        yield(base.clone().apply { y = (player.world.minHeight + 1).toDouble() })
    }.firstOrNull { candidate ->
        candidate.block.type.isAir
    }

    private fun expire(playerId: UUID) {
        val session = sessions.remove(playerId) ?: return
        session.dispose()
        val player = plugin.server.getPlayer(playerId) ?: return
        if (session.runtime is InputRuntime.Anvil) {
            player.closeInventory()
        }
        menuService.sendSystemMessage(player, "prompt-timeout")
    }

    private fun pollAnvilInput(playerId: UUID, expectedSession: InputSession) {
        val session = sessions[playerId] ?: return
        if (session !== expectedSession) {
            return
        }
        val runtime = session.runtime as? InputRuntime.Anvil ?: return
        val player = plugin.server.getPlayer(playerId) ?: return
        val view = player.openInventory
        if (view.topInventory.holder !== runtime.holder) {
            return
        }
        val renameText = extractRenameText(view)
            ?.takeIf { it.isNotBlank() }
            ?: runtime.initialText
        val normalized = normalizeAnvilText(renameText, runtime)
        if (normalized == runtime.currentText) {
            return
        }
        runtime.currentText = normalized
        applyAnvilViewState(view)
    }

    private fun normalizeAnvilText(text: String, runtime: InputRuntime.Anvil): String {
        var normalized = text
        if (runtime.initialText.isBlank()) {
            normalized = normalized.trimStart { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
            val prefixes = listOf("请输入内容", "输入内容", "Enter name", "Enter text")
            val prefix = prefixes.firstOrNull { normalized.startsWith(it) }
            if (prefix != null) {
                normalized = normalized.removePrefix(prefix).trimStart { it.isWhitespace() || it == '\u200B' || it == '\uFEFF' }
            }
        }
        return normalized
    }

    private fun cancelAll() {
        sessions.values.forEach { session ->
            session.dispose()
        }
        sessions.clear()
    }

    private fun supportsConversationCapture(): Boolean {
        return !plugin.server.javaClass.name.contains("mockbukkit", ignoreCase = true)
    }

    private class ChatCapturePrompt(
        private val service: ChatInputService,
        private val playerId: UUID,
        private val expectedSession: InputSession,
    ) : StringPrompt() {

        override fun getPromptText(context: ConversationContext): String = ""

        override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
            val player = Bukkit.getPlayer(playerId) ?: return null
            val session = service.sessions[playerId]
            if (session !== expectedSession) {
                return null
            }
            if (!service.sessions.remove(playerId, expectedSession)) {
                return null
            }

            expectedSession.timeoutTask.cancel()
            service.platformScheduler.executeFor(player, Runnable {
                service.handleSubmittedText(player, expectedSession, input.orEmpty().trim())
            })
            return null
        }
    }

    private fun matchesPromptValidation(prompt: PromptDefinition, input: String): Boolean {
        val validation = prompt.validation ?: return true
        validation.equals?.let { expected ->
            val matched = if (validation.ignoreCase) {
                input.equals(expected, ignoreCase = true)
            } else {
                input == expected
            }
            if (!matched) {
                return false
            }
        }
        validation.matches?.let { regex ->
            if (!regex.matches(input)) {
                return false
            }
        }
        return true
    }

    private fun isSameBlock(first: org.bukkit.Location, second: org.bukkit.Location): Boolean {
        return first.world?.uid == second.world?.uid &&
            first.blockX == second.blockX &&
            first.blockY == second.blockY &&
            first.blockZ == second.blockZ
    }

    private fun isNearbyBlock(first: org.bukkit.Location, second: org.bukkit.Location, maxDistance: Double): Boolean {
        if (first.world?.uid != second.world?.uid) {
            return false
        }
        val dx = (first.blockX - second.blockX).toDouble()
        val dy = (first.blockY - second.blockY).toDouble()
        val dz = (first.blockZ - second.blockZ).toDouble()
        return dx * dx + dy * dy + dz * dz <= maxDistance * maxDistance
    }

    private fun formatLocation(location: org.bukkit.Location): String {
        val worldName = location.world?.name ?: "unknown"
        return "$worldName(${location.blockX},${location.blockY},${location.blockZ})"
    }
}

private class InputSession(
    val menuId: String,
    val prompt: PromptDefinition,
    val placeholders: Map<String, String>,
    val timeoutTask: TaskHandle,
) {
    var runtime: InputRuntime = InputRuntime.Chat()

    fun dispose() {
        timeoutTask.cancel()
        when (val active = runtime) {
            is InputRuntime.Chat -> {
                active.conversation?.let { conversation ->
                    runCatching { conversation.abandon() }
                }
            }
            is InputRuntime.Sign -> Unit

            is InputRuntime.Anvil -> {
                active.pollTask.cancel()
            }
        }
    }
}

private sealed interface InputRuntime {
    data class Chat(val conversation: Conversation? = null) : InputRuntime
    data object Sign : InputRuntime
    data class Anvil(
        val holder: PromptAnvilHolder,
        val initialText: String,
        var currentText: String,
        var submitting: Boolean = false,
        var pollTask: TaskHandle = TaskHandle.NOOP,
    ) : InputRuntime
}
