package io.kudos.ms.sys.common.vo.microservice

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 微服务查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceQuery (


    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否为原子服务 */
    val atomicService: Boolean? = null,

    /** 仅启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {


    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysMicroServiceRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(
        ::code.name to OperatorEnum.ILIKE,
        ::name.name to OperatorEnum.ILIKE
    )


}
