package cc.keer.amenu.service.provider

import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.service.provider.builtin.EntriesPageProvider
import cc.keer.amenu.service.provider.builtin.PlaceholderStateProvider
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class MenuProviderRegistry {

    private val providers = ConcurrentHashMap<String, MenuDataProvider>()

    fun register(type: String, provider: MenuDataProvider): MenuProviderRegistry {
        providers[type.normalize()] = provider
        return this
    }

    fun provider(type: String): MenuDataProvider? {
        return providers[type.normalize()]
    }

    companion object {
        fun createBuiltins(platformScheduler: PlatformScheduler): MenuProviderRegistry {
            val entries = EntriesPageProvider(platformScheduler)
            return MenuProviderRegistry()
                .register("entries", entries)
                .register("legacy-static", entries)
                .register("legacy-static-delayed", entries)
                .register("placeholder-state", PlaceholderStateProvider())
        }
    }
}

private fun String.normalize(): String = trim().lowercase(Locale.ROOT)
