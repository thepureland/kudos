package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
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

    override fun getReturnEntityClass() = SysTenantRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::name to OperatorEnum.ILIKE,
        ::subSystemCode to OperatorEnum.LIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}