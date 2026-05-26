package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import kotlin.reflect.KProperty0


/**
 * Data source list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceQuery (

    /** Name */
    val name: String? = null,

    /** Subsystem code */
    val subSystemCode: String? = null,

    /** Microservice code */
    val microServiceCode: String? = null,

    /** Tenant id */
    val tenantId: String? = null,

    /** Whether enabled */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysDataSourceRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::name to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}