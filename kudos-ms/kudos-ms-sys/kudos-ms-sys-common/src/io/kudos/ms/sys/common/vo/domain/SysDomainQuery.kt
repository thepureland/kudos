package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import kotlin.reflect.KProperty0


/**
 * 域名查询条件载体
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


    constructor() : this("")

    override fun getReturnEntityClass() = SysDomainRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::domain to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}