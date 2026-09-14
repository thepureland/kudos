package io.kudos.ms.tag.common.subject.model

import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.requireTag
import kotlinx.serialization.Serializable

/** Complete tenant-scoped identity of an object that can receive tags. */
@Serializable
data class TagSubjectKey(
    val tenantId: String,
    val subjectType: String,
    val subjectId: String,
) {
    init {
        requireTag(tenantId.isNotBlank(), TagErrorCode.INVALID_SUBJECT_KEY) {
            "tenantId must not be blank"
        }
        requireTag(subjectId.isNotBlank(), TagErrorCode.INVALID_SUBJECT_KEY) {
            "subjectId must not be blank"
        }
        requireTag(SUBJECT_TYPE_PATTERN.matches(subjectType), TagErrorCode.INVALID_SUBJECT_TYPE) {
            "subjectType must be a lowercase namespaced code, for example estate.house"
        }
    }

    private companion object {
        val SUBJECT_TYPE_PATTERN = Regex("^[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)+$")
    }
}
