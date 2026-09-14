package io.kudos.ms.tag.common.query.model

import kotlinx.serialization.Serializable

/** Stable subject-id keyset page. A null cursor means there is no known next page. */
@Serializable
data class TagSubjectPage(
    val subjectIds: List<String>,
    val nextSubjectId: String? = null,
)
