package io.kudos.ms.sys.common.vo.datasource

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 数据源查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceQuery (

    //region your codes 1

    /** 名称 */
    val name: String? = null,

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 微服务编码 */
    val microServiceCode: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysDataSourceRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(::name.name to OperatorEnum.ILIKE)

    //endregion your codes 3

}
