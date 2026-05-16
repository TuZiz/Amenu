package cc.keer.amenu.service.provider

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.atomic.AtomicLong

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
        val current: ProviderResult? = null,
        val lastGood: ProviderResult.Success? = null,
        val expiresAtTick: Long? = null,
        val loading: Boolean = false,
        val requestToken: Long = 0L,
    )

    private val cache = Caffeine.newBuilder().build<Key, Entry>()
    private val tokens = AtomicLong()

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
        val token = tokens.incrementAndGet()
        cache.asMap().compute(key) { _, existing ->
            (existing ?: Entry()).copy(
                loading = true,
                requestToken = token,
            )
        }
        return token
    }

    fun complete(
        key: Key,
        requestToken: Long,
        result: ProviderResult,
        ttlTicks: Long?,
    ) {
        cache.asMap().compute(key) { _, existing ->
            val entry = existing ?: return@compute null
            if (entry.requestToken != requestToken) {
                return@compute entry
            }
            entry.copy(
                loading = false,
                current = result,
                lastGood = if (result is ProviderResult.Success) result else entry.lastGood,
                expiresAtTick = ttlTicks?.let { tickProvider() + it },
            )
        }
    }

    fun invalidate(
        key: Key,
        dropLastGood: Boolean = false,
    ) {
        cache.asMap().computeIfPresent(key) { _, entry ->
            entry.copy(
                current = null,
                expiresAtTick = null,
                loading = false,
                lastGood = if (dropLastGood) null else entry.lastGood,
            )
        }
    }

    fun clear() {
        cache.invalidateAll()
    }
}
