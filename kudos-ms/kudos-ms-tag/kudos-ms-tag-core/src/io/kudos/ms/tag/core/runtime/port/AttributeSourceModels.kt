package io.kudos.ms.tag.core.runtime.port

import io.kudos.ms.tag.common.attribute.model.TagAttributeFact

data class AttributeSourceConfig(
    val tenantId: String,
    val subjectType: String,
    val properties: Map<String, String> = emptyMap(),
)

@JvmInline
value class SourceCursor(val value: String)

data class AttributeSourceBatch(
    val facts: List<TagAttributeFact>,
    val nextCursor: SourceCursor?,
    val exhausted: Boolean,
)

data class ValidationError(
    val code: String,
    val message: String,
    val property: String? = null,
)
