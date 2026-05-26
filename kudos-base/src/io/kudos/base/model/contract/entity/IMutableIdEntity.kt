package io.kudos.base.model.contract.entity

/**
 * Entity interface with a mutable id.
 *
 * @param T the entity type
 * @author K
 * @since 1.0.0
 */
interface IMutableIdEntity<T> : IIdEntity<T> {

    /** Unique identifier */
    override var id: T

}
