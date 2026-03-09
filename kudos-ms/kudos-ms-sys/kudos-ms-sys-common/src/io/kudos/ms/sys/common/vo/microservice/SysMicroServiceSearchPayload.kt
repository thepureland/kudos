package io.kudos.ms.sys.common.vo.microservice

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 微服务查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceSearchPayload (

    //region your codes 1

    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否为原子服务 */
    val atomicService: Boolean? = null,

    /** 仅启用 */
    val active: Boolean? = true,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysMicroServiceRecord::class

    //endregion your codes 3

}
