package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 租户查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantQuery (

    //region your codes 1

    /** 名称 */
    val name: String? = null,

    val subSystemCode: String? = null,

    /** 仅启用 */
    val active: Boolean? = true,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysTenantRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(
        ::name.name to OperatorEnum.ILIKE,
        ::subSystemCode.name to OperatorEnum.LIKE
    )

    //endregion your codes 3

}