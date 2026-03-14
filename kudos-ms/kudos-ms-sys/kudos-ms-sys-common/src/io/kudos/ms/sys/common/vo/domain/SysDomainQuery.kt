package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


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

    override var returnEntityClass: KClass<*>? = SysDomainRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(::domain.name to OperatorEnum.ILIKE)


}