package io.kudos.base.model.contract.result

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * A result object carrying a unique identifier, to be returned as JSON; properties with null values are automatically stripped.
 *
 * @param T ID type
 * @author K
 * @since 1.0.0
 */
abstract class IdJsonResult<T>: IJsonResult, IIdEntity<T> {

//    /** Unique identifier */
//    @Suppress("UNCHECKED_CAST")
//    override var id: T = null as T

}
