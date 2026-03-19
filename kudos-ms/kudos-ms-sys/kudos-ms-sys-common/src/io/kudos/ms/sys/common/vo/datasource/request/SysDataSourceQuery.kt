package io.kudos.ms.sys.common.vo.datasource.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.datasource.response.SysDataSourceRow
import kotlin.reflect.KProperty0


/**
 * 数据源列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceQuery (

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

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysDataSourceRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::name to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

    override fun getSortableProperties() = setOf(::name.name)

}