package cc.keer.amenu.service.provider

import cc.keer.amenu.config.PageEntryDefinition

sealed interface ProviderResult {
    data class Success(
        val entries: List<PageEntryDefinition> = emptyList(),
        val placeholders: Map<String, String> = emptyMap(),
        val ttlTicks: Long? = null,
    ) : ProviderResult

    data object Empty : ProviderResult

    data class Error(
        val message: String? = null,
    ) : ProviderResult
}
