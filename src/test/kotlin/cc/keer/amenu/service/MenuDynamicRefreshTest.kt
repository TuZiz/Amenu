package cc.keer.amenu.service

import cc.keer.amenu.config.IconDefinition
import cc.keer.amenu.config.PageEntryDefinition
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.TaskHandle
import cc.keer.amenu.service.provider.MenuDataProvider
import cc.keer.amenu.service.provider.MenuProviderRegistry
import cc.keer.amenu.service.provider.ProviderCache
import cc.keer.amenu.service.provider.ProviderRequest
import cc.keer.amenu.service.provider.ProviderResult
import cc.keer.amenu.support.MenuPluginTestHarness
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class MenuDynamicRefreshTest : MenuPluginTestHarness() {

    private val plainText = PlainTextComponentSerializer.plainText()
    private val ticks = AtomicLong(0L)

    private lateinit var trackingScheduler: TrackingPlatformScheduler
    private lateinit var registry: MenuProviderRegistry
    private lateinit var cache: ProviderCache

    @BeforeEach
    fun setUpDynamicRefreshRuntime() {
        trackingScheduler = TrackingPlatformScheduler(plugin.platformScheduler)
        registry = MenuProviderRegistry()
        cache = ProviderCache(tickProvider = ticks::get)
    }

    @Test
    fun button_state_rechecks_without_menu_reopen_and_through_placeholder_pipeline() {
        writeMenu(
            "timed-gift",
            """
            title: "Timed Gift"
            layout:
              - "#########"
              - "#GX######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "G":
                material: BARRIER
                name: "Gift loading"
                lore:
                  - "Viewer %player_name%"
                states:
                  available:
                    conditions:
                      placeholder-equals:
                        "{reward-state}": "available"
                    material: EMERALD
                    name: "Gift for {viewer-name}"
                    lore:
                      - "Viewer %player_name%"
                  claimed:
                    conditions:
                      placeholder-equals:
                        "{reward-state}": "claimed"
                    material: CHEST
                    name: "Claimed by {viewer-name}"
                    lore:
                      - "Viewer %player_name%"
              "X":
                material: FEATHER
                name: "Refresh now"
                click:
                  - "page: refresh ticker"
            pages:
              ticker:
                symbol: "T"
                provider:
                  type: rotating-state
                update:
                  interval: 20
                loading:
                  material: CLOCK
                  name: "Refreshing state"
                entries:
                  driver:
                    material: PAPER
                    name: "Driver"
            """.trimIndent(),
        )

        val states = CopyOnWriteArrayList(
            listOf(
                mapOf("viewer-name" to "%player_name%", "reward-state" to "available"),
                mapOf("viewer-name" to "%player_name%", "reward-state" to "claimed"),
            ),
        )
        registry.register(
            "rotating-state",
            RecordingProvider { _ ->
                CompletableFuture.completedFuture(
                    ProviderResult.Success(placeholders = states.removeAt(0)),
                )
            },
        )
        val service = createService(
            registry = registry,
            cache = cache,
            placeholderPipeline = PlaceholderPipeline(FakePlaceholderApiBridge(enabled = true)),
        )

        service.openMenu(player, "timed-gift", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        val sameInventory = player.openInventory.topInventory
        val refreshHandle = trackingScheduler.repeatingHandles.last()
        assertEquals(Material.EMERALD, currentItem('G')!!.type)
        assertEquals("Gift for Tester", plainName(currentItem('G')))
        assertEquals(listOf("Viewer Tester"), plainLore(currentItem('G')))

        refreshHandle.runOnce()
        advanceTicks(1L)

        assertSame(sameInventory, player.openInventory.topInventory)
        assertEquals(Material.CHEST, currentItem('G')!!.type)
        assertEquals("Claimed by Tester", plainName(currentItem('G')))
        assertEquals(listOf("Viewer Tester"), plainLore(currentItem('G')))
    }

    @Test
    fun button_update_repaints_matching_slots_without_menu_reopen() {
        writeMenu(
            "button-countdown",
            """
            title: "Button Countdown"
            layout:
              - "#########"
              - "#DD######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "D":
                update: 4
                material: CLOCK
                name: "Egg %countdown%"
                lore:
                  - "Refresh %countdown%"
            """.trimIndent(),
        )
        ticks.set(10L)
        val service = createService(
            registry = registry,
            cache = cache,
            placeholderPipeline = PlaceholderPipeline(TickPlaceholderApiBridge(ticks::get)),
        )

        service.openMenu(player, "button-countdown", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        val sameInventory = player.openInventory.topInventory
        val refreshHandle = trackingScheduler.repeatingHandles.last()
        assertEquals(Material.CLOCK, currentItem('D')!!.type)
        assertEquals("Egg 10", plainName(currentItem('D', occurrence = 0)))
        assertEquals("Egg 10", plainName(currentItem('D', occurrence = 1)))

        ticks.set(9L)
        refreshHandle.runOnce()

        assertSame(sameInventory, player.openInventory.topInventory)
        assertEquals("Egg 9", plainName(currentItem('D', occurrence = 0)))
        assertEquals(listOf("Refresh 9"), plainLore(currentItem('D', occurrence = 1)))
    }

    @Test
    fun refresh_handles_are_cancelled_on_close_reload_quit_and_menu_replace() {
        writeTimedMenu("refresh-one")
        writeTimedMenu("refresh-two")

        registry.register("placeholder-state", RecordingProvider { request ->
            CompletableFuture.completedFuture(ProviderResult.Success(placeholders = request.resolvedParams))
        })
        val service = createService(registry = registry, cache = cache)

        service.openMenu(player, "refresh-one", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        val closeHandle = trackingScheduler.repeatingHandles.last()
        assertFalse(closeHandle.cancelled)

        service.handleInventoryClosed(player, "refresh-one")
        assertTrue(closeHandle.cancelled)

        service.openMenu(player, "refresh-one", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        val reloadHandle = trackingScheduler.repeatingHandles.last()
        service.reload(plugin.settings)
        assertTrue(reloadHandle.cancelled)

        service.openMenu(player, "refresh-one", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        val quitHandle = trackingScheduler.repeatingHandles.last()
        service.handlePlayerQuit(player)
        assertTrue(quitHandle.cancelled)

        service.openMenu(player, "refresh-one", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        val replaceHandle = trackingScheduler.repeatingHandles.last()
        service.openMenu(player, "refresh-two", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        assertTrue(replaceHandle.cancelled)
        assertEquals(1, trackingScheduler.repeatingHandles.count { !it.cancelled })
    }

    @Test
    fun provider_error_and_empty_states_render_explicit_fallback_items() {
        writeMenu(
            "surface-fallbacks",
            """
            title: "Surface Fallbacks"
            layout:
              - "#########"
              - "#EM######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            pages:
              error-surface:
                symbol: "E"
                provider:
                  type: missing-provider
                error:
                  material: RED_STAINED_GLASS_PANE
                  name: "Provider error"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback"
              empty-surface:
                symbol: "M"
                provider:
                  type: empty-provider
                empty:
                  material: BARRIER
                  name: "No rewards"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback"
            """.trimIndent(),
        )

        registry.register(
            "empty-provider",
            RecordingProvider { CompletableFuture.completedFuture(ProviderResult.Empty) },
        )
        val service = createService(registry = registry, cache = cache)

        service.openMenu(player, "surface-fallbacks", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        assertEquals(Material.RED_STAINED_GLASS_PANE, currentItem('E')!!.type)
        assertEquals("Provider error", plainName(currentItem('E')))
        assertEquals(Material.BARRIER, currentItem('M')!!.type)
        assertEquals("No rewards", plainName(currentItem('M')))
    }

    @Test
    fun ttl_expiry_reload_uses_loading_or_stale_last_good_before_safe_rerender() {
        writeMenu(
            "ttl-surface",
            """
            title: "TTL Surface"
            layout:
              - "#########"
              - "#R#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            pages:
              rewards:
                symbol: "R"
                provider:
                  type: expiring-provider
                  cache:
                    ttl: 5
                loading:
                  material: CLOCK
                  name: "Loading rewards"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback"
            """.trimIndent(),
        )

        val nextReload = CompletableFuture<ProviderResult>()
        val loads = AtomicLong(0L)
        registry.register(
            "expiring-provider",
            RecordingProvider {
                if (loads.incrementAndGet() == 1L) {
                    CompletableFuture.completedFuture(
                        ProviderResult.Success(entries = listOf(entry("first", "First reward", Material.EMERALD))),
                    )
                } else {
                    nextReload
                }
            },
        )
        val service = createService(registry = registry, cache = cache)

        service.openMenu(player, "ttl-surface", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        assertEquals("First reward", plainName(currentItem('R')))

        ticks.set(6L)
        service.openMenu(player, "ttl-surface", navigation = NavigationMode.NONE)

        val beforeReload = currentItem('R')!!
        assertTrue(beforeReload.type == Material.CLOCK || plainName(beforeReload) == "First reward")

        nextReload.complete(
            ProviderResult.Success(entries = listOf(entry("second", "Second reward", Material.DIAMOND))),
        )
        advanceTicks(1L)

        assertEquals("Second reward", plainName(currentItem('R')))
    }

    private fun createService(
        registry: MenuProviderRegistry,
        cache: ProviderCache,
        placeholderPipeline: PlaceholderPipeline = PlaceholderPipeline(FakePlaceholderApiBridge(enabled = false)),
    ): MenuService {
        return MenuService(
            plugin = plugin,
            settings = plugin.settings,
            repository = plugin.menuRepository,
            platformScheduler = trackingScheduler,
            placeholderPipeline = placeholderPipeline,
            providerRegistry = registry,
            providerCache = cache,
        )
    }

    private fun writeTimedMenu(menuId: String) {
        writeMenu(
            menuId,
            """
            title: "$menuId"
            layout:
              - "#########"
              - "#S#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "S":
                material: PAPER
                name: "State"
            pages:
              ticker:
                symbol: "T"
                provider:
                  type: placeholder-state
                  params:
                    viewer-name: "%player_name%"
                update:
                  interval: 20
                loading:
                  material: CLOCK
                  name: "Refreshing"
                entries:
                  driver:
                    material: PAPER
                    name: "Driver"
            """.trimIndent(),
        )
    }

    private fun plainName(item: ItemStack?): String? {
        val meta = item?.itemMeta ?: return null
        val componentGetter = meta.javaClass.methods.firstOrNull { it.name == "displayName" && it.parameterCount == 0 }
        val componentName = componentGetter?.invoke(meta) as? Component
        return componentName?.let(plainText::serialize)
    }

    private fun plainLore(item: ItemStack?): List<String> {
        val meta = item?.itemMeta ?: return emptyList()
        val loreGetter = meta.javaClass.methods.firstOrNull { it.name == "lore" && it.parameterCount == 0 }
        val lore = loreGetter?.invoke(meta) as? List<*>
        return lore.orEmpty().mapNotNull { line -> (line as? Component)?.let(plainText::serialize) }
    }

    private fun entry(
        id: String,
        name: String,
        material: Material = Material.PAPER,
    ): PageEntryDefinition {
        return PageEntryDefinition(
            id = id,
            icon = IconDefinition(
                materialName = material.name,
                texture = null,
                name = name,
                lore = emptyList(),
                amount = 1,
                glow = false,
            ),
            actions = emptyList(),
            placeholders = emptyMap(),
        )
    }

    private class RecordingProvider(
        private val loader: (ProviderRequest) -> CompletionStage<ProviderResult>,
    ) : MenuDataProvider {
        override fun load(request: ProviderRequest): CompletionStage<ProviderResult> {
            return loader(request)
        }
    }

    private class FakePlaceholderApiBridge(
        private val enabled: Boolean,
    ) : PlaceholderApiBridge {
        override fun isAvailable(): Boolean = enabled

        override fun render(player: Player, text: String): String {
            if (!enabled) {
                return text
            }
            return text.replace("%player_name%", player.name)
        }
    }

    private class TickPlaceholderApiBridge(
        private val tickProvider: () -> Long,
    ) : PlaceholderApiBridge {
        override fun isAvailable(): Boolean = true

        override fun render(player: Player, text: String): String {
            return text.replace("%countdown%", tickProvider().toString())
        }
    }

    private class TrackingPlatformScheduler(
        private val delegate: PlatformScheduler,
    ) : PlatformScheduler {
        val repeatingHandles = CopyOnWriteArrayList<TrackingTaskHandle>()

        override val isFolia: Boolean
            get() = delegate.isFolia

        override fun isPlayerThread(player: Player): Boolean = delegate.isPlayerThread(player)

        override fun executeFor(player: Player, task: Runnable) = delegate.executeFor(player, task)

        override fun executeGlobal(task: Runnable) = delegate.executeGlobal(task)

        override fun runLaterAsync(delayTicks: Long, task: Runnable): TaskHandle {
            return delegate.runLaterAsync(delayTicks, task)
        }

        override fun runLaterFor(player: Player, delayTicks: Long, task: Runnable): TaskHandle {
            return delegate.runLaterFor(player, delayTicks, task)
        }

        override fun runRepeatingFor(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle {
            val handle = TrackingTaskHandle(delegate.runRepeatingFor(player, delayTicks, periodTicks, task), task)
            repeatingHandles += handle
            return handle
        }
    }

    private class TrackingTaskHandle(
        private val delegate: TaskHandle,
        private val task: Runnable,
    ) : TaskHandle {
        var cancelled: Boolean = false
            private set

        override fun cancel() {
            cancelled = true
            delegate.cancel()
        }

        fun runOnce() {
            task.run()
        }
    }
}
