package cc.keer.amenu.service.provider

import cc.keer.amenu.config.PageRegionDefinition
import java.util.UUID

data class ProviderRequest(
    val viewerId: UUID,
    val viewerName: String,
    val menuId: String,
    val surfaceId: String,
    val providerType: String,
    val resolvedParams: Map<String, String>,
    val placeholders: Map<String, String>,
    val region: PageRegionDefinition,
)
