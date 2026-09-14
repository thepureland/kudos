package io.kudos.ms.tag.core.api

import io.kudos.ms.tag.common.catalog.api.ITagCatalogApi
import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import io.kudos.ms.tag.core.catalog.service.iservice.ITagCatalogService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Primary
@Component
open class TagCatalogApi(
    private val service: ITagCatalogService,
) : ITagCatalogApi {
    override fun listTags(tenantId: String, subjectType: String): List<TagDefinitionView> =
        service.listTags(tenantId, subjectType)
}
