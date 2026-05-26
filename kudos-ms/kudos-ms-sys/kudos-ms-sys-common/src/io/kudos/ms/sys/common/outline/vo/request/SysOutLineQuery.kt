package io.kudos.ms.sys.common.outline.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.outline.vo.response.SysOutLineRow
import kotlin.reflect.KProperty0


/**
 * Request VO for querying the outbound whitelist list.
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineQuery(

    /** Name */
    val name: String? = null,

    /** Hostname */
    val host: String? = null,

    /** Protocol */
    val protocol: String? = null,

    /** System code */
    val systemCode: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Only enabled */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysOutLineRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::name to OperatorEnum.ILIKE,
        ::host to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}
