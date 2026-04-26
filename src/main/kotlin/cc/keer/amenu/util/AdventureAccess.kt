package cc.keer.amenu.util

import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.UUID

object AdventureAccess {

    private val componentMessageMethod = resolveComponentMethod(CommandSender::class.java, "sendMessage", Component::class.java)
    private val componentDisplayNameMethod = resolveComponentMethod(ItemMeta::class.java, "displayName", Component::class.java)
    private val componentLoreMethod = resolveComponentMethod(ItemMeta::class.java, "lore", List::class.java)
    private val createProfileMethod = resolveStaticMethod("org.bukkit.Bukkit", "createProfile", UUID::class.java, String::class.java)
    private val profilePropertyConstructor = resolveConstructor("com.destroystokyo.paper.profile.ProfileProperty", String::class.java, String::class.java)
    private val profileSetPropertyMethod = resolveDynamicMethod(createProfileMethod?.returnType, "setProperty", profilePropertyConstructor?.declaringClass)
    private val skullSetPlayerProfileMethod = resolveFirstSingleArgumentMethod(SkullMeta::class.java, "setPlayerProfile")

    fun sendMessage(receiver: CommandSender, component: Component) {
        sendMessage(receiver, component, componentMessageMethod)
    }

    fun applyDisplayName(meta: ItemMeta, component: Component) {
        applyDisplayName(meta, component, componentDisplayNameMethod)
    }

    fun applyLore(meta: ItemMeta, lore: List<Component>) {
        applyLore(meta, lore, componentLoreMethod)
    }

    fun applySkullTexture(meta: SkullMeta, texture: String): Boolean {
        val property = runCatching {
            profilePropertyConstructor?.newInstance("textures", texture)
        }.getOrNull() ?: return false
        val profile = runCatching {
            createProfileMethod?.invoke(null, UUID.randomUUID(), "amenu")
        }.getOrNull() ?: return false
        val propertyApplied = runCatching {
            profileSetPropertyMethod?.invoke(profile, property)
            true
        }.getOrDefault(false)
        if (!propertyApplied) {
            return false
        }
        return runCatching {
            skullSetPlayerProfileMethod?.invoke(meta, profile)
            true
        }.getOrDefault(false)
    }

    internal fun sendMessage(
        receiver: CommandSender,
        component: Component,
        componentMethod: Method?,
    ) {
        val sent = runCatching {
            componentMethod?.invoke(receiver, component)
            componentMethod != null
        }.getOrDefault(false)
        if (!sent) {
            receiver.sendMessage(TextFormatter.legacyString(component))
        }
    }

    internal fun applyDisplayName(
        meta: ItemMeta,
        component: Component,
        componentMethod: Method?,
    ) {
        val applied = runCatching {
            componentMethod?.invoke(meta, component)
            componentMethod != null
        }.getOrDefault(false)
        if (!applied) {
            meta.setDisplayName(TextFormatter.legacyString(component))
        }
    }

    internal fun applyLore(
        meta: ItemMeta,
        lore: List<Component>,
        componentMethod: Method?,
    ) {
        val applied = runCatching {
            componentMethod?.invoke(meta, lore)
            componentMethod != null
        }.getOrDefault(false)
        if (!applied) {
            meta.lore = lore.map(TextFormatter::legacyString)
        }
    }

    private fun resolveComponentMethod(owner: Class<*>, name: String, parameterType: Class<*>): Method? {
        return runCatching { owner.getMethod(name, parameterType) }.getOrNull()
    }

    private fun resolveStaticMethod(ownerName: String, name: String, vararg parameterTypes: Class<*>): Method? {
        return runCatching {
            Class.forName(ownerName).getMethod(name, *parameterTypes)
        }.getOrNull()
    }

    private fun resolveConstructor(ownerName: String, vararg parameterTypes: Class<*>): Constructor<*>? {
        return runCatching {
            Class.forName(ownerName).getConstructor(*parameterTypes)
        }.getOrNull()
    }

    private fun resolveDynamicMethod(owner: Class<*>?, name: String, parameterType: Class<*>?): Method? {
        if (owner == null || parameterType == null) {
            return null
        }
        return runCatching { owner.getMethod(name, parameterType) }.getOrNull()
    }

    private fun resolveFirstSingleArgumentMethod(owner: Class<*>, name: String): Method? {
        return owner.methods.firstOrNull { method ->
            method.name == name && method.parameterCount == 1
        }
    }
}
