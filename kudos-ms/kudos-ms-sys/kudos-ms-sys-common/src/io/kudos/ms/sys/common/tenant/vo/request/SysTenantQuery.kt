package io.kudos.ms.sys.common.tenant.vo.request
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import kotlin.reflect.KProperty0


/**
 * 租户列表查询条件请求VO
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

    override fun getReturnEntityClass() = SysTenantRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::name to OperatorEnum.ILIKE,
        ::subSystemCode to OperatorEnum.LIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}