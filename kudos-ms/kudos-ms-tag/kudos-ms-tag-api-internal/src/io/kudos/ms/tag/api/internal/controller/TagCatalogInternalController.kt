package io.kudos.ms.tag.api.internal.controller

import io.kudos.ms.tag.common.catalog.api.ITagCatalogApi
import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.bind.annotation.RestController

@RestController
open class TagCatalogInternalController(
    @Qualifier("tagCatalogApi") private val delegate: ITagCatalogApi,
    private val tenantAccessGuard: TagTenantAccessGuard,
) : ITagCatalogApi {
    override fun listTags(tenantId: String, subjectType: String): List<TagDefinitionView> {
        tenantAccessGuard.requireTenant(tenantId)
        return delegate.listTags(tenantId, subjectType)
    }
}
