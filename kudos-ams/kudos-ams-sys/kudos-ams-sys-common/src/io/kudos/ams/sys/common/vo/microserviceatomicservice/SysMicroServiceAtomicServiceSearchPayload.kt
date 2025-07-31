package io.kudos.ams.sys.common.vo.microserviceatomicservice

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 微服务-原子服务关系查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceAtomicServiceSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = SysMicroServiceAtomicServiceRecord::class,

    /** 微服务编码 */
    var microServiceCode: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(SysMicroServiceAtomicServiceRecord::class)

    //endregion your codes 3

}