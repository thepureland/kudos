package io.kudos.ms.tag.common.query.model

import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.requireTag
import kotlinx.serialization.Serializable

/** A bounded keyset-paged query over materialized tag assignments. */
@Serializable
data class TagQueryRequest(
    val tenantId: String,
    val subjectType: String,
    val expression: TagQueryExpression,
    val afterSubjectId: String? = null,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    init {
        requireTag(tenantId.isNotBlank(), TagErrorCode.INVALID_QUERY_REQUEST) { "Tenant id must not be blank." }
        requireTag(subjectType.isNotBlank(), TagErrorCode.INVALID_QUERY_REQUEST) { "Subject type must not be blank." }
        requireTag(afterSubjectId == null || afterSubjectId.isNotBlank(), TagErrorCode.INVALID_QUERY_REQUEST) {
            "Subject cursor must be null or non-blank."
        }
        requireTag(pageSize in 1..MAX_PAGE_SIZE, TagErrorCode.QUERY_LIMIT_EXCEEDED) {
            "Tag query page size must be between 1 and $MAX_PAGE_SIZE."
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 100
        const val MAX_PAGE_SIZE = 500
    }
}
