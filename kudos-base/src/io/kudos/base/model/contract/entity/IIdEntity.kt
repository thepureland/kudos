package io.kudos.base.model.contract.entity

/**
 * Entity interface with an immutable id.
 *
 * @param T the entity type
 * @author K
 * @since 1.0.0
 */
interface IIdEntity<T> {

    /** Unique identifier */
    val id: T

}
