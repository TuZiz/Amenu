package cc.keer.amenu.command

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.gui.MenuHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock

class AMenuCommandTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: AMenuPlugin
    private lateinit var player: PlayerMock

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
        assertTrue(server.dispatchCommand(player, "amenu"))

        val defaultHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(defaultHolder)
        assertEquals("main", defaultHolder!!.menuId)

        assertTrue(server.dispatchCommand(player, "amenu open history"))

        val namedHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(namedHolder)
        assertEquals("history", namedHolder!!.menuId)
    }

    @Test
    fun reload_keeps_bundled_command_entrypoints_available() {
        player.addAttachment(plugin, "amenu.admin", true)
        player.recalculatePermissions()

        assertTrue(server.dispatchCommand(player, "amenu reload"))
        assertNotNull(player.nextComponentMessage())
        assertNotNull(plugin.menuRepository.menu("main"))
        assertNotNull(plugin.menuRepository.menu("history"))
        assertNotNull(plugin.menuRepository.menu("admin"))
        assertNotNull(plugin.menuRepository.menu("runtime"))

        assertTrue(server.dispatchCommand(player, "amenu"))
        val defaultHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(defaultHolder)
        assertEquals("main", defaultHolder!!.menuId)

        assertTrue(server.dispatchCommand(player, "amenu open history"))
        val namedHolder = player.openInventory.topInventory.holder as? MenuHolder
        assertNotNull(namedHolder)
        assertEquals("history", namedHolder!!.menuId)
    }
}
