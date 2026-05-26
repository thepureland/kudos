package io.kudos.ms.sys.common.domain.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.domain.vo.response.SysDomainRow
import kotlin.reflect.KProperty0


/**
 * Domain list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainQuery (

    /** Domain */
    val domain: String? = null,

    /** System code */
    val systemCode: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Active only */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysDomainRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::domain to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}