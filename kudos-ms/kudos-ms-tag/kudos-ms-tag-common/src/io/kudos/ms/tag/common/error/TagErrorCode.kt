package io.kudos.ms.tag.common.error

import kotlinx.serialization.Serializable

/** Stable machine-readable tag-domain error codes. */
@Serializable
enum class TagErrorCode {
    INVALID_SUBJECT_KEY,
    INVALID_SUBJECT_TYPE,
    INVALID_ATTRIBUTE_CODE,
    INVALID_TAG_CODE,
    INVALID_ATTRIBUTE_VALUE,
    INVALID_ATTRIBUTE_FACT,
    EMPTY_RULE_GROUP,
    INVALID_OPERAND_COUNT,
    EMPTY_QUERY_GROUP,
    IDEMPOTENCY_CONFLICT,
}

/** Raised when a shared tag contract violates a structural invariant. */
class TagValidationException(
    val errorCode: TagErrorCode,
    message: String,
) : IllegalArgumentException(message)

internal fun requireTag(
    condition: Boolean,
    errorCode: TagErrorCode,
    lazyMessage: () -> String,
) {
    if (!condition) {
        throw TagValidationException(errorCode, lazyMessage())
    }
}
