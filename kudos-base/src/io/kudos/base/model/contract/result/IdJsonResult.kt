package io.kudos.base.model.contract.result

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 要以json返回的带有惟一标识的结果对象，会自动去除值为null的属性
 *
 * @param T ID类型
 * @author K
 * @since 1.0.0
 */
abstract class IdJsonResult<T>: IJsonResult, IIdEntity<T> {

//    /** 惟一标识 */
//    @Suppress("UNCHECKED_CAST")
//    override var id: T = null as T

}