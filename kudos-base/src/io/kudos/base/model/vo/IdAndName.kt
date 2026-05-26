package io.kudos.base.model.vo

/**
 * Wrapper class for id and name.
 *
 * @author K
 * @since 1.0.0
 */
data class IdAndName<T> (

    /** Unique identifier */
    val id: T,

    /** Name */
    val name: String

)
