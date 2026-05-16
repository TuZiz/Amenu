package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.PluginSettings
import cc.keer.amenu.config.ButtonDefinition
import cc.keer.amenu.config.ComparisonOperator
import cc.keer.amenu.config.IconDefinition
import cc.keer.amenu.config.MenuAction
import cc.keer.amenu.config.MenuCondition
import cc.keer.amenu.config.MenuDefinition
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.config.PageEntryDefinition
import cc.keer.amenu.config.PageOperation
import cc.keer.amenu.config.PageRegionDefinition
import cc.keer.amenu.config.SoundSpec
import cc.keer.amenu.gui.MenuHolder
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.service.provider.MenuProviderRegistry
import cc.keer.amenu.service.provider.ProviderCache
import cc.keer.amenu.service.provider.ProviderRequest
import cc.keer.amenu.service.provider.ProviderResult
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.InventoryAccess
import cc.keer.amenu.util.ItemFactory
import cc.keer.amenu.util.TextFormatter
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.time.Duration
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max

class MenuService(
    private val plugin: AMenuPlugin,
    private var settings: PluginSettings,
    val repository: MenuRepository,
    private val platformScheduler: PlatformScheduler,
    val placeholderPipeline: PlaceholderPipeline = PlaceholderPipeline(BukkitPlaceholderApiBridge(plugin)),
    private val providerRegistry: MenuProviderRegistry = MenuProviderRegistry.createBuiltins(platformScheduler),
    private val providerCache: ProviderCache = ProviderCache(),
    private val dynamicRefreshController: DynamicRefreshController = DynamicRefreshController(platformScheduler),
) {

    private lateinit var chatInputService: ChatInputService
    private val history = ConcurrentHashMap<UUID, ArrayDeque<String>>()
    private val menuStates = ConcurrentHashMap<UUID, MutableMap<String, MenuViewState>>()
    private val closeCleanupSuppressed = ConcurrentHashMap.newKeySet<UUID>()
    private val pendingMenuOpens = ConcurrentHashMap<UUID, cc.keer.amenu.platform.TaskHandle>()

    fun attachChatInputService(service: ChatInputService) {
        chatInputService = service
    }

    fun reload(newSettings: PluginSettings) {
        settings = newSettings
        dynamicRefreshController.cancelAll()
        pendingMenuOpens.values.forEach(cc.keer.amenu.platform.TaskHandle::cancel)
        pendingMenuOpens.clear()
        menuStates.clear()
        providerCache.clear()
    }

    fun reloadSettings(newSettings: PluginSettings) {
        settings = newSettings
    }

    fun handleMenuDefinitionsChanged(
        changedMenuIds: Set<String>,
        reopenMenuIds: Set<String>,
        removedMenuIds: Set<String>,
    ) {
        if (changedMenuIds.isEmpty() && removedMenuIds.isEmpty()) {
            return
        }

        plugin.server.onlinePlayers.forEach { player ->
            runForPlayer(player) {
                cleanupRemovedMenus(player, removedMenuIds)
                val current = currentMenuId(player) ?: return@runForPlayer
                if (current !in changedMenuIds) {
                    return@runForPlayer
                }
                if (current in reopenMenuIds) {
                    val placeholders = menuStates[player.uniqueId]?.get(current)?.placeholders?.toMap().orEmpty()
                    openMenuInternal(player, current, placeholders, NavigationMode.NONE)
                } else {
                    renderOpenMenuIfCurrent(player, current)
                }
            }
        }
    }

    fun shutdown() {
        dynamicRefreshController.cancelAll()
        pendingMenuOpens.values.forEach(cc.keer.amenu.platform.TaskHandle::cancel)
        pendingMenuOpens.clear()
        menuStates.clear()
        history.clear()
        providerCache.clear()
    }

    fun handleInventoryClosed(player: Player, menuId: String) {
        if (closeCleanupSuppressed.contains(player.uniqueId)) {
            return
        }
        dynamicRefreshController.cancelMenu(player.uniqueId, menuId)
        val stateMap = menuStates[player.uniqueId] ?: return
        stateMap.remove(menuId)
        if (stateMap.isEmpty()) {
            menuStates.remove(player.uniqueId)
        }
    }

    fun handlePlayerQuit(player: Player) {
        dynamicRefreshController.cancelAllForPlayer(player.uniqueId)
        cancelPendingMenuOpen(player.uniqueId)
        menuStates.remove(player.uniqueId)
        history.remove(player.uniqueId)
    }

    fun openMenu(
        player: Player,
        menuId: String,
        placeholders: Map<String, String> = emptyMap(),
        navigation: NavigationMode = NavigationMode.NONE,
    ) {
        runForPlayer(player) {
            cancelPendingMenuOpen(player.uniqueId)
            openMenuInternal(player, menuId, placeholders, navigation)
        }
    }

    fun openMenuDeferred(
        player: Player,
        menuId: String,
        placeholders: Map<String, String> = emptyMap(),
        navigation: NavigationMode = NavigationMode.NONE,
        delayTicks: Long = 1L,
    ) {
        val delay = delayTicks.coerceAtLeast(1L)
        runForPlayer(player) {
            cancelPendingMenuOpen(player.uniqueId)
            pendingMenuOpens[player.uniqueId] = platformScheduler.runLaterFor(
                player,
                delay,
                Runnable {
                    pendingMenuOpens.remove(player.uniqueId)
                    openMenuInternal(player, menuId, placeholders, navigation)
                },
            )
        }
    }

    fun handleClick(player: Player, menuId: String, slot: Int, clickType: ClickType = ClickType.LEFT) {
        runForPlayer(player) {
            handleClickInternal(player, menuId, slot, clickType)
        }
    }

    fun executeActions(
        player: Player,
        menuId: String,
        actions: List<MenuAction>,
        placeholders: Map<String, String> = defaultPlaceholders(player),
    ) {
        runForPlayer(player) {
            executeActionChain(player, menuId, actions, 0, placeholders)
        }
    }

    private fun executeActionChain(
        player: Player,
        menuId: String,
        actions: List<MenuAction>,
        index: Int,
        placeholders: Map<String, String>,
    ) {
        if (index >= actions.size) {
            return
        }

        when (val action = actions[index]) {
            is MenuAction.Delay -> {
                platformScheduler.runLaterFor(
                    player,
                    action.ticks.coerceAtLeast(0L),
                    Runnable { executeActionChain(player, menuId, actions, index + 1, placeholders) },
                )
            }

            is MenuAction.Conditional -> {
                val branch = if (matchesConditions(player, placeholders, listOf(action.condition))) {
                    action.successActions
                } else {
                    action.denyActions
                }
                val remaining = if (index + 1 < actions.size) actions.subList(index + 1, actions.size) else emptyList()
                executeActionChain(player, menuId, branch + remaining, 0, placeholders)
            }

            else -> {
                executeAction(player, menuId, action, placeholders)
                executeActionChain(player, menuId, actions, index + 1, placeholders)
            }
        }
    }

    fun sendSystemMessage(
        receiver: CommandSender,
        key: String,
        placeholders: Map<String, String> = emptyMap(),
    ) {
        AdventureAccess.sendMessage(receiver, renderSystemText(key, placeholders))
    }

    fun renderSystemText(key: String, placeholders: Map<String, String> = emptyMap()): Component {
        return TextFormatter.component(settings.systemMessage(key, placeholders))
    }

    fun sendRawMessage(
        player: Player,
        text: String,
        placeholders: Map<String, String> = defaultPlaceholders(player),
    ) {
        AdventureAccess.sendMessage(player, renderComponent(player, text, placeholders))
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
        val state = stateFor(player, menu.id)
        state.placeholders.clear()
        state.placeholders.putAll(resolvedPlaceholders)
        val holder = MenuHolder(menu.id)
        val inventory = InventoryAccess.createInventory(
            holder,
            menu.size,
            renderComponent(player, menu.title, resolvedPlaceholders),
        )
        holder.bind(inventory)
        renderMenu(player, menu, inventory, state)

        closeCleanupSuppressed += player.uniqueId
        try {
            player.openInventory(inventory)
        } finally {
            closeCleanupSuppressed -= player.uniqueId
        }
        syncDynamicRefresh(player, menu)
    }

    private fun handleClickInternal(player: Player, menuId: String, slot: Int, clickType: ClickType) {
        val menu = repository.menu(menuId) ?: return
        val state = menuStates[player.uniqueId]?.get(menu.id)
        val placeholders = mergedPlaceholders(player, state) + clickPlaceholders(clickType)
        val button = menu.buttonAt(slot)?.let { resolveButton(player, it, placeholders) }
        if (button != null) {
            if (button.visiblePermission != null && !player.hasPermission(button.visiblePermission)) {
                return
            }
            if (button.permission != null && !player.hasPermission(button.permission)) {
                if (button.denyActions.isNotEmpty()) {
                    executeActions(player, menu.id, button.denyActions, placeholders)
                } else {
                    sendSystemMessage(player, "no-permission")
                }
                return
            }
            executeActions(player, menu.id, button.actions, placeholders)
            return
        }

        handlePageRegionClick(player, menu, slot, state, clickType)
    }

    private fun handlePageRegionClick(
        player: Player,
        menu: MenuDefinition,
        slot: Int,
        state: MenuViewState?,
        clickType: ClickType,
    ) {
        val activeState = state ?: return
        val region = menu.pageRegionAt(slot) ?: return
        val resolved = resolvePageEntry(player, activeState, menu, region, slot) ?: return
        executeActions(player, menu.id, resolved.entry.actions, resolved.placeholders + clickPlaceholders(clickType))
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
            is MenuAction.Delay -> Unit
            is MenuAction.Open -> openMenu(player, action.menuId, placeholders, NavigationMode.PUSH_CURRENT)
            is MenuAction.Prompt -> chatInputService.startPrompt(player, menuId, action.promptId, placeholders)
            is MenuAction.Page -> changePage(player, menuId, action)
            is MenuAction.PlayerCommand -> player.performCommand(placeholderPipeline.render(player, action.command, placeholders))
            is MenuAction.ConsoleCommand -> executeConsoleAction(player, action, placeholders)

            is MenuAction.Conditional -> Unit
            is MenuAction.TakePoint -> takePlayerPoints(player, action, placeholders)
            is MenuAction.Title -> showTitle(player, action, placeholders)
            is MenuAction.Message -> sendRawMessage(player, action.text, placeholders)
            is MenuAction.Sound -> playSound(player, action.spec)
        }
    }

    private fun takePlayerPoints(
        player: Player,
        action: MenuAction.TakePoint,
        placeholders: Map<String, String>,
    ) {
        val amountText = placeholderPipeline.render(player, action.amount, placeholders).trim()
        val amount = amountText.toDoubleOrNull()
        if (amount == null || !amount.isFinite() || amount <= 0.0 || amount > settings.maxTakePoint) {
            plugin.logger.warning(
                "Rejected unsafe take-point amount for ${player.name}: '$amountText' (max=${settings.maxTakePoint}).",
            )
            return
        }
        if (takePlayerPointsViaApi(player, amount)) {
            return
        }
        val commandAmount = if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
        val commands = listOf(
            "points take ${player.name} $commandAmount -s",
            "playerpoints take ${player.name} $commandAmount -s",
        )
        if (commands.any(::dispatchConsole)) {
            return
        }
        plugin.logger.warning(
            "Failed to execute PlayerPoints deduction for ${player.name} (amount=$amount). Tried commands: ${commands.joinToString()}",
        )
    }

    private fun executeConsoleAction(
        player: Player,
        action: MenuAction.ConsoleCommand,
        placeholders: Map<String, String>,
    ) {
        if (!settings.allowConsoleActions) {
            plugin.logger.warning("Blocked console action because security.allow-console-actions=false: ${action.command}")
            return
        }
        dispatchConsole(placeholderPipeline.render(player, action.command, placeholders))
    }

    private fun takePlayerPointsViaApi(player: Player, amount: Double): Boolean {
        val provider = plugin.server.pluginManager.getPlugin("PlayerPoints")
            ?.let { playerPoints -> runCatching { playerPoints.javaClass.methods.firstOrNull { it.name == "getAPI" }?.invoke(playerPoints) }.getOrNull() }
            ?: return false

        val method = provider.javaClass.methods.firstOrNull { method ->
            method.name == "take" &&
                method.parameterCount == 2 &&
                method.parameterTypes[0].isAssignableFrom(UUID::class.java)
        } ?: return false
        val numericArgument = when (method.parameterTypes[1]) {
            java.lang.Integer.TYPE, Integer::class.java -> amount.toInt()
            java.lang.Long.TYPE, java.lang.Long::class.java -> amount.toLong()
            java.lang.Double.TYPE, java.lang.Double::class.java -> amount
            java.lang.Float.TYPE, java.lang.Float::class.java -> amount.toFloat()
            else -> return false
        }
        return runCatching {
            method.invoke(provider, player.uniqueId, numericArgument) as? Boolean ?: true
        }.getOrDefault(false)
    }

    private fun dispatchConsole(command: String): Boolean {
        return plugin.server.dispatchCommand(plugin.server.consoleSender, command)
    }

    private fun playSound(player: Player, spec: SoundSpec) {
        val sound = runCatching { Sound.valueOf(spec.soundName) }.getOrNull() ?: return
        player.playSound(player.location, sound, spec.volume, spec.pitch)
    }

    private fun showTitle(
        player: Player,
        action: MenuAction.Title,
        placeholders: Map<String, String>,
    ) {
        player.showTitle(
            Title.title(
                renderComponent(player, action.title, placeholders),
                action.subtitle?.let { renderComponent(player, it, placeholders) } ?: Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(1800), Duration.ofMillis(300)),
            ),
        )
    }

    private fun changePage(player: Player, menuId: String, action: MenuAction.Page) {
        val menu = repository.menu(menuId) ?: return
        val state = stateFor(player, menu.id)
        val regionId = resolvePageRegionId(menu, action.regionId) ?: return
        val region = menu.pageRegions[regionId] ?: return
        val regionState = state.regions.getOrPut(regionId) { initialRegionState() }
        synchronizeRegionState(player, menu.id, state, region, regionState)
        val slotCount = menu.pageSlots(regionId).size.coerceAtLeast(1)
        val pageTotal = pageCount(regionState.entriesFor(region), slotCount)

        when (action.operation) {
            PageOperation.NEXT -> regionState.page = (regionState.page + 1).coerceAtMost(pageTotal - 1)
            PageOperation.PREVIOUS -> regionState.page = (regionState.page - 1).coerceAtLeast(0)
            PageOperation.FIRST -> regionState.page = 0
            PageOperation.LAST -> regionState.page = pageTotal - 1
            PageOperation.REFRESH -> {
                invalidateRegion(player, menu.id, region, dropLastGood = true)
                resetRegionState(state, regionState, region)
                regionState.page = 0
                synchronizeRegionState(player, menu.id, state, region, regionState)
            }
        }

        renderOpenMenuIfCurrent(player, menu.id)
    }

    private fun renderComponent(player: Player, text: String, placeholders: Map<String, String>): Component {
        return TextFormatter.component(placeholderPipeline.render(player, text, placeholders))
    }

    private fun openBack(player: Player, placeholders: Map<String, String>) {
        val stack = history[player.uniqueId]
        val previous = if (stack.isNullOrEmpty()) null else stack.removeLast()
        if (stack != null && stack.isEmpty()) {
            history.remove(player.uniqueId)
        }
        if (previous == null) {
            player.closeInventory()
            return
        }
        openMenuInternal(player, previous, placeholders, NavigationMode.NONE)
    }

    private fun currentMenuId(player: Player): String? {
        return (player.openInventory.topInventory?.holder as? MenuHolder)?.menuId
    }

    private fun clearHistory(player: Player) {
        dynamicRefreshController.cancelAllForPlayer(player.uniqueId)
        history.remove(player.uniqueId)
        menuStates.remove(player.uniqueId)
    }

    private fun cleanupRemovedMenus(player: Player, removedMenuIds: Set<String>) {
        if (removedMenuIds.isEmpty()) {
            return
        }

        history[player.uniqueId]?.let { stack ->
            stack.removeAll(removedMenuIds)
            if (stack.isEmpty()) {
                history.remove(player.uniqueId, stack)
            }
        }

        removedMenuIds.forEach { menuId ->
            dynamicRefreshController.cancelMenu(player.uniqueId, menuId)
        }
        val stateMap = menuStates[player.uniqueId]
        removedMenuIds.forEach { menuId -> stateMap?.remove(menuId) }
        if (stateMap != null && stateMap.isEmpty()) {
            menuStates.remove(player.uniqueId)
        }

        val current = currentMenuId(player) ?: return
        if (current in removedMenuIds) {
            player.closeInventory()
        }
    }

    private fun defaultPlaceholders(player: Player): Map<String, String> {
        return mapOf("player" to player.name)
    }

    private fun mergedPlaceholders(player: Player, state: MenuViewState?): Map<String, String> {
        val dynamic = state?.surfacePlaceholders
            ?.values
            ?.fold(emptyMap<String, String>()) { current, next -> current + next }
            ?: emptyMap()
        return defaultPlaceholders(player) + (state?.placeholders ?: emptyMap()) + dynamic
    }

    private fun clickPlaceholders(clickType: ClickType): Map<String, String> {
        val normalized = when (clickType) {
            ClickType.SHIFT_LEFT -> "shift-left"
            ClickType.SHIFT_RIGHT -> "shift-right"
            else -> when {
                clickType.isLeftClick -> "left"
                clickType.isRightClick -> "right"
                else -> clickType.name.lowercase().replace('_', '-')
            }
        }
        return mapOf("click-type" to normalized)
    }

    private fun resolveButton(
        player: Player,
        button: ButtonDefinition,
        placeholders: Map<String, String>,
    ): ResolvedButtonDefinition? {
        if (!matchesConditions(player, placeholders, button.conditions)) {
            return null
        }
        val state = button.states.firstOrNull { matchesConditions(player, placeholders, it.conditions) }
        return ResolvedButtonDefinition(
            icon = renderIcon(player, state?.icon ?: button.icon, placeholders),
            actions = state?.actions ?: button.actions,
            permission = state?.permission ?: button.permission,
            visiblePermission = state?.visiblePermission ?: button.visiblePermission,
            denyActions = state?.denyActions ?: button.denyActions,
        )
    }

    private fun matchesConditions(
        player: Player,
        placeholders: Map<String, String>,
        conditions: List<MenuCondition>,
    ): Boolean {
        return conditions.all { condition ->
            when (condition) {
                is MenuCondition.HasPermission -> player.hasPermission(condition.permission)
                is MenuCondition.MissingPermission -> !player.hasPermission(condition.permission)
                is MenuCondition.PlaceholderEquals ->
                    placeholderPipeline.matchesValue(player, condition.key, condition.value, placeholders)

                is MenuCondition.PlaceholderNotEquals ->
                    !placeholderPipeline.matchesValue(player, condition.key, condition.value, placeholders)

                is MenuCondition.Comparison ->
                    matchesComparisonCondition(player, condition, placeholders)
            }
        }
    }

    private fun matchesComparisonCondition(
        player: Player,
        condition: MenuCondition.Comparison,
        placeholders: Map<String, String>,
    ): Boolean {
        val left = placeholderPipeline.render(player, condition.left, placeholders)
        val right = placeholderPipeline.render(player, condition.right, placeholders)
        val leftNumber = left.toDoubleOrNull()
        val rightNumber = right.toDoubleOrNull()
        return if (leftNumber != null && rightNumber != null) {
            compareNumeric(leftNumber, rightNumber, condition.operator)
        } else {
            compareText(left, right, condition.operator)
        }
    }

    private fun compareNumeric(
        left: Double,
        right: Double,
        operator: ComparisonOperator,
    ): Boolean {
        return when (operator) {
            ComparisonOperator.GREATER_THAN -> left > right
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> left >= right
            ComparisonOperator.LESS_THAN -> left < right
            ComparisonOperator.LESS_THAN_OR_EQUAL -> left <= right
            ComparisonOperator.EQUAL -> left == right
            ComparisonOperator.NOT_EQUAL -> left != right
        }
    }

    private fun compareText(
        left: String,
        right: String,
        operator: ComparisonOperator,
    ): Boolean {
        val result = left.compareTo(right)
        return when (operator) {
            ComparisonOperator.GREATER_THAN -> result > 0
            ComparisonOperator.GREATER_THAN_OR_EQUAL -> result >= 0
            ComparisonOperator.LESS_THAN -> result < 0
            ComparisonOperator.LESS_THAN_OR_EQUAL -> result <= 0
            ComparisonOperator.EQUAL -> left == right
            ComparisonOperator.NOT_EQUAL -> left != right
        }
    }

    private fun renderMenu(
        player: Player,
        menu: MenuDefinition,
        inventory: Inventory,
        state: MenuViewState,
    ) {
        prepareDynamicSurfaces(player, menu, state)
        for (slot in 0 until menu.size) {
            inventory.setItem(slot, resolveItem(player, menu, slot, state))
        }
    }

    private fun resolveItem(
        player: Player,
        menu: MenuDefinition,
        slot: Int,
        state: MenuViewState,
    ): ItemStack? {
        val placeholders = mergedPlaceholders(player, state)
        val button = menu.buttonAt(slot)?.let { resolveButton(player, it, placeholders) }
        if (button != null) {
            if (button.visiblePermission != null && !player.hasPermission(button.visiblePermission)) {
                return null
            }
            return ItemFactory.create(button.icon, emptyMap())
        }

        val region = menu.pageRegionAt(slot) ?: return null
        val regionState = state.regions.getOrPut(region.id) { initialRegionState() }
        synchronizeRegionState(player, menu.id, state, region, regionState)
        val resolved = resolvePageEntry(player, state, menu, region, slot)
        return when {
            resolved != null -> ItemFactory.create(renderIcon(player, resolved.entry.icon, resolved.placeholders), emptyMap())
            regionState.status == RegionLoadStatus.LOADING || regionState.status == RegionLoadStatus.NOT_LOADED ->
                ItemFactory.create(renderIcon(player, region.loadingIcon, mergedPlaceholders(player, state)), emptyMap())
            regionState.status == RegionLoadStatus.ERROR && region.errorIcon != null ->
                ItemFactory.create(renderIcon(player, region.errorIcon, mergedPlaceholders(player, state)), emptyMap())
            else -> ItemFactory.create(renderIcon(player, region.emptyIcon, mergedPlaceholders(player, state)), emptyMap())
        }
    }

    private fun resolvePageEntry(
        player: Player,
        state: MenuViewState,
        menu: MenuDefinition,
        region: PageRegionDefinition,
        slot: Int,
    ): ResolvedPageEntry? {
        val regionState = state.regions.getOrPut(region.id) { initialRegionState() }
        synchronizeRegionState(player, menu.id, state, region, regionState)
        if (regionState.status != RegionLoadStatus.READY) {
            return null
        }

        val slots = menu.pageSlots(region.id)
        if (slots.isEmpty()) {
            return null
        }

        val entries = regionState.entriesFor(region)
        if (entries.isEmpty()) {
            return null
        }

        val slotIndex = slots.indexOf(slot)
        if (slotIndex < 0) {
            return null
        }

        val pageSize = slots.size.coerceAtLeast(1)
        val pageTotal = pageCount(entries, pageSize)
        regionState.page = regionState.page.coerceIn(0, pageTotal - 1)
        val startIndex = regionState.page * pageSize
        val globalIndex = startIndex + slotIndex
        if (globalIndex >= entries.size) {
            return null
        }

        val entry = entries[globalIndex]
        val placeholders = mergedPlaceholdersForEntry(
            player = player,
            base = mergedPlaceholders(player, state),
            region = region,
            entry = entry,
            globalIndex = globalIndex,
            page = regionState.page,
            pageSize = pageSize,
            pageTotal = pageTotal,
            pageSlot = slotIndex,
        )
        return ResolvedPageEntry(entry, placeholders)
    }

    private fun mergedPlaceholdersForEntry(
        player: Player,
        base: Map<String, String>,
        region: PageRegionDefinition,
        entry: PageEntryDefinition,
        globalIndex: Int,
        page: Int,
        pageSize: Int,
        pageTotal: Int,
        pageSlot: Int,
    ): Map<String, String> {
        val entryId = entry.id.ifBlank { (globalIndex + 1).toString() }
        return base +
            entry.placeholders.mapValues { (_, value) -> placeholderPipeline.render(player, value, base) } +
            mapOf(
                "region" to region.id,
                "entry-id" to entryId,
                "entry-index" to (globalIndex + 1).toString(),
                "page" to (page + 1).toString(),
                "page-total" to pageTotal.toString(),
                "page-size" to pageSize.toString(),
                "page-slot" to (pageSlot + 1).toString(),
            )
    }

    private fun renderOpenMenuIfCurrent(player: Player, menuId: String) {
        val holder = player.openInventory.topInventory.holder as? MenuHolder ?: return
        if (holder.menuId != menuId) {
            return
        }
        val menu = repository.menu(menuId) ?: return
        val state = stateFor(player, menu.id)
        renderMenu(player, menu, player.openInventory.topInventory, state)
        syncDynamicRefresh(player, menu)
        player.updateInventory()
    }

    private fun syncDynamicRefresh(player: Player, menu: MenuDefinition) {
        dynamicRefreshController.sync(player, menu, onRefreshSurface = { surfaceId ->
            refreshSurface(player.uniqueId, menu.id, surfaceId)
        }, onRefreshButton = { symbol ->
            refreshButton(player.uniqueId, menu.id, symbol)
        })
    }

    private fun refreshSurface(
        playerId: UUID,
        menuId: String,
        surfaceId: String,
    ) {
        val player = plugin.server.getPlayer(playerId) ?: return
        runForPlayer(player) {
            val menu = repository.menu(menuId) ?: return@runForPlayer
            val region = menu.pageRegions[surfaceId] ?: return@runForPlayer
            invalidateRegion(player, menuId, region)
            renderOpenMenuIfCurrent(player, menuId)
        }
    }

    private fun refreshButton(
        playerId: UUID,
        menuId: String,
        symbol: Char,
    ) {
        val player = plugin.server.getPlayer(playerId) ?: return
        runForPlayer(player) {
            val holder = player.openInventory.topInventory.holder as? MenuHolder ?: return@runForPlayer
            if (holder.menuId != menuId) {
                return@runForPlayer
            }
            val menu = repository.menu(menuId) ?: return@runForPlayer
            val state = stateFor(player, menu.id)
            menu.slotsFor(symbol).forEach { slot ->
                player.openInventory.topInventory.setItem(slot, resolveItem(player, menu, slot, state))
            }
            player.updateInventory()
        }
    }

    private fun resolvePageRegionId(menu: MenuDefinition, rawRegionId: String?): String? {
        if (!rawRegionId.isNullOrBlank()) {
            return rawRegionId.takeIf(menu.pageRegions::containsKey)
        }
        return menu.pageRegions.keys.singleOrNull()
    }

    private fun stateFor(player: Player, menuId: String): MenuViewState {
        return menuStates
            .computeIfAbsent(player.uniqueId) { mutableMapOf() }
            .getOrPut(menuId) { MenuViewState(player.uniqueId, menuId) }
    }

    private fun initialRegionState(): RegionViewState {
        return RegionViewState()
    }

    private fun resetRegionState(
        menuState: MenuViewState,
        state: RegionViewState,
        region: PageRegionDefinition,
    ) {
        menuState.surfacePlaceholders.remove(region.id)
        state.entries = emptyList()
        state.status = RegionLoadStatus.NOT_LOADED
    }

    private fun pageCount(entries: List<PageEntryDefinition>, pageSize: Int): Int {
        if (entries.isEmpty()) {
            return 1
        }
        return max(1, ceil(entries.size.toDouble() / pageSize.coerceAtLeast(1).toDouble()).toInt())
    }

    private fun cancelPendingMenuOpen(playerId: UUID) {
        pendingMenuOpens.remove(playerId)?.cancel()
    }

    private inline fun runForPlayer(player: Player, crossinline action: () -> Unit) {
        if (platformScheduler.isPlayerThread(player)) {
            action()
            return
        }

        platformScheduler.executeFor(player, Runnable { action() })
    }

    private fun renderIcon(
        player: Player,
        icon: IconDefinition,
        placeholders: Map<String, String>,
    ): IconDefinition {
        return icon.copy(
            name = icon.name?.let { placeholderPipeline.render(player, it, placeholders) },
            lore = placeholderPipeline.renderAll(player, icon.lore, placeholders),
        )
    }

    private fun prepareDynamicSurfaces(
        player: Player,
        menu: MenuDefinition,
        state: MenuViewState,
    ) {
        menu.pageRegions.values.forEach { region ->
            val regionState = state.regions.getOrPut(region.id) { initialRegionState() }
            synchronizeRegionState(player, menu.id, state, region, regionState)
        }
    }

    private fun synchronizeRegionState(
        player: Player,
        menuId: String,
        state: MenuViewState,
        region: PageRegionDefinition,
        regionState: RegionViewState,
    ) {
        val providerType = providerTypeFor(region) ?: run {
            regionState.entries = region.entries
            regionState.status = if (region.entries.isEmpty()) RegionLoadStatus.EMPTY else RegionLoadStatus.READY
            return
        }
        val key = providerKey(player.uniqueId, menuId, region, providerType)
        var snapshot = providerCache.read(key)
        val needsReload = snapshot.current == null || snapshot.expired
        if (needsReload && !snapshot.loading) {
            triggerProviderLoad(player, menuId, region, providerType, key, mergedPlaceholders(player, state))
            snapshot = providerCache.read(key)
        }
        applyRegionSnapshot(state, region, regionState, snapshot)
    }

    private fun triggerProviderLoad(
        player: Player,
        menuId: String,
        region: PageRegionDefinition,
        providerType: String,
        key: ProviderCache.Key,
        placeholders: Map<String, String>,
    ) {
        val provider = providerRegistry.provider(providerType) ?: run {
            providerCache.complete(
                key = key,
                requestToken = providerCache.beginLoad(key),
                result = ProviderResult.Error("Unknown provider $providerType"),
                ttlTicks = null,
            )
            return
        }
        val requestToken = providerCache.beginLoad(key)
        val request = ProviderRequest(
            viewerId = player.uniqueId,
            viewerName = player.name,
            menuId = menuId,
            surfaceId = region.id,
            providerType = providerType,
            resolvedParams = region.provider?.params
                ?.mapValues { (_, value) -> placeholderPipeline.render(player, value, placeholders) }
                ?: emptyMap(),
            placeholders = placeholders,
            region = region,
        )
        provider.load(request).whenComplete { result, throwable ->
            val resolved = when {
                throwable != null -> ProviderResult.Error(throwable.message)
                result != null -> result
                else -> ProviderResult.Empty
            }
            providerCache.complete(
                key = key,
                requestToken = requestToken,
                result = resolved,
                ttlTicks = ttlFor(region, resolved),
            )
            val livePlayer = plugin.server.getPlayer(player.uniqueId) ?: return@whenComplete
            platformScheduler.executeFor(livePlayer, Runnable {
                val menu = repository.menu(menuId) ?: return@Runnable
                val activeState = stateFor(livePlayer, menu.id)
                val activeRegion = menu.pageRegions[region.id] ?: return@Runnable
                val activeRegionState = activeState.regions.getOrPut(activeRegion.id) { initialRegionState() }
                synchronizeRegionState(livePlayer, menu.id, activeState, activeRegion, activeRegionState)
                renderOpenMenuIfCurrent(livePlayer, menu.id)
            })
        }
    }

    private fun applyRegionSnapshot(
        state: MenuViewState,
        region: PageRegionDefinition,
        regionState: RegionViewState,
        snapshot: ProviderCache.Snapshot,
    ) {
        val visibleSuccess = when {
            snapshot.current is ProviderResult.Success && !snapshot.expired -> snapshot.current
            snapshot.lastGood != null -> snapshot.lastGood
            else -> null
        }

        if (visibleSuccess != null) {
            regionState.entries = visibleSuccess.entries
            state.surfacePlaceholders[region.id] = visibleSuccess.placeholders
            regionState.status = RegionLoadStatus.READY
            return
        }

        state.surfacePlaceholders.remove(region.id)
        regionState.entries = emptyList()
        regionState.status = when {
            snapshot.loading -> RegionLoadStatus.LOADING
            snapshot.current is ProviderResult.Empty -> RegionLoadStatus.EMPTY
            snapshot.current is ProviderResult.Error -> RegionLoadStatus.ERROR
            else -> RegionLoadStatus.NOT_LOADED
        }
    }

    private fun providerTypeFor(region: PageRegionDefinition): String? {
        return region.provider?.type
            ?: if (region.entries.isNotEmpty() || region.asyncDelayTicks > 0L) "entries" else null
    }

    private fun providerKey(
        viewerId: UUID,
        menuId: String,
        region: PageRegionDefinition,
        providerType: String,
    ): ProviderCache.Key {
        return ProviderCache.Key(
            viewerId = viewerId,
            menuId = menuId,
            surfaceId = region.id,
            providerType = providerType,
        )
    }

    private fun invalidateRegion(
        player: Player,
        menuId: String,
        region: PageRegionDefinition,
        dropLastGood: Boolean = false,
    ) {
        val providerType = providerTypeFor(region) ?: return
        providerCache.invalidate(
            key = providerKey(player.uniqueId, menuId, region, providerType),
            dropLastGood = dropLastGood,
        )
    }

    private fun ttlFor(
        region: PageRegionDefinition,
        result: ProviderResult,
    ): Long? {
        return (result as? ProviderResult.Success)?.ttlTicks ?: region.provider?.cache?.ttl
    }
}

enum class NavigationMode {
    ROOT,
    PUSH_CURRENT,
    NONE,
}

private data class MenuViewState(
    val ownerId: UUID,
    val menuId: String,
    val placeholders: MutableMap<String, String> = linkedMapOf(),
    val surfacePlaceholders: MutableMap<String, Map<String, String>> = linkedMapOf(),
    val regions: MutableMap<String, RegionViewState> = linkedMapOf(),
)

private data class RegionViewState(
    var page: Int = 0,
    var status: RegionLoadStatus = RegionLoadStatus.NOT_LOADED,
    var entries: List<PageEntryDefinition> = emptyList(),
) {
    fun entriesFor(region: PageRegionDefinition): List<PageEntryDefinition> {
        return if (entries.isEmpty() && status == RegionLoadStatus.READY) region.entries else entries
    }
}

private enum class RegionLoadStatus {
    NOT_LOADED,
    LOADING,
    READY,
    EMPTY,
    ERROR,
}

private data class ResolvedPageEntry(
    val entry: PageEntryDefinition,
    val placeholders: Map<String, String>,
)

private data class ResolvedButtonDefinition(
    val icon: IconDefinition,
    val actions: List<MenuAction>,
    val permission: String?,
    val visiblePermission: String?,
    val denyActions: List<MenuAction>,
)
