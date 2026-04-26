package cc.keer.amenu.service

import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChatInputServiceTest : MenuPluginTestHarness() {

    @BeforeEach
    fun installPromptMenu() {
        writeMenu(
            "prompt-lab",
            """
            title: "Prompt Lab"
            layout:
              - "#########"
              - "#R#I#####"
              - "#########"
            fill:
              material: LIGHT_GRAY_STAINED_GLASS_PANE
              name: " "
            prompts:
              reusable:
                start:
                  - "Start reusable"
                cancel-actions:
                  - "[message] Cancel reusable"
                submit-actions:
                  - "[message] Submit {input}"
                  - "[player] record-input {input}"
              guarded:
                timeout-seconds: 1
                start:
                  - "Start guarded"
                invalid-actions:
                  - "[message] Guarded invalid"
                cancel-actions:
                  - "[message] Guarded cancel"
                submit-actions:
                  - "[message] Guarded {input}"
                  - "[player] record-input guarded-{input}"
                validation:
                  equals: "ok"
            buttons:
              "R":
                material: BOOK
                name: "Reusable"
                click:
                  - "[close]"
                  - "[prompt reusable]"
              "G":
                material: NAME_TAG
                name: "Guarded"
                click:
                  - "[close]"
                  - "[prompt guarded]"
              "I":
                material: PAPER
                name: "Inline"
                input:
                  start:
                    - "Start inline"
                  cancel:
                    - "[message] Cancel inline"
                  submit:
                    - "[message] Inline {input}"
                    - "[player] record-input inline-{input}"
            """,
        )
    }

    @Test
    fun reusable_prompt_and_inline_input_both_start_sessions() {
        val menu = requireNotNull(plugin.menuRepository.menu("prompt-lab"))

        plugin.chatInputService.startPrompt(player, "prompt-lab", "reusable", emptyMap())
        assertNull(currentMenuId())
        assertEquals("Start reusable", nextPlainMessage())

        assertTrue(menu.prompts.containsKey("button-i"))
        plugin.chatInputService.startPrompt(player, "prompt-lab", "button-i", emptyMap())
        assertNull(currentMenuId())
        assertEquals("[AMenu] 你原有的输入流程已被新的输入请求替换。", nextPlainMessage())
        assertEquals("Start inline", nextPlainMessage())
    }

    @Test
    fun submit_injects_input_into_follow_up_actions() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "prompt-lab", "reusable", emptyMap())
        assertEquals("Start reusable", nextPlainMessage())

        submitChat("Notch")

        assertEquals("Submit Notch", nextPlainMessage())
        assertEquals(listOf("Tester|record-input|Notch"), records)
    }

    @Test
    fun cancel_and_replace_paths_emit_expected_feedback() {
        plugin.chatInputService.startPrompt(player, "prompt-lab", "reusable", emptyMap())
        assertEquals("Start reusable", nextPlainMessage())

        submitChat("cancel")
        assertEquals("Cancel reusable", nextPlainMessage())

        plugin.chatInputService.startPrompt(player, "prompt-lab", "reusable", emptyMap())
        assertEquals("Start reusable", nextPlainMessage())

        plugin.chatInputService.startPrompt(player, "prompt-lab", "button-i", emptyMap())

        assertEquals("[AMenu] 你原有的输入流程已被新的输入请求替换。", nextPlainMessage())
        assertEquals("Start inline", nextPlainMessage())

        submitChat("cancel")
        assertEquals("Cancel inline", nextPlainMessage())
    }

    @Test
    fun timeout_cleans_up_session_and_notifies_player() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "prompt-lab", "button-i", emptyMap())
        assertEquals("Start inline", nextPlainMessage())

        advanceTicks(5L * 20L)
        assertEquals("[AMenu] 输入流程已超时，请重新打开菜单后再试一次。", nextPlainMessage())

        submitChat("late")

        assertTrue(records.isEmpty())
        assertNull(nextPlainMessage())
    }

    @Test
    fun invalid_input_and_prompt_timeout_follow_guard_rules() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "prompt-lab", "guarded", emptyMap())
        assertEquals("Start guarded", nextPlainMessage())

        submitChat("wrong")
        assertEquals("Guarded invalid", nextPlainMessage())
        assertTrue(records.isEmpty())

        plugin.chatInputService.startPrompt(player, "prompt-lab", "guarded", emptyMap())
        assertEquals("Start guarded", nextPlainMessage())

        submitChat("ok")
        assertEquals("Guarded ok", nextPlainMessage())
        assertEquals(listOf("Tester|record-input|guarded-ok"), records)

        plugin.chatInputService.startPrompt(player, "prompt-lab", "guarded", emptyMap())
        assertEquals("Start guarded", nextPlainMessage())

        advanceTicks(20L)
        assertEquals("[AMenu] 输入流程已超时，请重新打开菜单后再试一次。", nextPlainMessage())
    }
}
