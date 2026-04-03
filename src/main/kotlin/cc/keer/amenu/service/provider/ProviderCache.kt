package cc.keer.amenu.service.provider

import com.github.benmanes.caffeine.cache.Caffeine

class ProviderCache(
    private val tickProvider: () -> Long = { System.nanoTime() / 50_000_000L },
) {

    data class Key(
        val viewerId: java.util.UUID,
        val menuId: String,
        val surfaceId: String,
        val providerType: String,
    )

    data class Snapshot(
        val current: ProviderResult?,
        val lastGood: ProviderResult.Success?,
        val expired: Boolean,
        val loading: Boolean,
    )

    private data class Entry(
        var current: ProviderResult? = null,
        var lastGood: ProviderResult.Success? = null,
        var expiresAtTick: Long? = null,
        var loading: Boolean = false,
        var requestToken: Long = 0L,
    )

    private val cache = Caffeine.newBuilder().build<Key, Entry>()

    fun read(key: Key): Snapshot {
        val entry = cache.getIfPresent(key)
        val expired = entry?.expiresAtTick?.let { tickProvider() >= it } ?: false
        return Snapshot(
            current = entry?.current,
            lastGood = entry?.lastGood,
            expired = expired,
            loading = entry?.loading == true,
        )
    }

    fun beginLoad(key: Key): Long {
        val entry = cache.get(key) { Entry() }!!
        entry.loading = true
        entry.requestToken += 1
        return entry.requestToken
    }

    fun complete(
        key: Key,
        requestToken: Long,
        result: ProviderResult,
        ttlTicks: Long?,
    ) {
        val entry = cache.get(key) { Entry() } ?: return
        if (entry.requestToken != requestToken) {
            return
        }
        entry.loading = false
        entry.current = result
        if (result is ProviderResult.Success) {
            entry.lastGood = result
        }
        entry.expiresAtTick = ttlTicks?.let { tickProvider() + it }
    }

    fun invalidate(key: Key) {
        val entry = cache.getIfPresent(key) ?: return
        entry.current = null
        entry.expiresAtTick = null
        entry.loading = false
    }

    fun clear() {
        cache.invalidateAll()
    }
}
