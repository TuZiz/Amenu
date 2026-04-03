package cc.keer.amenu.config

data class MenuDefinition(
    val id: String,
    val title: String,
    val rows: Int,
    val layout: List<String>,
    val prompts: Map<String, PromptDefinition>,
    val buttons: Map<Char, ButtonDefinition>,
    val pageRegions: Map<String, PageRegionDefinition>,
    val bindings: List<MenuBindingDefinition>,
) {
    val size: Int = rows * 9

    fun buttonAt(slot: Int): ButtonDefinition? {
        return symbolAt(slot)?.let(buttons::get)
    }

    fun pageRegionAt(slot: Int): PageRegionDefinition? {
        val symbol = symbolAt(slot) ?: return null
        return pageRegions.values.firstOrNull { it.symbol == symbol }
    }

    fun slotsFor(symbol: Char): List<Int> {
        if (symbol == ' ') {
            return emptyList()
        }
        val slots = mutableListOf<Int>()
        for (row in layout.indices) {
            for (column in 0 until 9) {
                if (layout[row][column] == symbol) {
                    slots += (row * 9) + column
                }
            }
        }
        return slots
    }

    fun pageSlots(regionId: String): List<Int> {
        val region = pageRegions[regionId] ?: return emptyList()
        return slotsFor(region.symbol)
    }

    private fun symbolAt(slot: Int): Char? {
        if (slot < 0 || slot >= size) {
            return null
        }
        val row = slot / 9
        val column = slot % 9
        return layout[row][column]
    }
}

data class PromptDefinition(
    val id: String,
    val type: PromptType,
    val startMessages: List<String>,
    val cancelKeywords: Set<String>,
    val timeoutSeconds: Long?,
    val submitActions: List<MenuAction>,
    val cancelActions: List<MenuAction>,
    val invalidActions: List<MenuAction>,
    val validation: PromptValidation?,
    val signLines: List<String>,
    val anvilTitle: String?,
    val anvilText: String?,
)

data class PromptValidation(
    val equals: String?,
    val matches: Regex?,
    val ignoreCase: Boolean,
)

enum class PromptType {
    CHAT,
    SIGN,
    ANVIL,
}

data class ButtonDefinition(
    val symbol: Char,
    val icon: IconDefinition,
    val actions: List<MenuAction>,
    val permission: String?,
    val visiblePermission: String?,
    val denyActions: List<MenuAction>,
    val conditions: List<MenuCondition>,
    val states: List<ButtonStateDefinition>,
)

data class ButtonStateDefinition(
    val id: String,
    val conditions: List<MenuCondition>,
    val icon: IconDefinition?,
    val actions: List<MenuAction>?,
    val permission: String?,
    val visiblePermission: String?,
    val denyActions: List<MenuAction>?,
)

data class ProviderCacheDefinition(
    val ttl: Long?,
)

data class ProviderUpdateDefinition(
    val interval: Long,
)

data class ProviderDefinition(
    val type: String,
    val params: Map<String, String>,
    val cache: ProviderCacheDefinition?,
    val update: ProviderUpdateDefinition?,
)

data class ProviderSurfaceDefinition(
    val loading: IconDefinition?,
    val empty: IconDefinition?,
    val error: IconDefinition?,
)

data class PageRegionDefinition(
    val id: String,
    val symbol: Char,
    val entries: List<PageEntryDefinition>,
    val provider: ProviderDefinition?,
    val surface: ProviderSurfaceDefinition,
    val loadingIcon: IconDefinition,
    val emptyIcon: IconDefinition,
    val errorIcon: IconDefinition?,
    val asyncDelayTicks: Long,
)

data class PageEntryDefinition(
    val id: String,
    val icon: IconDefinition,
    val actions: List<MenuAction>,
    val placeholders: Map<String, String>,
)

data class MenuBindingDefinition(
    val id: String,
    val menuId: String,
    val type: MenuBindingType,
    val materialName: String?,
    val name: String?,
    val actions: Set<MenuBindingAction>,
    val permission: String?,
    val placeholders: Map<String, String>,
    val conditions: List<MenuCondition>,
)

enum class MenuBindingType {
    ITEM,
}

enum class MenuBindingAction {
    RIGHT_CLICK,
    LEFT_CLICK,
    RIGHT_CLICK_AIR,
    RIGHT_CLICK_BLOCK,
    LEFT_CLICK_AIR,
    LEFT_CLICK_BLOCK,
}

sealed interface MenuCondition {
    data class HasPermission(val permission: String) : MenuCondition
    data class MissingPermission(val permission: String) : MenuCondition
    data class PlaceholderEquals(val key: String, val value: String) : MenuCondition
    data class PlaceholderNotEquals(val key: String, val value: String) : MenuCondition
}

data class IconStyle(
    val materialName: String? = null,
    val texture: String? = null,
    val name: String? = null,
    val lore: List<String>? = null,
    val amount: Int? = null,
    val glow: Boolean? = null,
) {
    fun isEmpty(): Boolean {
        return materialName == null &&
            texture == null &&
            name == null &&
            lore == null &&
            amount == null &&
            glow == null
    }

    fun merged(override: IconStyle): IconStyle {
        return IconStyle(
            materialName = override.materialName ?: materialName,
            texture = override.texture ?: texture,
            name = override.name ?: name,
            lore = override.lore ?: lore,
            amount = override.amount ?: amount,
            glow = override.glow ?: glow,
        )
    }

    fun finalize(): IconDefinition {
        return IconDefinition(
            materialName = materialName,
            texture = texture,
            name = name,
            lore = lore ?: emptyList(),
            amount = amount ?: 1,
            glow = glow ?: false,
        )
    }
}

data class IconDefinition(
    val materialName: String?,
    val texture: String?,
    val name: String?,
    val lore: List<String>,
    val amount: Int,
    val glow: Boolean,
)

sealed interface MenuAction {
    data object Close : MenuAction
    data object Back : MenuAction
    data object Refresh : MenuAction
    data class Delay(val ticks: Long) : MenuAction
    data class Open(val menuId: String) : MenuAction
    data class Prompt(val promptId: String) : MenuAction
    data class Page(val operation: PageOperation, val regionId: String? = null) : MenuAction
    data class PlayerCommand(val command: String) : MenuAction
    data class ConsoleCommand(val command: String) : MenuAction
    data class Message(val text: String) : MenuAction
    data class Sound(val spec: SoundSpec) : MenuAction
}

enum class PageOperation {
    NEXT,
    PREVIOUS,
    FIRST,
    LAST,
    REFRESH,
}

data class SoundSpec(
    val soundName: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
)
