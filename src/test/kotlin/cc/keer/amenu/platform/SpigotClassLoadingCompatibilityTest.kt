package cc.keer.amenu.platform

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SpigotClassLoadingCompatibilityTest {

    @Test
    fun main_classes_do_not_reference_paper_or_folia_only_types() {
        val classesDir = Path.of("target", "classes")
        val forbidden = listOf(
            "io/papermc/paper/event/player/AsyncChatEvent",
            "io/papermc/paper/threadedregions/scheduler/ScheduledTask",
        )
        val offenders = Files.walk(classesDir).use { paths ->
            paths
                .filter { path -> path.toString().endsWith(".class") }
                .flatMap { path ->
                    val text = Files.readAllBytes(path).toString(Charsets.ISO_8859_1)
                    forbidden
                        .filter { marker -> text.contains(marker) }
                        .map { marker -> "${classesDir.relativize(path)} -> $marker" }
                        .stream()
                }
                .toList()
        }

        assertFalse(offenders.isNotEmpty(), "Forbidden Paper/Folia hard references: $offenders")
    }
}
