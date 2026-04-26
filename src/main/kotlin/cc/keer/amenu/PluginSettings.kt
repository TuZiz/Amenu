package cc.keer.amenu

import org.bukkit.configuration.file.FileConfiguration

data class PluginSettings(
    val defaultMenuId: String,
    val commandOpenPermission: String?,
    val chatTimeoutSeconds: Long,
    val globalCancelKeywords: Set<String>,
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
                defaultMenuId = config.getString("default-menu", "menu").orEmpty(),
                commandOpenPermission = config.getString("command-open-permission")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: "amenu.open",
                chatTimeoutSeconds = config.getLong("chat-input.timeout-seconds", 45L).coerceAtLeast(5L),
                globalCancelKeywords = config.getStringList("chat-input.global-cancel-keywords")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                    .toSet(),
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
