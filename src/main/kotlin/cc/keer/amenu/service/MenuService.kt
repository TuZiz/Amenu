package cc.keer.amenu.service

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.PluginSettings
import cc.keer.amenu.config.ButtonDefinition
import cc.keer.amenu.config.IconDefinition
import cc.keer.amenu.config.MenuCondition
import cc.keer.amenu.config.MenuAction
import cc.keer.amenu.config.MenuDefinition
import cc.keer.amenu.config.PageEntryDefinition
import cc.keer.amenu.config.PageOperation
import cc.keer.amenu.config.PageRegionDefinition
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.config.SoundSpec
import cc.keer.amenu.gui.MenuHolder
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.InventoryAccess
import cc.keer.amenu.util.ItemFactory
import cc.keer.amenu.util.TextFormatter
import net.kyori.adventure.text.Component
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

class MenuService(
    private val plugin: AMenuPlugin,
    private var settings: PluginSettings,
    val repository: MenuRepository,
    private val platformScheduler: PlatformScheduler,
    val placeholderPipeline: PlaceholderPipeline = PlaceholderPipeline(BukkitPlaceholderApiBridge(plugin)),
) {

    private lateinit var chatInputService: ChatInputService
    private val history = mutableMapOf<UUID, ArrayDeque<String>>()
    private val menuStates = mutableMapOf<UUID, MutableMap<String, MenuViewState>>()

    fun attachChatInputService(service: ChatInputService) {
        chatInputService = service
    }

    fun reload(newSettings: PluginSettings) {
        settings = newSettings
        menuStates.clear()
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

        player.openInventory(inventory)
    }

    private fun handleClickInternal(player: Player, menuId: String, slot: Int) {
        val menu = repository.menu(menuId) ?: return
        val state = menuStates[player.uniqueId]?.get(menu.id)
        val placeholders = mergedPlaceholders(player, state)
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

        handlePageRegionClick(player, menu, slot, state)
    }

    private fun handlePageRegionClick(player: Player, menu: MenuDefinition, slot: Int, state: MenuViewState?) {
        val activeState = state ?: return
        val region = menu.pageRegionAt(slot) ?: return
        val resolved = resolvePageEntry(player, activeState, menu, region, slot) ?: return
        executeActions(player, menu.id, resolved.entry.actions, resolved.placeholders)
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
            is MenuAction.ConsoleCommand -> plugin.server.dispatchCommand(
                plugin.server.consoleSender,
                placeholderPipeline.render(player, action.command, placeholders),
            )

            is MenuAction.Message -> sendRawMessage(player, action.text, placeholders)
            is MenuAction.Sound -> playSound(player, action.spec)
        }
    }

    private fun playSound(player: Player, spec: SoundSpec) {
        val sound = runCatching { Sound.valueOf(spec.soundName) }.getOrNull() ?: return
        player.playSound(player.location, sound, spec.volume, spec.pitch)
    }

    private fun changePage(player: Player, menuId: String, action: MenuAction.Page) {
        val menu = repository.menu(menuId) ?: return
        val state = stateFor(player, menu.id)
        val regionId = resolvePageRegionId(menu, action.regionId) ?: return
        val region = menu.pageRegions[regionId] ?: return
        val regionState = state.regions.getOrPut(regionId) { initialRegionState(region) }
        val slotCount = menu.pageSlots(regionId).size.coerceAtLeast(1)
        val pageTotal = pageCount(regionState.entriesFor(region), slotCount)

        when (action.operation) {
            PageOperation.NEXT -> regionState.page = (regionState.page + 1).coerceAtMost(pageTotal - 1)
            PageOperation.PREVIOUS -> regionState.page = (regionState.page - 1).coerceAtLeast(0)
            PageOperation.FIRST -> regionState.page = 0
            PageOperation.LAST -> regionState.page = pageTotal - 1
            PageOperation.REFRESH -> {
                resetRegionState(regionState, region)
                regionState.page = 0
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
        openMenuInternal(player, previous ?: settings.defaultMenuId, placeholders, NavigationMode.NONE)
    }

    private fun currentMenuId(player: Player): String? {
        return (player.openInventory.topInventory.holder as? MenuHolder)?.menuId
    }

    private fun clearHistory(player: Player) {
        history.remove(player.uniqueId)
        menuStates.remove(player.uniqueId)
    }

    private fun defaultPlaceholders(player: Player): Map<String, String> {
        return mapOf("player" to player.name)
    }

    private fun mergedPlaceholders(player: Player, state: MenuViewState?): Map<String, String> {
        return defaultPlaceholders(player) + (state?.placeholders ?: emptyMap())
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
            }
        }
    }

    private fun renderMenu(
        player: Player,
        menu: MenuDefinition,
        inventory: Inventory,
        state: MenuViewState,
    ) {
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
        val resolved = resolvePageEntry(player, state, menu, region, slot)
        return when {
            resolved != null -> ItemFactory.create(renderIcon(player, resolved.entry.icon, resolved.placeholders), emptyMap())
            state.regions.getOrPut(region.id) { initialRegionState(region) }.status != RegionLoadStatus.READY ->
                ItemFactory.create(renderIcon(player, region.loadingIcon, mergedPlaceholders(player, state)), emptyMap())

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
        val regionState = state.regions.getOrPut(region.id) { initialRegionState(region) }
        if (regionState.status == RegionLoadStatus.NOT_LOADED) {
            triggerAsyncLoad(state.ownerId, menu.id, region, regionState)
        }
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
            base = state.placeholders,
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

    private fun triggerAsyncLoad(
        ownerId: UUID,
        menuId: String,
        region: PageRegionDefinition,
        regionState: RegionViewState,
    ) {
        if (region.asyncDelayTicks <= 0L) {
            regionState.status = RegionLoadStatus.READY
            regionState.entries = region.entries
            return
        }
        if (regionState.status == RegionLoadStatus.LOADING) {
            return
        }
        regionState.status = RegionLoadStatus.LOADING
        val requestId = regionState.nextRequest()
        regionState.pendingTask?.cancel()
        val player = plugin.server.getPlayer(ownerId) ?: return
        regionState.pendingTask = platformScheduler.runLaterFor(player, region.asyncDelayTicks, Runnable {
            if (regionState.requestId != requestId) {
                return@Runnable
            }
            regionState.pendingTask = null
            regionState.entries = region.entries
            regionState.status = RegionLoadStatus.READY
            renderMenuForActiveView(ownerId, menuId)
        })
    }

    private fun renderMenuForActiveView(playerId: UUID, menuId: String) {
        val player = plugin.server.getPlayer(playerId) ?: return
        renderOpenMenuIfCurrent(player, menuId)
    }

    private fun renderOpenMenuIfCurrent(player: Player, menuId: String) {
        val holder = player.openInventory.topInventory.holder as? MenuHolder ?: return
        if (holder.menuId != menuId) {
            return
        }
        val menu = repository.menu(menuId) ?: return
        val state = stateFor(player, menu.id)
        renderMenu(player, menu, player.openInventory.topInventory, state)
        player.updateInventory()
    }

    private fun resolvePageRegionId(menu: MenuDefinition, rawRegionId: String?): String? {
        if (!rawRegionId.isNullOrBlank()) {
            return rawRegionId.takeIf(menu.pageRegions::containsKey)
        }
        return menu.pageRegions.keys.singleOrNull()
    }

    private fun stateFor(player: Player, menuId: String): MenuViewState {
        return menuStates
            .getOrPut(player.uniqueId) { mutableMapOf() }
            .getOrPut(menuId) { MenuViewState(player.uniqueId, menuId) }
    }

    private fun initialRegionState(region: PageRegionDefinition): RegionViewState {
        return if (region.asyncDelayTicks > 0L) {
            RegionViewState()
        } else {
            RegionViewState(status = RegionLoadStatus.READY, entries = region.entries.toList())
        }
    }

    private fun resetRegionState(state: RegionViewState, region: PageRegionDefinition) {
        state.pendingTask?.cancel()
        state.pendingTask = null
        state.entries = if (region.asyncDelayTicks > 0L) emptyList() else region.entries.toList()
        state.status = if (region.asyncDelayTicks > 0L) RegionLoadStatus.NOT_LOADED else RegionLoadStatus.READY
    }

    private fun pageCount(entries: List<PageEntryDefinition>, pageSize: Int): Int {
        if (entries.isEmpty()) {
            return 1
        }
        return max(1, ceil(entries.size.toDouble() / pageSize.coerceAtLeast(1).toDouble()).toInt())
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
    val regions: MutableMap<String, RegionViewState> = linkedMapOf(),
)

private data class RegionViewState(
    var page: Int = 0,
    var status: RegionLoadStatus = RegionLoadStatus.NOT_LOADED,
    var entries: List<PageEntryDefinition> = emptyList(),
    var requestId: Int = 0,
    var pendingTask: cc.keer.amenu.platform.TaskHandle? = null,
) {
    fun nextRequest(): Int {
        requestId += 1
        return requestId
    }

    fun entriesFor(region: PageRegionDefinition): List<PageEntryDefinition> {
        return if (entries.isEmpty() && status == RegionLoadStatus.READY) region.entries else entries
    }
}

private enum class RegionLoadStatus {
    NOT_LOADED,
    LOADING,
    READY,
}

private data class ResolvedPageEntry(
    val entry: PageEntryDefinition,
    val placeholders: Map<String, String>,
)

private data class ResolvedButtonDefinition(
    val icon: cc.keer.amenu.config.IconDefinition,
    val actions: List<MenuAction>,
    val permission: String?,
    val visiblePermission: String?,
    val denyActions: List<MenuAction>,
)
