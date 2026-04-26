package cc.keer.amenu.command

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.gui.MenuBindingListener
import cc.keer.amenu.gui.MenuHolder
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.BindingItemAccess
import cc.keer.amenu.util.TextFormatter
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.io.File

class AMenuCommandTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: AMenuPlugin
    private lateinit var player: PlayerMock

    private val serializer = PlainTextComponentSerializer.plainText()

    private fun grantOpenPermission() {
        player.addAttachment(plugin, "amenu.open", true)
        player.recalculatePermissions()
    }

    private fun interactWith(item: ItemStack, action: Action = Action.RIGHT_CLICK_AIR) {
        player.inventory.setItemInMainHand(item)
        MenuBindingListener(plugin, plugin.menuRepository, plugin.menuService).onPlayerInteract(
            PlayerInteractEvent(player, action, item, null, BlockFace.SELF, EquipmentSlot.HAND),
        )
        server.scheduler.waitAsyncEventsFinished()
        server.scheduler.performTicks(1)
        server.scheduler.waitAsyncEventsFinished()
    }

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(AMenuPlugin::class.java)
        player = server.addPlayer()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun opens_default_and_named_menu() {
        grantOpenPermission()
        assertTrue(server.dispatchCommand(player, "amenu"))

        val defaultHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(defaultHolder)
        assertEquals("menu", defaultHolder!!.menuId)

        assertTrue(server.dispatchCommand(player, "amenu open history"))

        val namedHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(namedHolder)
        assertEquals("history", namedHolder!!.menuId)
    }

    @Test
    fun reload_keeps_bundled_command_entrypoints_available() {
        player.addAttachment(plugin, "amenu.admin", true)
        grantOpenPermission()
        player.recalculatePermissions()

        assertTrue(server.dispatchCommand(player, "amenu reload"))
        assertNotNull(player.nextComponentMessage())
        assertNotNull(plugin.menuRepository.menu("menu"))
        assertNotNull(plugin.menuRepository.menu("showcase"))
        assertNotNull(plugin.menuRepository.menu("history"))
        assertNotNull(plugin.menuRepository.menu("admin"))
        assertNotNull(plugin.menuRepository.menu("runtime"))

        assertTrue(server.dispatchCommand(player, "amenu"))
        val defaultHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(defaultHolder)
        assertEquals("menu", defaultHolder!!.menuId)

        assertTrue(server.dispatchCommand(player, "amenu open history"))
        val namedHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(namedHolder)
        assertEquals("history", namedHolder!!.menuId)
    }

    @Test
    fun reload_reports_invalid_menu_and_preserves_previous_loaded_state() {
        player.addAttachment(plugin, "amenu.admin", true)
        grantOpenPermission()
        player.recalculatePermissions()

        val menuFile = File(plugin.dataFolder, "menus/menu.yml")
        val original = menuFile.readText(Charsets.UTF_8)
        menuFile.writeText("Title: broken\nShape:\n  - \"`broken\"\n", Charsets.UTF_8)

        assertTrue(server.dispatchCommand(player, "amenu reload"))

        val message = serializer.serialize(requireNotNull(player.nextComponentMessage()))
        assertTrue(message.isNotBlank())
        assertNotNull(plugin.menuRepository.menu("menu"))

        assertTrue(server.dispatchCommand(player, "amenu"))
        val defaultHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(defaultHolder)
        assertEquals("menu", defaultHolder!!.menuId)

        menuFile.writeText(original, Charsets.UTF_8)
        assertTrue(server.dispatchCommand(player, "amenu reload"))
    }

    @Test
    fun removed_sync_bundled_command_no_longer_appears_in_tab_completion() {
        val command = plugin.getCommand("amenu")!!
        val completions = command.tabCompleter!!.onTabComplete(player, command, "amenu", arrayOf(""))!!
        assertTrue("reload" in completions)
        assertTrue("open" in completions)
        assertTrue("give" !in completions)
        assertTrue("sync-bundled" !in completions)
    }

    @Test
    fun command_entrypoint_remains_canonical_even_with_item_bindings() {
        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta!!
        BindingItemAccess.write(plugin, meta, "browser-compass")
        AdventureAccess.applyDisplayName(meta, TextFormatter.component("<aqua><bold>AMenu 导航罗盘</bold></aqua>"))
        compass.itemMeta = meta
        player.inventory.setItemInMainHand(compass)

        interactWith(compass, Action.RIGHT_CLICK_AIR)
        val boundHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(boundHolder)
        assertEquals("showcase", boundHolder!!.menuId)

        grantOpenPermission()
        assertTrue(server.dispatchCommand(player, "amenu"))
        val commandHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(commandHolder)
        assertEquals("menu", commandHolder!!.menuId)
    }

    @Test
    fun direct_command_open_requires_permission_but_bindings_and_internal_navigation_do_not() {
        assertTrue(server.dispatchCommand(player, "amenu"))
        assertNull(player.openInventory.topInventory?.holder as? MenuHolder)
        assertNotNull(player.nextComponentMessage())

        val compass = ItemStack(Material.COMPASS)
        val meta = compass.itemMeta!!
        BindingItemAccess.write(plugin, meta, "browser-compass")
        AdventureAccess.applyDisplayName(meta, TextFormatter.component("<aqua><bold>AMenu 瀵艰埅缃楃洏</bold></aqua>"))
        compass.itemMeta = meta
        player.inventory.setItemInMainHand(compass)

        interactWith(compass, Action.RIGHT_CLICK_AIR)
        assertEquals("showcase", (player.openInventory.topInventory.holder as? MenuHolder)?.menuId)

        plugin.menuService.handleClick(player, "showcase", 9)
        assertEquals("history", (player.openInventory.topInventory.holder as? MenuHolder)?.menuId)
    }

    @Test
    fun subfolder_menu_can_open_by_unique_basename_alias() {
        grantOpenPermission()

        val menuFile = File(plugin.dataFolder, "menus/points/topup.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Top Up"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: EMERALD
                name: "Top Up"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        plugin.reloadPlugin()

        assertTrue(server.dispatchCommand(player, "amenu open topup"))
        assertEquals("points/topup", (player.openInventory.topInventory.holder as? MenuHolder)?.menuId)
    }

    @Test
    fun open_tab_completion_hides_folder_style_menu_ids() {
        val menuFile = File(plugin.dataFolder, "menus/points/topup.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            title: "Top Up"
            layout:
              - "#########"
              - "#A#######"
              - "#########"
            fill:
              material: GRAY_STAINED_GLASS_PANE
              name: " "
            buttons:
              "A":
                material: EMERALD
                name: "Top Up"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        plugin.reloadPlugin()

        val command = plugin.getCommand("amenu")!!
        val completions = command.tabCompleter!!.onTabComplete(player, command, "amenu", arrayOf("open", ""))!!

        assertTrue("topup" in completions)
        assertTrue("menu" in completions)
        assertFalse("points/topup" in completions)
        assertFalse(completions.any { it.contains('/') })
    }

    @Test
    fun deleted_bundled_menu_is_not_restored_after_initial_bootstrap() {
        val menuFile = File(plugin.dataFolder, "menus/menu.yml")
        assertTrue(menuFile.exists())
        assertTrue(menuFile.delete())

        val bootstrap = AMenuPlugin::class.java.getDeclaredMethod("bootstrapFiles")
        bootstrap.isAccessible = true
        bootstrap.invoke(plugin)

        assertFalse(menuFile.exists())
    }

    @Test
    fun existing_data_folder_without_marker_does_not_reextract_bundled_menus() {
        val menuFile = File(plugin.dataFolder, "menus/menu.yml")
        val markerFile = File(plugin.dataFolder, ".bundled-menus-initialized")
        assertTrue(menuFile.exists())
        assertTrue(markerFile.exists())
        assertTrue(menuFile.delete())
        assertTrue(markerFile.delete())

        val bootstrap = AMenuPlugin::class.java.getDeclaredMethod("bootstrapFiles")
        bootstrap.isAccessible = true
        bootstrap.invoke(plugin)

        assertFalse(menuFile.exists())
        assertTrue(markerFile.exists())
    }

    @Test
    fun command_bindings_open_menu_without_real_command_registration() {
        val menuFile = File(plugin.dataFolder, "menus/command-bound.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(
            """
            Title: "Bound Command"
            Shape:
              - "#########"
              - "#`主页`#######"
            Fill:
              mats: GRAY_STAINED_GLASS_PANE
              name: " "
            BUTTONS:
              "主页":
                mats: COMPASS
                name: "<aqua>主页</aqua>"
            Bindings:
              command:
                - "cd"
                - "菜单"
            """.trimIndent() + System.lineSeparator(),
            Charsets.UTF_8,
        )
        plugin.reloadPlugin()

        val aliasEvent = PlayerCommandPreprocessEvent(player, "/cd")
        server.pluginManager.callEvent(aliasEvent)
        assertTrue(aliasEvent.isCancelled)
        assertEquals("command-bound", (player.openInventory.topInventory.holder as? MenuHolder)?.menuId)

        player.closeInventory()
        val chineseAliasEvent = PlayerCommandPreprocessEvent(player, "/菜单")
        server.pluginManager.callEvent(chineseAliasEvent)
        assertTrue(chineseAliasEvent.isCancelled)
        assertEquals("command-bound", (player.openInventory.topInventory.holder as? MenuHolder)?.menuId)
        player.closeInventory()
        val subcommandEvent = PlayerCommandPreprocessEvent(player, "/cd extra")
        server.pluginManager.callEvent(subcommandEvent)
        assertFalse(subcommandEvent.isCancelled)
        assertNull(player.openInventory.topInventory?.holder as? MenuHolder)
    }
}
