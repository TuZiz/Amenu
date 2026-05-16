package cc.keer.amenu.command

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.FullInventoryAction
import cc.keer.amenu.config.IconDefinition
import cc.keer.amenu.config.MenuBindingDefinition
import cc.keer.amenu.config.MenuBindingType
import cc.keer.amenu.service.NavigationMode
import cc.keer.amenu.util.BindingItemAccess
import cc.keer.amenu.util.ItemFactory
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class AMenuCommand(
    private val plugin: AMenuPlugin,
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.isNotEmpty() && args[0].equals("reload", ignoreCase = true)) {
            if (!sender.hasPermission("amenu.admin")) {
                plugin.menuService.sendSystemMessage(sender, "no-permission")
                return true
            }

            sender.sendMessage("[AMenu] 正在异步重载配置...")
            plugin.reloadPluginAsync().whenComplete { report, throwable ->
                val deliver = Runnable {
                    if (throwable != null) {
                        sender.sendMessage("[AMenu] reload failed: ${throwable.message}")
                        return@Runnable
                    }
                    plugin.handleReloadReport(report, initiator = "command")
                    if (report.menuReport.successful) {
                        plugin.menuService.sendSystemMessage(sender, "reloaded")
                    } else {
                        val errorSummary = report.menuReport.errors.joinToString(" | ") { "${it.fileName}: ${it.message}" }
                        val playerSender = sender as? Player
                        if (playerSender != null) {
                            plugin.menuService.sendRawMessage(
                                playerSender,
                                "<red>菜单重载失败，已保留旧菜单。</red> <gray>$errorSummary</gray>",
                            )
                        } else {
                            sender.sendMessage("[AMenu] 菜单重载失败，已保留旧菜单。$errorSummary")
                        }
                    }
                }
                val playerSender = sender as? Player
                if (playerSender != null) {
                    plugin.platformScheduler.executeFor(playerSender, deliver)
                } else {
                    plugin.platformScheduler.executeGlobal(deliver)
                }
            }
            return true
        }

        if (args.isNotEmpty() && args[0].equals("give", ignoreCase = true)) {
            val player = sender as? Player
            if (player == null) {
                plugin.menuService.sendSystemMessage(sender, "only-player")
                return true
            }
            if (!player.hasPermission("amenu.admin")) {
                plugin.menuService.sendSystemMessage(player, "no-permission")
                return true
            }
            return handleGive(player, args)
        }

        val player = sender as? Player
        if (player == null) {
            plugin.menuService.sendSystemMessage(sender, "only-player")
            return true
        }

        val openPermission = plugin.settings.commandOpenPermission
        if (!openPermission.isNullOrBlank() && !player.hasPermission(openPermission)) {
            plugin.menuService.sendSystemMessage(player, "no-permission")
            return true
        }

        if (args.isNotEmpty() && args[0].equals("open", ignoreCase = true)) {
            val targetMenu = args.getOrNull(1)?.takeIf { it.isNotBlank() } ?: run {
                sender.sendMessage(command.usage)
                return true
            }
            plugin.menuService.openMenu(player, targetMenu, navigation = NavigationMode.ROOT)
            return true
        }

        sender.sendMessage(command.usage)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): MutableList<String> {
        return when (args.size) {
            1 -> listOf("open", "reload", "give")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()

            2 -> when {
                args[0].equals("open", ignoreCase = true) -> plugin.menuRepository.listResolvableMenuIds()
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                args[0].equals("give", ignoreCase = true) -> itemBindingIds()
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()

                else -> mutableListOf()
            }

            else -> mutableListOf()
        }
    }

    private fun handleGive(player: Player, args: Array<out String>): Boolean {
        val bindingId = args.getOrNull(1)?.takeIf { it.isNotBlank() }
        if (bindingId == null) {
            plugin.menuService.sendRawMessage(player, "<red>Usage: /amenu give <bindingId></red>")
            return true
        }
        val binding = plugin.menuRepository.listBindings().firstOrNull { candidate ->
            candidate.type == MenuBindingType.ITEM && candidate.id.equals(bindingId, ignoreCase = true)
        }
        if (binding == null) {
            plugin.menuService.sendRawMessage(player, "<red>Unknown item binding: <yellow>$bindingId</yellow></red>")
            return true
        }

        giveBindingItem(player, boundItem(player, binding), binding.id)
        return true
    }

    private fun giveBindingItem(player: Player, item: ItemStack, bindingId: String) {
        val leftovers = player.inventory.addItem(item)
        if (leftovers.isEmpty()) {
            plugin.menuService.sendRawMessage(player, "<green>Received binding item: <yellow>$bindingId</yellow></green>")
            return
        }

        when (plugin.settings.fullInventoryAction) {
            FullInventoryAction.DROP -> {
                leftovers.values.forEach { leftover ->
                    player.world.dropItemNaturally(player.location, leftover)
                }
                plugin.menuService.sendRawMessage(player, "<yellow>背包已满，绑定物品已掉落在脚下。</yellow>")
            }

            FullInventoryAction.DENY -> {
                plugin.menuService.sendRawMessage(player, "<red>背包已满，未发放绑定物品。</red>")
            }
        }
    }

    private fun boundItem(player: Player, binding: MenuBindingDefinition): ItemStack {
        val placeholders = mapOf(
            "binding-id" to binding.id,
            "binding-type" to binding.type.name.lowercase(),
            "binding-menu" to binding.menuId,
        ) + binding.placeholders.mapValues { (_, value) ->
            plugin.menuService.placeholderPipeline.render(player, value)
        }
        val item = ItemFactory.create(
            IconDefinition(
                materialName = binding.materialName ?: "COMPASS",
                texture = null,
                name = binding.name ?: "<aqua><bold>AMenu Binding</bold></aqua>",
                lore = listOf(
                    "<gray>Right click to open <white>{binding-menu}</white>.</gray>",
                    "<dark_gray>binding-id: {binding-id}</dark_gray>",
                ),
                amount = 1,
                glow = false,
            ),
            placeholders,
        )
        val meta = item.itemMeta ?: return item
        BindingItemAccess.write(plugin, meta, binding.id)
        item.itemMeta = meta
        return item
    }

    private fun itemBindingIds(): List<String> {
        return plugin.menuRepository.listBindings()
            .asSequence()
            .filter { it.type == MenuBindingType.ITEM }
            .map(MenuBindingDefinition::id)
            .distinct()
            .sorted()
            .toList()
    }
}
