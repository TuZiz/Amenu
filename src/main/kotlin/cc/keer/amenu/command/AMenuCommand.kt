package cc.keer.amenu.command

import cc.keer.amenu.AMenuPlugin
import cc.keer.amenu.service.NavigationMode
import cc.keer.amenu.util.AdventureAccess
import cc.keer.amenu.util.BindingItemAccess
import cc.keer.amenu.util.TextFormatter
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.Material
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

            val report = plugin.reloadPlugin()
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
            val targetMenu = args.getOrNull(1)?.takeIf { it.isNotBlank() } ?: plugin.settings.defaultMenuId
            plugin.menuService.openMenu(player, targetMenu, navigation = NavigationMode.ROOT)
            return true
        }

        plugin.menuService.openDefaultMenu(player)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>,
    ): MutableList<String> {
        return when (args.size) {
            1 -> listOf("open", "reload")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()

            2 -> if (args[0].equals("open", ignoreCase = true)) {
                plugin.menuRepository.listResolvableMenuIds()
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                    .toMutableList()
            } else {
                mutableListOf()
            }

            else -> mutableListOf()
        }
    }

    private fun handleGive(player: Player, args: Array<out String>): Boolean {
        return when (args.getOrNull(1)?.lowercase()) {
            "browser-compass" -> {
                player.inventory.setItemInMainHand(browserCompass())
                plugin.menuService.sendRawMessage(player, "<green>Received browser-compass.</green>")
                true
            }

            else -> {
                plugin.menuService.sendRawMessage(player, "<red>Unknown give target.</red>")
                true
            }
        }
    }

    private fun browserCompass(): ItemStack {
        val item = ItemStack(Material.COMPASS)
        val meta = item.itemMeta ?: return item
        BindingItemAccess.write(plugin, meta, "browser-compass")
        AdventureAccess.applyDisplayName(meta, TextFormatter.component("<aqua><bold>AMenu Browser</bold></aqua>"))
        AdventureAccess.applyLore(
            meta,
            listOf(
                TextFormatter.component("<gray>Right click to open the showcase binding.</gray>"),
                TextFormatter.component("<dark_gray>binding-id: browser-compass</dark_gray>"),
            ),
        )
        item.itemMeta = meta
        return item
    }
}
