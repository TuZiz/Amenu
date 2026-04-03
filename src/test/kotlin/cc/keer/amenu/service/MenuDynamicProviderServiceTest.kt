package cc.keer.amenu.service

import cc.keer.amenu.config.PageEntryDefinition
import cc.keer.amenu.config.PageRegionDefinition
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class MenuDynamicProviderServiceTest : MenuPluginTestHarness() {

    private val plainText = PlainTextComponentSerializer.plainText()
    private val ticks = AtomicLong(0L)

    private lateinit var trackingScheduler: TrackingPlatformScheduler
    private lateinit var registry: MenuProviderRegistry
    private lateinit var cache: ProviderCache

    @BeforeEach
    fun setUpDynamicRuntime() {
        trackingScheduler = TrackingPlatformScheduler(plugin.platformScheduler)
        registry = MenuProviderRegistry()
        cache = ProviderCache(tickProvider = ticks::get)
    }

    @Test
    fun provider_results_are_cached_per_viewer_menu_surface_and_invalidated_by_refresh() {
        writeMenu(
            "provider-cache",
            """
            title: "Provider Cache"
            layout:
              - "#########"
              - "#RRRXSSS#"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "X":
                material: FEATHER
                name: "Refresh rewards"
                click:
                  - "page: refresh rewards"
            pages:
              rewards:
                symbol: "R"
                provider:
                  type: counting-entries
                  cache:
                    ttl: 40
                loading:
                  material: CLOCK
                  name: "Loading rewards"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback reward"
              state:
                symbol: "S"
                provider:
                  type: counting-entries
                  cache:
                    ttl: 40
                loading:
                  material: CLOCK
                  name: "Loading state"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback state"
            """.trimIndent(),
        )

        val provider = RecordingProvider { request ->
            CompletableFuture.completedFuture(
                ProviderResult.Success(
                    entries = listOf(entry("${request.surfaceId}-${request.viewerId}", request.surfaceId)),
                ),
            )
        }
        registry.register("counting-entries", provider)
        val service = createService(registry = registry, cache = cache)

        service.openMenu(player, "provider-cache", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        service.openMenu(player, "provider-cache", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        val secondViewer = server.addPlayer("SecondViewer")
        service.openMenu(secondViewer, "provider-cache", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        service.handleClick(player, "provider-cache", slotOf("provider-cache", 'X'))
        advanceTicks(1L)

        assertEquals(3, provider.requests.count { it.surfaceId == "rewards" })
        assertEquals(2, provider.requests.count { it.surfaceId == "state" })
        assertEquals(
            2,
            provider.requests
                .filter { it.surfaceId == "rewards" }
                .map(ProviderRequest::viewerId)
                .distinct()
                .size,
        )

        val key = ProviderCache.Key(
            viewerId = player.uniqueId,
            menuId = "provider-cache",
            surfaceId = "rewards",
            providerType = "counting-entries",
        )
        assertEquals(player.uniqueId, key.viewerId)
        assertEquals("provider-cache", key.menuId)
        assertEquals("rewards", key.surfaceId)
        assertEquals("counting-entries", key.providerType)
    }

    @Test
    fun provider_request_params_are_resolved_before_invocation() {
        writeMenu(
            "provider-params",
            """
            title: "Provider Params"
            layout:
              - "#########"
              - "#RRR#####"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            pages:
              rewards:
                symbol: "R"
                provider:
                  type: request-recorder
                  params:
                    viewer-name: "{player}"
                    external-name: "%player_name%"
                loading:
                  material: CLOCK
                  name: "Loading"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback reward"
            """.trimIndent(),
        )

        val enabledProvider = RecordingProvider { request ->
            CompletableFuture.completedFuture(ProviderResult.Success(entries = listOf(entry("resolved", request.resolvedParams.getValue("viewer-name")))))
        }
        registry.register("request-recorder", enabledProvider)
        val enabledService = createService(
            registry = registry,
            cache = cache,
            placeholderPipeline = PlaceholderPipeline(FakePlaceholderApiBridge(enabled = true)),
        )

        enabledService.openMenu(player, "provider-params", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        val enabledRequest = enabledProvider.requests.single()
        assertEquals("Tester", enabledRequest.resolvedParams["viewer-name"])
        assertEquals("Tester", enabledRequest.resolvedParams["external-name"])

        val disabledRegistry = MenuProviderRegistry()
        val disabledProvider = RecordingProvider { request ->
            CompletableFuture.completedFuture(ProviderResult.Success(entries = listOf(entry("disabled", request.resolvedParams.getValue("viewer-name")))))
        }
        disabledRegistry.register("request-recorder", disabledProvider)
        val disabledService = createService(
            registry = disabledRegistry,
            cache = ProviderCache(tickProvider = ticks::get),
            placeholderPipeline = PlaceholderPipeline(FakePlaceholderApiBridge(enabled = false)),
        )

        disabledService.openMenu(player, "provider-params", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        val disabledRequest = disabledProvider.requests.single()
        assertEquals("Tester", disabledRequest.resolvedParams["viewer-name"])
        assertEquals("%player_name%", disabledRequest.resolvedParams["external-name"])
    }

    @Test
    fun cache_expiry_reloads_through_loading_or_stale_last_good_fallback() {
        writeMenu(
            "provider-expiry",
            """
            title: "Provider Expiry"
            layout:
              - "#########"
              - "#RRR#####"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            pages:
              rewards:
                symbol: "R"
                provider:
                  type: expiring-entries
                  cache:
                    ttl: 5
                loading:
                  material: CLOCK
                  name: "Loading next reward"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback reward"
            """.trimIndent(),
        )

        val nextReload = CompletableFuture<ProviderResult>()
        val loads = AtomicLong(0L)
        val provider = RecordingProvider {
            if (loads.incrementAndGet() == 1L) {
                CompletableFuture.completedFuture(
                    ProviderResult.Success(
                        entries = listOf(entry("first", "First reward", Material.EMERALD)),
                    ),
                )
            } else {
                nextReload
            }
        }
        registry.register("expiring-entries", provider)
        val service = createService(registry = registry, cache = cache)

        service.openMenu(player, "provider-expiry", navigation = NavigationMode.ROOT)
        advanceTicks(1L)
        assertEquals("First reward", plainName(currentItem('R')))

        ticks.set(6L)
        service.openMenu(player, "provider-expiry", navigation = NavigationMode.ROOT)

        val beforeReload = currentItem('R')
        assertNotNull(beforeReload)
        assertTrue(beforeReload!!.type == Material.CLOCK || plainName(beforeReload) == "First reward")

        nextReload.complete(
            ProviderResult.Success(
                entries = listOf(entry("second", "Second reward", Material.DIAMOND)),
            ),
        )
        advanceTicks(1L)

        assertEquals("Second reward", plainName(currentItem('R')))
    }

    @Test
    fun button_state_provider_values_flow_through_placeholder_pipeline() {
        writeMenu(
            "provider-states",
            """
            title: "Provider States"
            layout:
              - "#########"
              - "#GXR#####"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "G":
                material: BARRIER
                name: "Gift loading"
                states:
                  available:
                    conditions:
                      placeholder-equals:
                        "{reward-state}": "available"
                    material: EMERALD
                    name: "Gift for {viewer-name}"
                    lore:
                      - "Owner {player}"
                  claimed:
                    conditions:
                      placeholder-equals:
                        "{reward-state}": "claimed"
                    material: CHEST
                    name: "Claimed by {viewer-name}"
                    lore:
                      - "Already claimed"
              "X":
                material: FEATHER
                name: "Refresh state"
                click:
                  - "page: refresh reward-state"
            pages:
              reward-state:
                symbol: "R"
                provider:
                  type: placeholder-state
                  cache:
                    ttl: 40
                  params:
                    viewer-name: "%player_name%"
                    reward-state: "available"
                loading:
                  material: CLOCK
                  name: "Loading reward state"
                entries:
                  fallback:
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
            "placeholder-state",
            RecordingProvider { _ ->
                CompletableFuture.completedFuture(
                    ProviderResult.Success(
                        placeholders = states.removeAt(0),
                    ),
                )
            },
        )
        val service = createService(
            registry = registry,
            cache = cache,
            placeholderPipeline = PlaceholderPipeline(FakePlaceholderApiBridge(enabled = true)),
        )

        service.openMenu(player, "provider-states", navigation = NavigationMode.ROOT)
        advanceTicks(1L)

        assertEquals(Material.EMERALD, currentItem('G')!!.type)
        assertEquals("Gift for Tester", plainName(currentItem('G')))
        assertEquals(listOf("Owner Tester"), plainLore(currentItem('G')))

        service.handleClick(player, "provider-states", slotOf("provider-states", 'X'))
        advanceTicks(1L)

        assertEquals(Material.CHEST, currentItem('G')!!.type)
        assertEquals("Claimed by Tester", plainName(currentItem('G')))
        assertEquals(listOf("Already claimed"), plainLore(currentItem('G')))
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
            icon = cc.keer.amenu.config.IconDefinition(
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
        val requests = CopyOnWriteArrayList<ProviderRequest>()

        override fun load(request: ProviderRequest): CompletionStage<ProviderResult> {
            requests += request
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

    private class TrackingPlatformScheduler(
        private val delegate: PlatformScheduler,
    ) : PlatformScheduler {
        val executeForCalls = CopyOnWriteArrayList<String>()

        override val isFolia: Boolean
            get() = delegate.isFolia

        override fun isPlayerThread(player: Player): Boolean = delegate.isPlayerThread(player)

        override fun executeFor(player: Player, task: Runnable) {
            executeForCalls += player.name
            delegate.executeFor(player, task)
        }

        override fun executeGlobal(task: Runnable) = delegate.executeGlobal(task)

        override fun runLaterAsync(delayTicks: Long, task: Runnable): TaskHandle {
            return delegate.runLaterAsync(delayTicks, task)
        }

        override fun runLaterFor(player: Player, delayTicks: Long, task: Runnable): TaskHandle {
            return delegate.runLaterFor(player, delayTicks, task)
        }

        override fun runRepeatingFor(player: Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle {
            return delegate.runRepeatingFor(player, delayTicks, periodTicks, task)
        }
    }
}
