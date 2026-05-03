package cc.keer.amenu.service

import cc.keer.amenu.config.PageEntryDefinition
import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.platform.TaskHandle
import cc.keer.amenu.service.provider.MenuDataProvider
import cc.keer.amenu.service.provider.MenuProviderRegistry
import cc.keer.amenu.service.provider.ProviderCache
import cc.keer.amenu.service.provider.ProviderRequest
import cc.keer.amenu.service.provider.ProviderResult
import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

class MenuServiceCompatibilityTest : MenuPluginTestHarness() {

    @BeforeEach
    fun installCompatibilityMenus() {
        writeMenu(
            "menu",
            """
            title: "Main"
            layout:
              - "#########"
              - "#N#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "N":
                material: BOOK
                name: "Next"
                click:
                  - "[open compat-next]"
            """,
        )
        writeMenu(
            "compat-next",
            """
            title: "Compat Next"
            layout:
              - "#########"
              - "#########"
              - "#########"
            fill:
              material: BLACK_STAINED_GLASS_PANE
              name: " "
            """,
        )
        plugin.saveConfig()
        plugin.reloadPlugin()
    }

    @Test
    fun open_named_menu_still_succeeds_after_scheduler_handoff() {
        server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable { plugin.menuService.openMenu(player, "menu", navigation = NavigationMode.ROOT) },
        )
        server.scheduler.waitAsyncTasksFinished()
        advanceTicks(1L)

        assertEquals("menu", currentMenuId())
    }

    @Test
    fun click_actions_continue_to_open_secondary_menus() {
        plugin.menuService.openMenu(player, "menu", navigation = NavigationMode.ROOT)
        server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable { plugin.menuService.handleClick(player, "menu", slotOf("menu", 'N')) },
        )
        server.scheduler.waitAsyncTasksFinished()
        advanceTicks(1L)

        assertEquals("compat-next", currentMenuId())
    }

    @Test
    fun async_provider_completion_rerenders_through_platform_safe_handoff() {
        writeMenu(
            "compat-provider",
            """
            title: "Compat Provider"
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
                  type: async-compat-provider
                loading:
                  material: CLOCK
                  name: "Loading async data"
                entries:
                  fallback:
                    material: PAPER
                    name: "Fallback"
            """.trimIndent(),
        )

        val future = CompletableFuture<ProviderResult>()
        val registry = MenuProviderRegistry()
        registry.register(
            "async-compat-provider",
            object : MenuDataProvider {
                override fun load(request: ProviderRequest): CompletionStage<ProviderResult> {
                    return future
                }
            },
        )
        val scheduler = TrackingPlatformScheduler(plugin.platformScheduler)
        val service = MenuService(
            plugin = plugin,
            settings = plugin.settings,
            repository = plugin.menuRepository,
            platformScheduler = scheduler,
            placeholderPipeline = plugin.menuService.placeholderPipeline,
            providerRegistry = registry,
            providerCache = ProviderCache(tickProvider = AtomicLong(0L)::get),
        )

        service.openMenu(player, "compat-provider", navigation = NavigationMode.ROOT)
        assertEquals("compat-provider", currentMenuId())
        assertEquals("CLOCK", currentItem('R')?.type?.name)

        server.scheduler.runTaskAsynchronously(
            plugin,
            Runnable {
                future.complete(
                    ProviderResult.Success(
                        entries = listOf(
                            PageEntryDefinition(
                                id = "compat",
                                icon = cc.keer.amenu.config.IconDefinition(
                                    materialName = "EMERALD",
                                    texture = null,
                                    name = "Async reward",
                                    lore = emptyList(),
                                    amount = 1,
                                    glow = false,
                                ),
                                actions = emptyList(),
                                placeholders = emptyMap(),
                            ),
                        ),
                    ),
                )
            },
        )
        server.scheduler.waitAsyncTasksFinished()
        advanceTicks(1L)

        assertTrue(scheduler.executeForCalls.any { it == player.name })
        assertEquals("EMERALD", currentItem('R')?.type?.name)
    }

    private class TrackingPlatformScheduler(
        private val delegate: PlatformScheduler,
    ) : PlatformScheduler {
        val executeForCalls = CopyOnWriteArrayList<String>()

        override val isFolia: Boolean
            get() = delegate.isFolia

        override fun isPlayerThread(player: org.bukkit.entity.Player): Boolean = delegate.isPlayerThread(player)

        override fun executeFor(player: org.bukkit.entity.Player, task: Runnable) {
            executeForCalls += player.name
            delegate.executeFor(player, task)
        }

        override fun executeGlobal(task: Runnable) = delegate.executeGlobal(task)

        override fun runLaterAsync(delayTicks: Long, task: Runnable): TaskHandle {
            return delegate.runLaterAsync(delayTicks, task)
        }

        override fun runLaterFor(player: org.bukkit.entity.Player, delayTicks: Long, task: Runnable): TaskHandle {
            return delegate.runLaterFor(player, delayTicks, task)
        }

        override fun runRepeatingFor(player: org.bukkit.entity.Player, delayTicks: Long, periodTicks: Long, task: Runnable): TaskHandle {
            return delegate.runRepeatingFor(player, delayTicks, periodTicks, task)
        }
    }
}
