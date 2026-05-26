package io.kudos.base.model.payload

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * Form payload base class.
 *
 * @param T ID type
 * @author K
 * @since 1.0.0
 */
abstract class FormPayload<T>: IIdEntity<T> {

//    /** Unique identifier. */
//    @Suppress("UNCHECKED_CAST")
//    override var id: T = null as T

}