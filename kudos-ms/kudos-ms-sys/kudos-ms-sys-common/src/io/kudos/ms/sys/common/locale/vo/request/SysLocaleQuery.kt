package io.kudos.ms.sys.common.locale.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.locale.vo.response.SysLocaleRow
import kotlin.reflect.KProperty0


/**
 * 语言字典列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleQuery(

    /** 语言代码 */
    val code: String? = null,

    /** 显示名称 */
    val displayName: String? = null,

    /** 仅启用 */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysLocaleRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::displayName to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}
