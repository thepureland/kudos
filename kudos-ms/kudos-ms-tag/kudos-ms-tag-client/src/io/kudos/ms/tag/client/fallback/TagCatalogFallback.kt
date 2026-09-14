package io.kudos.ms.tag.client.fallback

import io.kudos.ms.tag.client.proxy.ITagCatalogProxy
import io.kudos.ms.tag.common.catalog.model.TagDefinitionView
import io.kudos.ms.tag.common.error.TagServiceUnavailableException

open class TagCatalogFallback : ITagCatalogProxy {
    fun listTags(cause: Throwable, tenantId: String, subjectType: String): List<TagDefinitionView> =
        unavailable("listTags", cause)

    override fun listTags(tenantId: String, subjectType: String): List<TagDefinitionView> =
        unavailable("listTags", null)

    private fun <T> unavailable(operation: String, cause: Throwable?): T =
        throw TagServiceUnavailableException(operation, cause)
}
