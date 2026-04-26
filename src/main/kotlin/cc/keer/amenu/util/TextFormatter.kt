package cc.keer.amenu.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

object TextFormatter {

    private val miniMessage = MiniMessage.miniMessage()
    private val legacyInput = LegacyComponentSerializer.legacyAmpersand()
    private val legacyOutput = LegacyComponentSerializer.legacySection()
    private val plainOutput = PlainTextComponentSerializer.plainText()

    fun component(text: String): Component {
        val component = if (text.contains('<') && text.contains('>')) {
            miniMessage.deserialize(text)
        } else {
            legacyInput.deserialize(text)
        }
        return component.decoration(TextDecoration.ITALIC, false)
    }

    fun legacyString(component: Component): String {
        return legacyOutput.serialize(component)
    }

    fun plainString(component: Component): String {
        return plainOutput.serialize(component)
    }
}
