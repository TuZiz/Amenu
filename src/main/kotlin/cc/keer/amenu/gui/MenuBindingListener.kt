package cc.keer.amenu.gui

import cc.keer.amenu.config.MenuBindingAction
import cc.keer.amenu.config.MenuBindingDefinition
import cc.keer.amenu.config.MenuBindingType
import cc.keer.amenu.config.ComparisonOperator
import cc.keer.amenu.config.MenuCondition
import cc.keer.amenu.config.MenuRepository
import cc.keer.amenu.service.MenuService
import cc.keer.amenu.service.NavigationMode
import cc.keer.amenu.service.PlaceholderPipeline
import cc.keer.amenu.util.BindingItemAccess
import cc.keer.amenu.util.TextFormatter
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

class MenuBindingListener(
    private val plugin: org.bukkit.plugin.java.JavaPlugin,
    private val repository: MenuRepository,
    private val menuService: MenuService,
    private val placeholderPipeline: PlaceholderPipeline = menuService.placeholderPipeline,
) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val raw = event.message.removePrefix("/").trim()
        if (raw.isBlank()) {
            return
        }

        val commandName = raw.substringBefore(' ').trim()
        val argsText = raw.substringAfter(' ', "").trim()
        val player = event.player
        val binding = repository.listBindings().firstOrNull { candidate ->
            candidate.type == MenuBindingType.COMMAND && matchesCommandBinding(player, commandName, argsText, candidate)
        } ?: return

        event.isCancelled = true
        val base = mapOf(
            "binding-id" to binding.id,
            "binding-type" to binding.type.name.lowercase(),
            "binding-command" to commandName,
            "binding-args" to argsText,
            "binding-raw" to raw,
        )
        val placeholders = base + binding.placeholders.mapValues { (_, value) ->
            placeholderPipeline.render(player, value, base)
        }
        menuService.openMenu(player, binding.menuId, placeholders, NavigationMode.ROOT)
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != null && event.hand != EquipmentSlot.HAND) {
            return
        }

        val player = event.player
        val item = event.item ?: return
        if (item.type == Material.AIR) {
            return
        }

        val binding = repository.listBindings().firstOrNull { candidate ->
            candidate.type == MenuBindingType.ITEM && matchesItemBinding(player, item, event.action, candidate)
        } ?: return

        event.isCancelled = true
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)
        val base = mapOf(
            "binding-id" to binding.id,
            "binding-type" to binding.type.name.lowercase(),
            "binding-action" to event.action.name.lowercase(),
            "binding-material" to item.type.name.lowercase(),
        )
        val placeholders = base + binding.placeholders.mapValues { (_, value) ->
            placeholderPipeline.render(player, value, base)
        }
        menuService.openMenu(player, binding.menuId, placeholders, NavigationMode.ROOT)
    }

    private fun matchesItemBinding(
        player: Player,
        item: ItemStack,
        action: Action,
        binding: MenuBindingDefinition,
    ): Boolean {
        val base = mapOf(
            "binding-id" to binding.id,
            "binding-type" to binding.type.name.lowercase(),
            "binding-action" to action.name.lowercase(),
            "binding-material" to item.type.name.lowercase(),
        )
        val markedBindingId = BindingItemAccess.read(plugin, item.itemMeta)
        if (markedBindingId != null && !markedBindingId.equals(binding.id, ignoreCase = true)) {
            return false
        }
        if (!binding.materialName.isNullOrBlank()) {
            val expected = Material.matchMaterial(binding.materialName.uppercase()) ?: return false
            if (item.type != expected) {
                return false
            }
        }
        if (binding.permission != null && !player.hasPermission(binding.permission)) {
            return false
        }
        if (!matchesAction(action, binding.actions)) {
            return false
        }
        if (markedBindingId == null && !binding.name.isNullOrBlank()) {
            val itemName = resolvePlainName(item.itemMeta) ?: return false
            val bindingName = TextFormatter.plainString(TextFormatter.component(placeholderPipeline.render(player, binding.name, base)))
            if (itemName != bindingName) {
                return false
            }
        }

        val placeholders = base + binding.placeholders.mapValues { (_, value) ->
            placeholderPipeline.render(player, value, base)
        }

        return binding.conditions.all { condition ->
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

    private fun matchesCommandBinding(
        player: Player,
        commandName: String,
        argsText: String,
        binding: MenuBindingDefinition,
    ): Boolean {
        val alias = binding.commandAlias?.trim()?.removePrefix("/")?.lowercase() ?: return false
        if (commandName.lowercase() != alias) {
            return false
        }
        if (argsText.isNotBlank()) {
            return false
        }
        if (binding.permission != null && !player.hasPermission(binding.permission)) {
            menuService.sendSystemMessage(player, "no-permission")
            return false
        }

        val base = mapOf(
            "binding-id" to binding.id,
            "binding-type" to binding.type.name.lowercase(),
            "binding-command" to commandName,
            "binding-args" to argsText,
            "binding-raw" to buildString {
                append(commandName)
                if (argsText.isNotBlank()) {
                    append(' ')
                    append(argsText)
                }
            },
        )
        val placeholders = base + binding.placeholders.mapValues { (_, value) ->
            placeholderPipeline.render(player, value, base)
        }

        return binding.conditions.all { condition ->
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
            when (condition.operator) {
                ComparisonOperator.GREATER_THAN -> leftNumber > rightNumber
                ComparisonOperator.GREATER_THAN_OR_EQUAL -> leftNumber >= rightNumber
                ComparisonOperator.LESS_THAN -> leftNumber < rightNumber
                ComparisonOperator.LESS_THAN_OR_EQUAL -> leftNumber <= rightNumber
                ComparisonOperator.EQUAL -> leftNumber == rightNumber
                ComparisonOperator.NOT_EQUAL -> leftNumber != rightNumber
            }
        } else {
            val result = left.compareTo(right)
            when (condition.operator) {
                ComparisonOperator.GREATER_THAN -> result > 0
                ComparisonOperator.GREATER_THAN_OR_EQUAL -> result >= 0
                ComparisonOperator.LESS_THAN -> result < 0
                ComparisonOperator.LESS_THAN_OR_EQUAL -> result <= 0
                ComparisonOperator.EQUAL -> left == right
                ComparisonOperator.NOT_EQUAL -> left != right
            }
        }
    }

    private fun matchesAction(action: Action, configured: Set<MenuBindingAction>): Boolean {
        return configured.any { candidate ->
            when (candidate) {
                MenuBindingAction.RIGHT_CLICK -> action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK
                MenuBindingAction.LEFT_CLICK -> action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK
                MenuBindingAction.RIGHT_CLICK_AIR -> action == Action.RIGHT_CLICK_AIR
                MenuBindingAction.RIGHT_CLICK_BLOCK -> action == Action.RIGHT_CLICK_BLOCK
                MenuBindingAction.LEFT_CLICK_AIR -> action == Action.LEFT_CLICK_AIR
                MenuBindingAction.LEFT_CLICK_BLOCK -> action == Action.LEFT_CLICK_BLOCK
            }
        }
    }

    private fun resolvePlainName(meta: ItemMeta?): String? {
        if (meta == null) {
            return null
        }
        val componentGetter = meta.javaClass.methods.firstOrNull { it.name == "displayName" && it.parameterCount == 0 }
        val componentName = runCatching { componentGetter?.invoke(meta) }.getOrNull()
        if (componentName is net.kyori.adventure.text.Component) {
            return TextFormatter.plainString(componentName)
        }
        val legacyGetter = meta.javaClass.methods.firstOrNull { it.name == "getDisplayName" && it.parameterCount == 0 }
        val legacy = runCatching { legacyGetter?.invoke(meta) as? String }.getOrNull()
        return legacy?.let(ChatColor::stripColor)
    }

}
