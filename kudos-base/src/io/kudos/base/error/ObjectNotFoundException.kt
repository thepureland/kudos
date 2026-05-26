package io.kudos.base.error

/**
 * Thrown when the target object cannot be found.
 * Used when the object was expected to exist but was not located.
 *
 * @author K
 * @since 1.0.0
 */
class ObjectNotFoundException(
    override val message: String?,
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {

    constructor(e: Throwable) : this(e.message, e)

}