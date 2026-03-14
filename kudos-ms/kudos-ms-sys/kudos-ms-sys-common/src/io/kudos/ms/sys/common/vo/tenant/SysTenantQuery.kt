package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


/**
 * 租户查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantQuery (


    /** 名称 */
    val name: String? = null,

    val subSystemCode: String? = null,

    /** 仅启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {


    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysTenantRow::class

    override val operators: Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::name to OperatorEnum.ILIKE,
        ::subSystemCode to OperatorEnum.LIKE
    )


}