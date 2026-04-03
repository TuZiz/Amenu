package cc.keer.amenu.service.provider

import java.util.concurrent.CompletionStage

interface MenuDataProvider {
    fun load(request: ProviderRequest): CompletionStage<ProviderResult>
}
