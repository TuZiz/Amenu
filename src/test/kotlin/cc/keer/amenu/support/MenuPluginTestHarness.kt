package cc.keer.amenu.support

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.gui.MenuBindingListener
import cc.keer.amenu.gui.MenuHolder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.block.BlockFace
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import java.io.File

abstract class MenuPluginTestHarness {

    protected lateinit var server: ServerMock
    protected lateinit var plugin: AMenuPlugin
    protected lateinit var player: PlayerMock

    private val serializer = PlainTextComponentSerializer.plainText()

    @BeforeEach
    open fun setUpHarness() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(AMenuPlugin::class.java)
        player = server.addPlayer("Tester")

        plugin.config.set("chat-input.timeout-seconds", 5)
        plugin.config.set("chat-input.global-cancel-keywords", listOf("cancel", "取消"))
        plugin.config.set("messages.prefix", "[AMenu] ")
        plugin.config.set("messages.only-player", "只有玩家可以使用这个命令。")
        plugin.config.set("messages.no-permission", "你没有权限执行这个操作。")
        plugin.config.set("messages.reloaded", "已重载菜单配置。")
        plugin.config.set("messages.prompt-replaced", "你原有的输入流程已被新的输入请求替换。")
        plugin.config.set("messages.prompt-timeout", "输入流程已超时，请重新打开菜单后再试一次。")
        plugin.config.set("messages.menu-missing", "菜单不存在：{menu}")
        plugin.config.set("messages.prompt-missing", "输入流程不存在：{prompt}")
        plugin.saveConfig()
        plugin.reloadPlugin()
        plugin.configHotReloadService.stop()
    }

    @AfterEach
    open fun tearDownHarness() {
        MockBukkit.unmock()
    }

    protected fun writeMenu(menuId: String, yaml: String) {
        val menuFile = File(plugin.dataFolder, "menus/$menuId.yml")
        menuFile.parentFile.mkdirs()
        menuFile.writeText(yaml.trimIndent() + System.lineSeparator(), Charsets.UTF_8)
        plugin.menuRepository.loadMenus()
    }

    protected fun openMenu(menuId: String) {
        plugin.menuService.openMenu(player, menuId, navigation = cc.keer.amenu.service.NavigationMode.ROOT)
    }

    protected fun currentMenuId(): String? {
        return (player.openInventory.topInventory?.holder as? MenuHolder)?.menuId
    }

    protected fun slotOf(menuId: String, symbol: Char, occurrence: Int = 0): Int {
        val menu = requireNotNull(plugin.menuRepository.menu(menuId)) { "Menu $menuId was not loaded." }
        var seen = 0
        menu.layout.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, current ->
                if (current == symbol) {
                    if (seen == occurrence) {
                        return rowIndex * 9 + columnIndex
                    }
                    seen++
                }
            }
        }
        error("Could not find symbol '$symbol' in menu '$menuId'")
    }

    protected fun clickCurrent(symbol: Char, occurrence: Int = 0) {
        val menuId = requireNotNull(currentMenuId()) { "Player does not currently have an AMenu inventory open." }
        plugin.menuService.handleClick(player, menuId, slotOf(menuId, symbol, occurrence))
    }

    protected fun clickCurrentSlot(slot: Int) {
        val menuId = requireNotNull(currentMenuId()) { "Player does not currently have an AMenu inventory open." }
        plugin.menuService.handleClick(player, menuId, slot)
    }

    protected fun click(menuId: String, symbol: Char, occurrence: Int = 0) {
        plugin.menuService.handleClick(player, menuId, slotOf(menuId, symbol, occurrence))
    }

    protected fun currentItem(symbol: Char, occurrence: Int = 0): ItemStack? {
        val menuId = requireNotNull(currentMenuId()) { "Player does not currently have an AMenu inventory open." }
        return player.openInventory.topInventory.getItem(slotOf(menuId, symbol, occurrence))
    }

    protected fun interactWith(item: ItemStack, action: Action = Action.RIGHT_CLICK_AIR) {
        player.inventory.setItemInMainHand(item)
        MenuBindingListener(plugin, plugin.menuRepository, plugin.menuService).onPlayerInteract(
            PlayerInteractEvent(player, action, item, null, BlockFace.SELF, EquipmentSlot.HAND),
        )
        server.scheduler.waitAsyncEventsFinished()
        server.scheduler.performTicks(1)
        server.scheduler.waitAsyncEventsFinished()
    }

    protected fun advanceTicks(ticks: Long) {
        server.scheduler.performTicks(ticks)
        server.scheduler.waitAsyncTasksFinished()
        server.scheduler.waitAsyncEventsFinished()
        server.scheduler.performTicks(1)
        server.scheduler.waitAsyncTasksFinished()
        server.scheduler.waitAsyncEventsFinished()
    }

    protected fun submitChat(input: String) {
        player.chat(input)
        server.scheduler.waitAsyncEventsFinished()
        server.scheduler.performTicks(1)
        server.scheduler.waitAsyncTasksFinished()
    }

    protected fun nextPlainMessage(target: PlayerMock = player): String? {
        return runCatching { target.nextComponentMessage()?.let(serializer::serialize) }.getOrNull()
    }

    protected fun nextConsoleMessage(): String? {
        return runCatching { server.consoleSender.nextComponentMessage()?.let(serializer::serialize) }.getOrNull()
    }

    protected fun heardSoundNames(target: PlayerMock = player): List<String> {
        return target.heardSounds.map { it.sound }
    }

    protected fun registerRecordingCommand(
        name: String,
        sink: MutableList<String>,
    ) {
        server.commandMap.register(
            "amenu-test",
            object : Command(name) {
                override fun execute(
                    sender: CommandSender,
                    commandLabel: String,
                    args: Array<out String>,
                ): Boolean {
                    val payload = if (args.isEmpty()) "" else args.joinToString(" ")
                    sink += "${sender.name}|$commandLabel|$payload"
                    return true
                }
            },
        )
    }

    protected fun grantPermission(permission: String) {
        player.addAttachment(plugin, permission, true)
        player.recalculatePermissions()
    }

    protected fun denyPermission(permission: String) {
        player.addAttachment(plugin, permission, false)
        player.recalculatePermissions()
    }
}
