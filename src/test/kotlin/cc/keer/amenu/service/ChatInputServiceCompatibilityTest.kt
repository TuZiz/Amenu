package cc.keer.amenu.service

import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChatInputServiceCompatibilityTest : MenuPluginTestHarness() {

    @BeforeEach
    fun installPromptMenu() {
        writeMenu(
            "compat-runtime",
            """
            title: "Compat Runtime"
            layout:
              - "#########"
              - "#P#######"
              - "#########"
            fill:
              material: LIGHT_GRAY_STAINED_GLASS_PANE
              name: " "
            prompts:
              reusable:
                start:
                  - "Compat start"
                cancel-actions:
                  - "[message] Compat cancel"
                submit-actions:
                  - "[message] Compat {input}"
                  - "[player] record-input {input}"
            buttons:
              "P":
                material: PAPER
                name: "Prompt"
                click:
                  - "[prompt reusable]"
            """,
        )
    }

    @Test
    fun async_chat_completion_runs_submit_actions_after_scheduler_handoff() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "compat-runtime", "reusable", emptyMap())
        assertEquals("Compat start", nextPlainMessage())

        player.chat("Quartz")
        server.scheduler.waitAsyncEventsFinished()
        server.scheduler.performTicks(1L)
        server.scheduler.waitAsyncTasksFinished()

        assertEquals("Compat Quartz", nextPlainMessage())
        assertEquals(listOf("Tester|record-input|Quartz"), records)
    }

    @Test
    fun timeout_expiry_still_cleans_up_through_platform_scheduler() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "compat-runtime", "reusable", emptyMap())
        assertEquals("Compat start", nextPlainMessage())

        advanceTicks(5L * 20L)
        assertEquals("[AMenu] Prompt timed out.", nextPlainMessage())

        submitChat("late")
        assertTrue(records.isEmpty())
    }

    @Test
    fun prompt_replacement_keeps_only_the_latest_session_active() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "compat-runtime", "reusable", emptyMap())
        assertEquals("Compat start", nextPlainMessage())

        plugin.chatInputService.startPrompt(player, "compat-runtime", "reusable", emptyMap())
        assertEquals("[AMenu] Prompt replaced.", nextPlainMessage())
        assertEquals("Compat start", nextPlainMessage())

        submitChat("latest")
        assertEquals("Compat latest", nextPlainMessage())
        assertEquals(listOf("Tester|record-input|latest"), records)
    }

    @Test
    fun reload_and_shutdown_cancel_pending_prompt_tasks() {
        val records = mutableListOf<String>()
        registerRecordingCommand("record-input", records)

        plugin.chatInputService.startPrompt(player, "compat-runtime", "reusable", emptyMap())
        assertEquals("Compat start", nextPlainMessage())

        plugin.reloadPlugin()
        advanceTicks(5L * 20L)
        submitChat("after-reload")

        assertTrue(records.isEmpty())
        assertNull(nextPlainMessage())

        plugin.chatInputService.startPrompt(player, "compat-runtime", "reusable", emptyMap())
        assertEquals("Compat start", nextPlainMessage())

        plugin.onDisable()
        advanceTicks(5L * 20L)
        submitChat("after-shutdown")

        assertTrue(records.isEmpty())
        assertNull(nextPlainMessage())
    }
}
