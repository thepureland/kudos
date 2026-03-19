package io.kudos.ms.sys.common.vo.system.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.vo.system.response.SysSystemRow
import kotlin.reflect.KProperty0


/**
 * 系统列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemQuery (

    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否子系统 */
    val subSystem: Boolean? = null,

    /** 是否启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysSystemRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::name to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

    override fun getSortableProperties() = setOf(
        ::code.name,
        ::name.name,
    )

}