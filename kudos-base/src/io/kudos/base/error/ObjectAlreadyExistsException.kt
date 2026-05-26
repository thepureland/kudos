package io.kudos.base.error

/**
 * Thrown when the target object already exists.
 *
 * @author K
 * @since 1.0.0
 */
class ObjectAlreadyExistsException(
    override val message: String?,
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {

    constructor(e: Throwable) : this(e.message, e)

}