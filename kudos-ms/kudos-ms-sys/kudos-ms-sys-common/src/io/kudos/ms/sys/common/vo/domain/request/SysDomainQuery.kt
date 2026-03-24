package io.kudos.ms.sys.common.vo.domain.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.domain.response.SysDomainRow
import kotlin.reflect.KProperty0


/**
 * 域名列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainQuery (

    /** 域名 */
    val domain: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 仅启用 */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysDomainRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::domain to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}