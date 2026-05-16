package cc.keer.amenu

import org.bukkit.configuration.file.FileConfiguration

data class PluginSettings(
    val commandOpenPermission: String?,
    val chatTimeoutSeconds: Long,
    val globalCancelKeywords: Set<String>,
    val fullInventoryAction: FullInventoryAction,
    val allowConsoleActions: Boolean,
    val warnUnsafeConsolePlaceholders: Boolean,
    val maxTakePoint: Double,
    val messages: Map<String, String>,
) {

    fun systemMessage(key: String, placeholders: Map<String, String> = emptyMap()): String {
        val prefix = messages["prefix"].orEmpty()
        val body = messages[key] ?: key
        return applyPlaceholders(prefix + body, placeholders)
    }

    companion object {
        fun from(config: FileConfiguration): PluginSettings {
            val messagesSection = config.getConfigurationSection("messages")
            val messages = messagesSection?.getKeys(false)
                ?.associateWith { key -> messagesSection.getString(key).orEmpty() }
                ?: emptyMap()
            return PluginSettings(
                commandOpenPermission = config.getString("command-open-permission")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: "amenu.open",
                chatTimeoutSeconds = config.getLong("chat-input.timeout-seconds", 45L).coerceAtLeast(5L),
                globalCancelKeywords = config.getStringList("chat-input.global-cancel-keywords")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet(),
                fullInventoryAction = FullInventoryAction.from(config.getString("give.full-inventory-action")),
                allowConsoleActions = config.getBoolean("security.allow-console-actions", true),
                warnUnsafeConsolePlaceholders = config.getBoolean("security.warn-unsafe-console-placeholders", true),
                maxTakePoint = config.getDouble("security.max-take-point", 100000.0).coerceAtLeast(0.0),
                messages = messages,
            )
        }

        private fun applyPlaceholders(text: String, placeholders: Map<String, String>): String {
            return placeholders.entries.fold(text) { current, (key, value) ->
                current.replace("{$key}", value)
            }
        }
    }
}

enum class FullInventoryAction {
    DROP,
    DENY,
    ;

    companion object {
        fun from(raw: String?): FullInventoryAction {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: DROP
        }
    }
}
