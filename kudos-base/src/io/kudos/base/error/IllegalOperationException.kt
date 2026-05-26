package io.kudos.base.error

/**
 * Thrown for illegal operations.
 *
 * @author K
 * @since 1.0.0
 */
class IllegalOperationException (
    override val message: String?,
    override val cause: Throwable? = null
) : RuntimeException(message, cause) {

    constructor(e: Throwable) : this(e.message, e)

}