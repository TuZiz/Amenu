package cc.keer.amenu.service

import org.bukkit.entity.Player

class PlaceholderPipeline(
    private val bridge: PlaceholderApiBridge = PlaceholderApiBridge.disabled(),
) {

    fun render(
        player: Player,
        text: String,
        placeholders: Map<String, String> = emptyMap(),
    ): String {
        val builtIns = mapOf("player" to player.name)
        val resolved = (builtIns + placeholders).entries.fold(text) { current, (key, value) ->
            current.replace("{$key}", value)
        }
        return if (bridge.isAvailable()) {
            bridge.render(player, resolved)
        } else {
            resolved
        }
    }

    fun renderAll(
        player: Player,
        texts: List<String>,
        placeholders: Map<String, String> = emptyMap(),
    ): List<String> {
        return texts.map { text -> render(player, text, placeholders) }
    }

    fun matchesValue(
        player: Player,
        key: String,
        expected: String,
        placeholders: Map<String, String>,
    ): Boolean {
        val renderedExpected = render(player, expected, placeholders)
        return if (looksLikePlaceholderExpression(key)) {
            render(player, key, placeholders) == renderedExpected
        } else {
            placeholders[key] == renderedExpected
        }
    }

    private fun looksLikePlaceholderExpression(value: String): Boolean {
        return (value.contains('{') && value.contains('}')) || value.contains('%')
    }
}
