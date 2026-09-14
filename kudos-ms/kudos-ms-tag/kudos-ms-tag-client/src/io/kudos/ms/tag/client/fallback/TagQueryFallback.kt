package io.kudos.ms.tag.client.fallback

import io.kudos.ms.tag.client.proxy.ITagQueryProxy
import io.kudos.ms.tag.common.error.TagServiceUnavailableException
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage

open class TagQueryFallback : ITagQueryProxy {
    fun findSubjects(cause: Throwable, request: TagQueryRequest): TagSubjectPage =
        unavailable("findSubjects", cause)

    override fun findSubjects(request: TagQueryRequest): TagSubjectPage =
        unavailable("findSubjects", null)

    private fun <T> unavailable(operation: String, cause: Throwable?): T =
        throw TagServiceUnavailableException(operation, cause)
}
