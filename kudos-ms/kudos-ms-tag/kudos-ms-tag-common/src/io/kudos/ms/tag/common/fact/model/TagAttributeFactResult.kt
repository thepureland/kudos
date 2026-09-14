package io.kudos.ms.tag.common.fact.model

import io.kudos.ms.tag.common.error.TagErrorCode
import kotlinx.serialization.Serializable

@Serializable
enum class TagAttributeFactStatus { APPLIED, DUPLICATE, REJECTED, RETRYABLE_FAILED }

@Serializable
data class TagAttributeFactResult(
    val eventId: String,
    val status: TagAttributeFactStatus,
    val stateVersion: Long? = null,
    val errorCode: TagErrorCode? = null,
    val errorMessage: String? = null,
)
