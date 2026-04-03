package cc.keer.amenu.service.provider.builtin

import cc.keer.amenu.service.provider.MenuDataProvider
import cc.keer.amenu.service.provider.ProviderRequest
import cc.keer.amenu.service.provider.ProviderResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class PlaceholderStateProvider : MenuDataProvider {
    override fun load(request: ProviderRequest): CompletionStage<ProviderResult> {
        return CompletableFuture.completedFuture(
            ProviderResult.Success(placeholders = request.resolvedParams),
        )
    }
}
