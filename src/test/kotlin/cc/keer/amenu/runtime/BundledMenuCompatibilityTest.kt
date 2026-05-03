package cc.keer.amenu.runtime

import cc.keer.amenu.support.MenuPluginTestHarness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BundledMenuCompatibilityTest : MenuPluginTestHarness() {

    private fun grantOpenPermission() {
        grantPermission("amenu.open")
    }

    private fun copyTemplateToMenu(template: File): String {
        val menu = File(plugin.dataFolder, "menus/${template.name}")
        menu.parentFile.mkdirs()
        template.copyTo(menu, overwrite = true)
        return template.nameWithoutExtension.lowercase()
    }

    private fun installFeatureTemplatesAsMenus(): Set<String> {
        val copiedIds = releasedTemplateFiles().map(::copyTemplateToMenu).toSet()
        val report = plugin.menuRepository.loadMenus()
        assertTrue(report.successful)
        return copiedIds
    }

    private fun releasedTemplateFiles(): List<File> {
        return File(plugin.dataFolder, "templates")
            .listFiles { file -> file.isFile && file.extension.equals("yml", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    @Test
    fun bundled_feature_templates_are_released_without_auto_seeded_menu() {
        val templates = releasedTemplateFiles()
        val menuIds = plugin.menuRepository.listMenuIds().toSet()

        assertTrue(menuIds.isEmpty())
        assertFalse(File(plugin.dataFolder, "menus/menu.yml").isFile)
        assertTrue(templates.isNotEmpty())
        templates.forEach { template ->
            assertFalse(menuIds.contains(template.nameWithoutExtension.lowercase()))
        }
        assertFalse(File(plugin.dataFolder, "menus/templates").exists())

        grantOpenPermission()
        assertTrue(server.dispatchCommand(player, "amenu"))
        assertNull(currentMenuId())
        assertNotNull(nextPlainMessage())
    }

    @Test
    fun copied_feature_templates_load_as_normal_menus() {
        val copiedIds = installFeatureTemplatesAsMenus()
        val menuIds = plugin.menuRepository.listMenuIds().toSet()

        assertTrue(menuIds.containsAll(copiedIds))
        copiedIds.forEach { menuId ->
            val menu = plugin.menuRepository.menu(menuId)
            assertNotNull(menu)
            plugin.menuService.openMenu(player, menuId)
            assertEquals(menuId, currentMenuId())
        }
    }

    @Test
    fun feature_templates_remain_inactive_until_copied_to_menus() {
        val template = releasedTemplateFiles().first()
        val menuId = template.nameWithoutExtension.lowercase()

        grantOpenPermission()
        assertTrue(server.dispatchCommand(player, "amenu"))
        assertNull(currentMenuId())
        assertNotNull(nextPlainMessage())

        copyTemplateToMenu(template)
        val report = plugin.menuRepository.loadMenus()
        assertTrue(report.successful)

        plugin.menuService.openMenu(player, menuId)
        assertEquals(menuId, currentMenuId())
    }

    @Test
    fun feature_template_files_use_modern_shape_style() {
        releasedTemplateFiles().forEach { template ->
            val text = template.readText(Charsets.UTF_8)
            assertTrue(text.contains("Shape:"))
            assertFalse(text.contains(Regex("(?m)^Fill:|^fill:|\\bmats:|^\\s*conditions:|^\\s*states:|deny-actions:")))
        }
    }
}
