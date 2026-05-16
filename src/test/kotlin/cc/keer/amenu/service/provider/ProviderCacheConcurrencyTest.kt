package cc.keer.amenu.service.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ProviderCacheConcurrencyTest {

    @Test
    fun stale_request_token_does_not_override_newer_result() {
        var tick = 10L
        val cache = ProviderCache(tickProvider = { tick })
        val key = key()
        val oldToken = cache.beginLoad(key)
        val newToken = cache.beginLoad(key)

        val newer = ProviderResult.Success(placeholders = mapOf("value" to "new"))
        cache.complete(key, newToken, newer, ttlTicks = 20)
        cache.complete(key, oldToken, ProviderResult.Error("old"), ttlTicks = 20)

        val snapshot = cache.read(key)
        assertSame(newer, snapshot.current)
        assertSame(newer, snapshot.lastGood)
        assertFalse(snapshot.loading)
        tick = 31L
        assertTrue(cache.read(key).expired)
    }

    @Test
    fun invalidate_preserves_last_good_unless_requested() {
        val cache = ProviderCache()
        val key = key()
        val token = cache.beginLoad(key)
        val success = ProviderResult.Success(placeholders = mapOf("value" to "ok"))
        cache.complete(key, token, success, ttlTicks = null)

        cache.invalidate(key)
        assertEquals(null, cache.read(key).current)
        assertSame(success, cache.read(key).lastGood)

        cache.invalidate(key, dropLastGood = true)
        assertEquals(null, cache.read(key).lastGood)
    }

    private fun key(): ProviderCache.Key {
        return ProviderCache.Key(
            viewerId = UUID.randomUUID(),
            menuId = "main",
            surfaceId = "region",
            providerType = "test",
        )
    }
}
