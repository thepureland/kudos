package io.kudos.ms.tag.core.fact.model

import io.kudos.ms.tag.common.error.TagErrorCode

enum class AttributeFactStatus { APPLIED, DUPLICATE, REJECTED, RETRYABLE_FAILED }

data class AttributeFactResult(
    val eventId: String,
    val status: AttributeFactStatus,
    val stateVersion: Long? = null,
    val errorCode: TagErrorCode? = null,
    val errorMessage: String? = null,
)

class AttributeFactValidationException(
    val errorCode: TagErrorCode,
    message: String,
) : IllegalArgumentException(message)
