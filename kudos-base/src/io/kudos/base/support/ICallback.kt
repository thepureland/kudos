package io.kudos.base.support

import java.io.Serializable

/**
 * Callback interface.
 *
 * @param P Parameter type
 * @param R Return value type
 * @author K
 * @since 1.0.0
 */
fun interface ICallback<P, R> : Serializable {

    /**
     * Callback behavior.
     *
     * @param p Parameter
     * @return Return value
     * @author K
     * @since 1.0.0
     */
    fun execute(p: P): R

}