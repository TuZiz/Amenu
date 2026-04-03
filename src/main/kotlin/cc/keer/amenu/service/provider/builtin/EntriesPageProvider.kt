package cc.keer.amenu.service.provider.builtin

import cc.keer.amenu.platform.PlatformScheduler
import cc.keer.amenu.service.provider.MenuDataProvider
import cc.keer.amenu.service.provider.ProviderRequest
import cc.keer.amenu.service.provider.ProviderResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class EntriesPageProvider(
    private val platformScheduler: PlatformScheduler,
) : MenuDataProvider {

    override fun load(request: ProviderRequest): CompletionStage<ProviderResult> {
        val success = ProviderResult.Success(entries = request.region.entries)
        val delayTicks = request.region.asyncDelayTicks.coerceAtLeast(0L)
        if (delayTicks <= 0L) {
            return CompletableFuture.completedFuture(success)
        }

        val future = CompletableFuture<ProviderResult>()
        platformScheduler.runLaterAsync(
            delayTicks,
            Runnable { future.complete(success) },
        )
        return future
    }
}
