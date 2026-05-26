package io.kudos.ms.sys.common.tenant.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import kotlin.reflect.KProperty0


/**
 * Tenant list query condition request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantQuery (

    /** Name. */
    val name: String? = null,

    val subSystemCode: String? = null,

    /** Active only. */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysTenantRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::name to OperatorEnum.ILIKE,
        ::subSystemCode to OperatorEnum.LIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}