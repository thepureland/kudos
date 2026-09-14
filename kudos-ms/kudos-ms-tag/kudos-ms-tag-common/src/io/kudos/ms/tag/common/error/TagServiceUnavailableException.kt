package io.kudos.ms.tag.common.error

/** Fail-closed signal used when a remote tag operation cannot produce an authoritative result. */
class TagServiceUnavailableException(
    val operation: String,
    cause: Throwable?,
) : IllegalStateException("Tag service operation [$operation] is unavailable.", cause)
