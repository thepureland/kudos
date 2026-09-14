package io.kudos.ms.tag.common.catalog.api

import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

/** Shared local/remote read contract for reusable tag definitions. */
interface ITagCatalogApi {
    @GetExchange("/api/internal/tag/catalog/tags")
    fun listTags(
        @RequestParam tenantId: String,
        @RequestParam subjectType: String,
    ): List<TagDefinitionView>
}
