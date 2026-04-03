package cc.keer.amenu.config

import cc.keer.amenu.AMenuPlugin
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Locale
import java.util.logging.Level
import kotlin.math.max

class MenuRepository(
    private val plugin: AMenuPlugin,
) {

    private val menus = linkedMapOf<String, MenuDefinition>()

    fun loadMenus() {
        menus.clear()
        val menuFolder = File(plugin.dataFolder, "menus")
        if (!menuFolder.exists()) {
            menuFolder.mkdirs()
        }

        menuFolder.listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
            ?.sortedBy { it.name.lowercase(Locale.ROOT) }
            ?.forEach(::loadMenuFile)
    }

    fun menu(menuId: String): MenuDefinition? = menus[menuId.lowercase(Locale.ROOT)]

    fun listMenuIds(): List<String> = menus.keys.toList()

    fun listBindings(): List<MenuBindingDefinition> = menus.values.flatMap(MenuDefinition::bindings)

    private fun loadMenuFile(file: File) {
        runCatching {
            val yaml = YamlConfiguration.loadConfiguration(file)
            val id = file.nameWithoutExtension.lowercase(Locale.ROOT)
            val parsedLayout = parseLayout(yaml)
            val templates = loadTemplates(section(yaml, "templates", "Templates"))
            val prompts = loadPrompts(section(yaml, "prompts", "Prompts")).toMutableMap()
            val fill = parseIconStyle(section(yaml, "fill", "Fill"))
            val pageRegions = loadPageRegions(section(yaml, "pages", "Pages"), templates, parsedLayout.symbols)
            val buttons = loadButtons(
                section(yaml, "buttons", "BUTTONS", "Buttons"),
                templates,
                prompts,
                fill,
                parsedLayout.symbols,
            )
            val bindings = loadBindings(id, section(yaml, "bindings", "Bindings"))
            MenuDefinition(
                id = id,
                title = stringValue(yaml, "title", "Title") ?: id,
                rows = parsedLayout.layout.size,
                layout = parsedLayout.layout,
                prompts = prompts.toMap(),
                buttons = buttons,
                pageRegions = pageRegions,
                bindings = bindings,
            )
        }.onSuccess { definition ->
            menus[definition.id] = definition
        }.onFailure { exception ->
            plugin.logger.log(Level.SEVERE, "Failed to load menu file ${file.name}: ${exception.message}", exception)
        }
    }

    private fun parseLayout(section: ConfigurationSection): ParsedLayout {
        val rawShape = stringList(section, "shape", "Shape")
        if (rawShape.isNotEmpty()) {
            return parseShapeLayout(rawShape)
        }

        val rawLayout = stringList(section, "layout", "Layout")
        val layout = normalizeLayout(rawLayout, section.getInt("rows", rawLayout.size.coerceAtLeast(3)))
        return ParsedLayout(layout, emptyMap())
    }

    private fun normalizeLayout(rawLayout: List<String>, requestedRows: Int): List<String> {
        val rows = requestedRows.coerceIn(1, 6)
        return (0 until rows).map { index ->
            rawLayout.getOrNull(index).orEmpty().padEnd(9, ' ').take(9)
        }
    }

    private fun parseShapeLayout(rawShape: List<String>): ParsedLayout {
        val symbols = linkedMapOf<String, Char>()
        val normalizedRows = rawShape.take(6).map { row ->
            val slots = tokenizeShapeRow(row).map { token ->
                when {
                    token.length == 1 -> token.first()
                    else -> symbols.getOrPut(token) { nextSyntheticSymbol(symbols.size) }
                }
            }
            require(slots.size <= 9) { "Shape row '$row' expands to ${slots.size} slots, but at most 9 are allowed" }
            slots.joinToString(separator = "").padEnd(9, ' ')
        }
        return ParsedLayout(normalizeLayout(normalizedRows, normalizedRows.size.coerceAtLeast(1)), symbols.toMap())
    }

    private fun tokenizeShapeRow(raw: String): List<String> {
        val tokens = mutableListOf<String>()
        var index = 0
        while (index < raw.length) {
            if (raw[index] == '`') {
                val end = raw.indexOf('`', index + 1)
                require(end > index) { "Shape row '$raw' contains an unterminated backtick token" }
                tokens += raw.substring(index + 1, end)
                index = end + 1
                continue
            }
            tokens += raw[index].toString()
            index++
        }
        return tokens
    }

    private fun loadTemplates(section: ConfigurationSection?): Map<String, IconStyle> {
        if (section == null) {
            return emptyMap()
        }
        return section.getKeys(false).associateWith { key ->
            parseIconStyle(section.getConfigurationSection(key))
        }
    }

    private fun loadPageRegions(
        section: ConfigurationSection?,
        templates: Map<String, IconStyle>,
        shapeSymbols: Map<String, Char>,
    ): Map<String, PageRegionDefinition> {
        if (section == null) {
            return emptyMap()
        }
        return section.getKeys(false).associateWith { key ->
            val regionSection = section.getConfigurationSection(key)
                ?: error("Page region section $key is invalid")
            parsePageRegion(key, regionSection, templates, shapeSymbols)
        }
    }

    private fun loadPrompts(section: ConfigurationSection?): Map<String, PromptDefinition> {
        if (section == null) {
            return emptyMap()
        }
        return section.getKeys(false).associateWith { key ->
            val promptSection = section.getConfigurationSection(key) ?: error("Prompt section $key is invalid")
            parsePrompt(key, promptSection)
        }
    }

    private fun loadButtons(
        section: ConfigurationSection?,
        templates: Map<String, IconStyle>,
        prompts: MutableMap<String, PromptDefinition>,
        fill: IconStyle,
        shapeSymbols: Map<String, Char>,
    ): Map<Char, ButtonDefinition> {
        if (section == null) {
            return buildDefaultFill(fill)
        }
        val buttons = section.getKeys(false).associate { key ->
            val buttonSection = section.getConfigurationSection(key)
                ?: error("Button section $key is invalid")
            val symbol = resolveLayoutSymbol(key, shapeSymbols, "button")
            val template = buttonSection.getString("template")?.let { templateId ->
                templates[templateId] ?: error("Unknown template '$templateId' in button '$key'")
            } ?: IconStyle()
            val baseStyle = template.merged(parseIconStyle(buttonSection))
            val icon = baseStyle.finalize()
            val inlinePromptId = buttonSection.getConfigurationSection("input")?.let { inputSection ->
                val promptId = "button-${normalizeTokenKey(key)}"
                prompts[promptId] = parsePrompt(promptId, inputSection)
                promptId
            }
            val actions = parseActions(actionStrings(buttonSection)).toMutableList()
            if (inlinePromptId != null && actions.none { it is MenuAction.Prompt && it.promptId == inlinePromptId }) {
                actions += MenuAction.Prompt(inlinePromptId)
            }
            symbol to ButtonDefinition(
                symbol = symbol,
                icon = icon,
                actions = actions,
                permission = buttonSection.getString("permission")?.takeIf { it.isNotBlank() },
                visiblePermission = buttonSection.getString("visible-permission")?.takeIf { it.isNotBlank() },
                denyActions = parseActions(stringList(buttonSection, "deny-actions", "deny")),
                conditions = parseConditions(buttonSection.getConfigurationSection("conditions")),
                states = loadButtonStates(buttonSection.getConfigurationSection("states"), baseStyle),
            )
        }
        if (!buttons.containsKey('#') && !fill.isEmpty()) {
            return buttons + ('#' to ButtonDefinition('#', fill.finalize(), emptyList(), null, null, emptyList(), emptyList(), emptyList()))
        }
        return buttons
    }

    private fun loadButtonStates(
        section: ConfigurationSection?,
        baseStyle: IconStyle,
    ): List<ButtonStateDefinition> {
        if (section == null) {
            return emptyList()
        }
        return section.getKeys(false).map { key ->
            val stateSection = section.getConfigurationSection(key)
                ?: error("Button state '$key' is invalid")
            val hasIconOverride = listOf("material", "mats", "head", "texture", "name", "lore", "amount", "glow", "shiny", "display")
                .any(stateSection::contains)
            val actionValues = actionStrings(stateSection)
            ButtonStateDefinition(
                id = key,
                conditions = parseConditions(stateSection.getConfigurationSection("conditions")),
                icon = if (hasIconOverride) baseStyle.merged(parseIconStyle(stateSection)).finalize() else null,
                actions = if (actionValues.isEmpty()) null else parseActions(actionValues),
                permission = if (stateSection.contains("permission")) stateSection.getString("permission")?.takeIf { it.isNotBlank() } else null,
                visiblePermission = if (stateSection.contains("visible-permission")) {
                    stateSection.getString("visible-permission")?.takeIf { it.isNotBlank() }
                } else {
                    null
                },
                denyActions = if (stateSection.contains("deny-actions") || stateSection.contains("deny")) {
                    parseActions(stringList(stateSection, "deny-actions", "deny"))
                } else {
                    null
                },
            )
        }
    }

    private fun parseIconStyle(section: ConfigurationSection?): IconStyle {
        if (section == null) {
            return IconStyle()
        }
        val display = section.getConfigurationSection("display")
        return IconStyle(
            materialName = stringValue(section, "material", "mats") ?: stringValue(display, "material", "mats"),
            texture = stringValue(section, "texture", "head") ?: stringValue(display, "texture", "head"),
            name = scalarOrFirstList(section, "name") ?: scalarOrFirstList(display, "name"),
            lore = listValue(section, "lore") ?: listValue(display, "lore"),
            amount = when {
                section.contains("amount") -> section.getInt("amount")
                display?.contains("amount") == true -> display.getInt("amount")
                else -> null
            },
            glow = when {
                section.contains("glow") -> section.getBoolean("glow")
                section.contains("shiny") -> section.getBoolean("shiny")
                display?.contains("glow") == true -> display.getBoolean("glow")
                display?.contains("shiny") == true -> display.getBoolean("shiny")
                else -> null
            },
        )
    }

    private fun parsePageRegion(
        id: String,
        section: ConfigurationSection,
        templates: Map<String, IconStyle>,
        shapeSymbols: Map<String, Char>,
    ): PageRegionDefinition {
        val symbolText = section.getString("symbol")?.trim().orEmpty()
        val entriesSection = section.getConfigurationSection("entries")
            ?: error("Page region '$id' must declare entries")
        val entries = entriesSection.getKeys(false).map { key ->
            val entrySection = entriesSection.getConfigurationSection(key)
                ?: error("Entry '$key' in page region '$id' is invalid")
            parsePageEntry(key, entrySection, templates)
        }
        val asyncDelayTicks = max(0L, section.getLong("async-delay", section.getLong("async-delay-ticks", 0L)))
        val provider = parseProviderDefinition(section, entries.isNotEmpty(), asyncDelayTicks)
        val loadingIcon = defaultLoadingIcon(id).merged(parseIconStyle(section.getConfigurationSection("loading"))).finalize()
        val emptyIcon = defaultEmptyIcon(id).merged(parseIconStyle(section.getConfigurationSection("empty"))).finalize()
        val errorIcon = defaultErrorIcon(id).merged(parseIconStyle(section.getConfigurationSection("error"))).finalize()
        return PageRegionDefinition(
            id = id,
            symbol = resolveLayoutSymbol(symbolText, shapeSymbols, "page region '$id'"),
            entries = entries,
            provider = provider,
            surface = ProviderSurfaceDefinition(
                loading = loadingIcon,
                empty = emptyIcon,
                error = errorIcon,
            ),
            loadingIcon = loadingIcon,
            emptyIcon = emptyIcon,
            errorIcon = errorIcon,
            asyncDelayTicks = asyncDelayTicks,
        )
    }

    private fun parseProviderDefinition(
        section: ConfigurationSection,
        hasEntries: Boolean,
        asyncDelayTicks: Long,
    ): ProviderDefinition? {
        val providerSection = section.getConfigurationSection("provider")
        val update = parseProviderUpdate(section.getConfigurationSection("update"))
        if (providerSection == null) {
            return if (hasEntries || asyncDelayTicks > 0L || update != null) {
                ProviderDefinition(
                    type = if (asyncDelayTicks > 0L) "legacy-static-delayed" else "legacy-static",
                    params = emptyMap(),
                    cache = null,
                    update = update,
                )
            } else {
                null
            }
        }

        val type = providerSection.getString("type")?.trim().orEmpty()
        require(type.isNotBlank()) { "Page region provider.type must not be blank" }
        val paramsSection = providerSection.getConfigurationSection("params")
        return ProviderDefinition(
            type = type,
            params = paramsSection?.getKeys(false)?.associateWith { key ->
                paramsSection.getString(key).orEmpty()
            } ?: emptyMap(),
            cache = parseProviderCache(providerSection.getConfigurationSection("cache")),
            update = update,
        )
    }

    private fun parseProviderCache(section: ConfigurationSection?): ProviderCacheDefinition? {
        if (section == null) {
            return null
        }
        val ttl = section.getLong("ttl").takeIf { it > 0L } ?: return null
        return ProviderCacheDefinition(ttl)
    }

    private fun parseProviderUpdate(section: ConfigurationSection?): ProviderUpdateDefinition? {
        if (section == null) {
            return null
        }
        val interval = section.getLong("interval").takeIf { it > 0L } ?: return null
        return ProviderUpdateDefinition(interval)
    }

    private fun parsePageEntry(
        id: String,
        section: ConfigurationSection,
        templates: Map<String, IconStyle>,
    ): PageEntryDefinition {
        val template = section.getString("template")?.let { templateId ->
            templates[templateId] ?: error("Unknown template '$templateId' in page entry '$id'")
        } ?: IconStyle()
        val icon = template.merged(parseIconStyle(section)).finalize()
        return PageEntryDefinition(
            id = id,
            icon = icon,
            actions = parseActions(actionStrings(section)),
            placeholders = section.getConfigurationSection("placeholders")
                ?.getKeys(false)
                ?.associateWith { key -> section.getConfigurationSection("placeholders")!!.getString(key).orEmpty() }
                ?: emptyMap(),
        )
    }

    private fun loadBindings(
        menuId: String,
        section: ConfigurationSection?,
    ): List<MenuBindingDefinition> {
        if (section == null) {
            return emptyList()
        }

        val bindings = mutableListOf<MenuBindingDefinition>()
        val itemSpecs = stringList(section, "item", "Item")
        if (itemSpecs.isNotEmpty()) {
            bindings += itemSpecs.map { spec -> parseShorthandItemBinding(menuId, spec) }
        }

        val commandSpecs = stringList(section, "command", "Command")
        if (commandSpecs.isNotEmpty()) {
            plugin.logger.warning("Bindings.command 暂未实现动态指令注册，请继续使用 /amenu open <menuId> 作为命令入口。")
        }

        section.getKeys(false)
            .filterNot { key -> key.equals("item", true) || key.equals("command", true) }
            .forEach { key ->
                val bindingSection = section.getConfigurationSection(key)
                    ?: error("Binding '$key' is invalid")
                bindings += parseBinding(menuId, key, bindingSection)
            }

        return bindings
    }

    private fun parseShorthandItemBinding(menuId: String, raw: String): MenuBindingDefinition {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "Bindings.Item entries must not be blank" }

        val attributes = trimmed.split(WHITESPACE_REGEX)
            .filter { token -> token.isNotBlank() }
            .mapNotNull { token ->
                val separator = token.indexOf(':')
                if (separator <= 0) {
                    null
                } else {
                    token.substring(0, separator).lowercase(Locale.ROOT) to token.substring(separator + 1)
                }
            }
            .toMap()

        val material = attributes["material"] ?: attributes["mat"] ?: trimmed
        val id = attributes["id"] ?: material.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-")
        return MenuBindingDefinition(
            id = id,
            menuId = menuId,
            type = MenuBindingType.ITEM,
            materialName = material,
            name = attributes["name"],
            actions = linkedSetOf(
                MenuBindingAction.RIGHT_CLICK,
                MenuBindingAction.RIGHT_CLICK_AIR,
                MenuBindingAction.RIGHT_CLICK_BLOCK,
            ),
            permission = attributes["permission"],
            placeholders = emptyMap(),
            conditions = emptyList(),
        )
    }

    private fun parseBinding(
        menuId: String,
        id: String,
        section: ConfigurationSection,
    ): MenuBindingDefinition {
        val type = runCatching {
            MenuBindingType.valueOf(section.getString("type", "ITEM").orEmpty().trim().uppercase(Locale.ROOT))
        }.getOrElse {
            error("Binding '$id' declares an unknown type")
        }
        val actions = (section.getStringList("actions") + section.getStringList("trigger-actions"))
            .ifEmpty { listOf("RIGHT_CLICK", "RIGHT_CLICK_AIR", "RIGHT_CLICK_BLOCK") }
            .mapTo(linkedSetOf(), ::parseBindingAction)
        val placeholdersSection = section.getConfigurationSection("placeholders")
        return MenuBindingDefinition(
            id = id,
            menuId = menuId,
            type = type,
            materialName = stringValue(section, "material", "mats"),
            name = scalarOrFirstList(section, "name"),
            actions = actions,
            permission = section.getString("permission")?.takeIf { it.isNotBlank() },
            placeholders = placeholdersSection?.getKeys(false)?.associateWith { key ->
                placeholdersSection.getString(key).orEmpty()
            } ?: emptyMap(),
            conditions = parseConditions(section.getConfigurationSection("conditions")),
        )
    }

    private fun parsePrompt(id: String, section: ConfigurationSection): PromptDefinition {
        val type = runCatching {
            PromptType.valueOf(section.getString("type", "CHAT").orEmpty().trim().uppercase(Locale.ROOT))
        }.getOrElse {
            error("Prompt '$id' declares an unknown type")
        }
        return PromptDefinition(
            id = id,
            type = type,
            startMessages = stringList(section, "start"),
            cancelKeywords = stringList(section, "cancel-keywords")
                .map { it.trim().lowercase(Locale.ROOT) }
                .filter { it.isNotEmpty() }
                .toSet(),
            timeoutSeconds = section.getLong("timeout-seconds").takeIf { it > 0L },
            submitActions = parseActions(stringList(section, "submit-actions", "submit")),
            cancelActions = parseActions(stringList(section, "cancel-actions", "cancel")),
            invalidActions = parseActions(stringList(section, "invalid-actions", "invalid")),
            validation = parsePromptValidation(section.getConfigurationSection("validation")),
            signLines = stringList(section, "sign-lines", "lines"),
            anvilTitle = section.getString("anvil-title") ?: section.getString("title"),
            anvilText = section.getString("initial-text") ?: section.getString("text"),
        )
    }

    private fun parsePromptValidation(section: ConfigurationSection?): PromptValidation? {
        if (section == null) {
            return null
        }
        val equals = section.getString("equals")?.takeIf { it.isNotBlank() }
        val regexText = section.getString("matches")?.takeIf { it.isNotBlank() }
        val ignoreCase = section.getBoolean("ignore-case", true)
        val matches = regexText?.let { pattern ->
            runCatching {
                Regex(
                    pattern,
                    if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet(),
                )
            }.getOrElse { error("Prompt validation regex '$pattern' is invalid: ${it.message}") }
        }
        if (equals == null && matches == null) {
            return null
        }
        return PromptValidation(
            equals = equals,
            matches = matches,
            ignoreCase = ignoreCase,
        )
    }

    private fun buildDefaultFill(fill: IconStyle): Map<Char, ButtonDefinition> {
        if (fill.isEmpty()) {
            return emptyMap()
        }
        return mapOf('#' to ButtonDefinition('#', fill.finalize(), emptyList(), null, null, emptyList(), emptyList(), emptyList()))
    }

    private fun actionStrings(section: ConfigurationSection): List<String> {
        val nestedActions = section.getConfigurationSection("actions")
        return buildList {
            addAll(stringList(section, "actions", "click", "run"))
            if (nestedActions != null) {
                addAll(stringList(nestedActions, "all"))
                addAll(stringList(nestedActions, "left"))
                addAll(stringList(nestedActions, "right"))
                addAll(stringList(nestedActions, "shift-left"))
                addAll(stringList(nestedActions, "shift-right"))
            }
        }
    }

    private fun parseActions(rawActions: List<String>): List<MenuAction> {
        return rawActions.map { raw ->
            val action = raw.trim()
            val descriptor = parseActionDescriptor(action)
            if (descriptor == null) {
                MenuAction.PlayerCommand(action)
            } else {
                val type = descriptor.first
                val value = descriptor.second
                when (type) {
                    "close" -> MenuAction.Close
                    "back" -> MenuAction.Back
                    "refresh" -> MenuAction.Refresh
                    "delay" -> MenuAction.Delay(requireArgument(type, value).toLongOrNull() ?: error("Action '$type' requires a numeric value"))
                    "open" -> MenuAction.Open(requireArgument(type, value))
                    "prompt" -> MenuAction.Prompt(requireArgument(type, value))
                    "page" -> parsePageAction(value)
                    "player" -> MenuAction.PlayerCommand(requireArgument(type, value))
                    "command" -> MenuAction.PlayerCommand(requireArgument(type, value))
                    "console" -> MenuAction.ConsoleCommand(requireArgument(type, value))
                    "message" -> MenuAction.Message(requireArgument(type, value))
                    "sound" -> MenuAction.Sound(parseSoundSpec(requireArgument(type, value)))
                    else -> error("Unknown action type '$type'")
                }
            }
        }
    }

    private fun parseActionDescriptor(action: String): Pair<String, String>? {
        if (!action.startsWith("[") || !action.contains(']')) {
            val colonIndex = action.indexOf(':')
            if (colonIndex > 0) {
                val type = action.substring(0, colonIndex).trim().lowercase(Locale.ROOT)
                val value = action.substring(colonIndex + 1).trim()
                val normalizedType = when (type) {
                    "menu" -> "open"
                    else -> type
                }
                return normalizedType to value
            }
            return when (action.trim().lowercase(Locale.ROOT)) {
                "close" -> "close" to ""
                "back" -> "back" to ""
                "refresh" -> "refresh" to ""
                else -> null
            }
        }
        val closingIndex = action.indexOf(']')
        if (closingIndex <= 1) {
            return null
        }

        val inside = action.substring(1, closingIndex).trim()
        if (inside.isBlank()) {
            return null
        }

        val outside = action.substring(closingIndex + 1).trim()
        val parts = inside.split(WHITESPACE_REGEX, limit = 2)
        val type = parts[0].lowercase(Locale.ROOT)
        val inlineValue = parts.getOrNull(1).orEmpty()
        val value = outside.ifBlank { inlineValue }.trim()
        return type to value
    }

    private fun parseSoundSpec(raw: String): SoundSpec {
        val parts = raw.split(':')
        return SoundSpec(
            soundName = parts[0],
            volume = parts.getOrNull(1)?.toFloatOrNull() ?: 1f,
            pitch = parts.getOrNull(2)?.toFloatOrNull() ?: 1f,
        )
    }

    private fun parsePageAction(raw: String): MenuAction.Page {
        val parts = raw.split(WHITESPACE_REGEX).filter { token -> token.isNotBlank() }
        val operationToken = parts.firstOrNull()?.lowercase(Locale.ROOT).orEmpty()
        val operation = when (operationToken) {
            "next" -> PageOperation.NEXT
            "prev", "previous" -> PageOperation.PREVIOUS
            "first" -> PageOperation.FIRST
            "last" -> PageOperation.LAST
            "refresh", "reload" -> PageOperation.REFRESH
            else -> error("Unknown page operation '$operationToken'")
        }
        return MenuAction.Page(operation, parts.getOrNull(1))
    }

    private fun parseBindingAction(raw: String): MenuBindingAction {
        return when (raw.trim().uppercase(Locale.ROOT)) {
            "RIGHT_CLICK" -> MenuBindingAction.RIGHT_CLICK
            "LEFT_CLICK" -> MenuBindingAction.LEFT_CLICK
            "RIGHT_CLICK_AIR" -> MenuBindingAction.RIGHT_CLICK_AIR
            "RIGHT_CLICK_BLOCK" -> MenuBindingAction.RIGHT_CLICK_BLOCK
            "LEFT_CLICK_AIR" -> MenuBindingAction.LEFT_CLICK_AIR
            "LEFT_CLICK_BLOCK" -> MenuBindingAction.LEFT_CLICK_BLOCK
            else -> error("Unknown binding action '$raw'")
        }
    }

    private fun parseConditions(section: ConfigurationSection?): List<MenuCondition> {
        if (section == null) {
            return emptyList()
        }
        val conditions = mutableListOf<MenuCondition>()
        section.getString("has-permission")?.takeIf { it.isNotBlank() }?.let {
            conditions += MenuCondition.HasPermission(it)
        }
        section.getString("missing-permission")?.takeIf { it.isNotBlank() }?.let {
            conditions += MenuCondition.MissingPermission(it)
        }
        section.getConfigurationSection("placeholder-equals")?.let { placeholders ->
            placeholders.getKeys(false).forEach { key ->
                conditions += MenuCondition.PlaceholderEquals(key, placeholders.getString(key).orEmpty())
            }
        }
        section.getConfigurationSection("placeholder-not-equals")?.let { placeholders ->
            placeholders.getKeys(false).forEach { key ->
                conditions += MenuCondition.PlaceholderNotEquals(key, placeholders.getString(key).orEmpty())
            }
        }
        return conditions
    }

    private fun requireArgument(type: String, value: String): String {
        require(value.isNotBlank()) { "Action '$type' requires a value" }
        return value
    }

    private fun defaultLoadingIcon(regionId: String): IconStyle {
        return IconStyle(
            materialName = "CLOCK",
            name = "<yellow><bold>Loading ${regionId.replace('-', ' ')}</bold></yellow>",
            lore = listOf("<gray>Async showcase data is still arriving.</gray>"),
        )
    }

    private fun defaultEmptyIcon(regionId: String): IconStyle {
        return IconStyle(
            materialName = "BARRIER",
            name = "<red><bold>No entries in ${regionId.replace('-', ' ')}</bold></red>",
            lore = listOf("<gray>This page region is currently empty.</gray>"),
        )
    }

    private fun defaultErrorIcon(regionId: String): IconStyle {
        return IconStyle(
            materialName = "RED_STAINED_GLASS_PANE",
            name = "<red><bold>Failed to load ${regionId.replace('-', ' ')}</bold></red>",
            lore = listOf("<gray>The provider returned an error.</gray>"),
        )
    }

    private fun resolveLayoutSymbol(key: String, shapeSymbols: Map<String, Char>, label: String): Char {
        shapeSymbols[key]?.let { return it }
        require(key.length == 1) { "$label '$key' must be a one-character symbol or appear in Shape using backticks" }
        return key.first()
    }

    private fun normalizeTokenKey(raw: String): String {
        return raw.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').ifBlank { "token" }
    }

    private fun nextSyntheticSymbol(index: Int): Char {
        val codePoint = 0xE000 + index
        require(codePoint <= 0xF8FF) { "Too many backtick tokens were declared in Shape" }
        return codePoint.toChar()
    }

    private fun section(root: ConfigurationSection?, vararg keys: String): ConfigurationSection? {
        if (root == null) {
            return null
        }
        return keys.firstNotNullOfOrNull { key -> root.getConfigurationSection(key) }
    }

    private fun stringValue(root: ConfigurationSection?, vararg keys: String): String? {
        if (root == null) {
            return null
        }
        return keys.firstNotNullOfOrNull { key ->
            if (root.contains(key)) root.getString(key) else null
        }
    }

    private fun scalarOrFirstList(root: ConfigurationSection?, vararg keys: String): String? {
        if (root == null) {
            return null
        }
        return keys.firstNotNullOfOrNull { key ->
            when {
                root.isString(key) -> root.getString(key)
                root.isList(key) -> root.getStringList(key).firstOrNull()
                else -> null
            }
        }
    }

    private fun listValue(root: ConfigurationSection?, vararg keys: String): List<String>? {
        if (root == null) {
            return null
        }
        return keys.firstNotNullOfOrNull { key ->
            when {
                root.isList(key) -> root.getStringList(key)
                root.isString(key) -> listOf(root.getString(key).orEmpty())
                else -> null
            }
        }
    }

    private fun stringList(root: ConfigurationSection?, vararg keys: String): List<String> {
        return listValue(root, *keys) ?: emptyList()
    }

    private companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}

private data class ParsedLayout(
    val layout: List<String>,
    val symbols: Map<String, Char>,
)
