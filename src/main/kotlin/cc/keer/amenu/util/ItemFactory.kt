package cc.keer.amenu.util

import cc.keer.amenu.config.IconDefinition
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

object ItemFactory {

    fun create(
        icon: IconDefinition,
        placeholders: Map<String, String>,
    ): ItemStack {
        val material = resolveMaterial(icon)
        val item = ItemStack(material, icon.amount.coerceIn(1, 64))
        val meta = item.itemMeta ?: return item

        if (meta is SkullMeta && !icon.texture.isNullOrBlank()) {
            AdventureAccess.applySkullTexture(meta, icon.texture)
        }

        icon.name?.let {
            AdventureAccess.applyDisplayName(meta, TextFormatter.component(applyPlaceholders(it, placeholders)))
        }
        if (icon.lore.isNotEmpty()) {
            AdventureAccess.applyLore(
                meta,
                icon.lore.map { line -> TextFormatter.component(applyPlaceholders(line, placeholders)) },
            )
        }
        if (icon.glow) {
            meta.addEnchant(Enchantment.LURE, 1, true)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
        item.itemMeta = meta
        return item
    }

    private fun resolveMaterial(icon: IconDefinition): Material {
        if (!icon.texture.isNullOrBlank()) {
            return Material.PLAYER_HEAD
        }
        return icon.materialName
            ?.let { Material.matchMaterial(it.uppercase()) }
            ?: Material.PAPER
    }

    private fun applyPlaceholders(text: String, placeholders: Map<String, String>): String {
        return placeholders.entries.fold(text) { current, (key, value) ->
            current.replace("{$key}", value)
        }
    }
}
