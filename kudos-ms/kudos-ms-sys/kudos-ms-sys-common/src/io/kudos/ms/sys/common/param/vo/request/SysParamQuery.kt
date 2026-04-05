package io.kudos.ms.sys.common.param.vo.request
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.param.vo.response.SysParamRow
import kotlin.reflect.KProperty0


/**
 * 参数列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysParamQuery (

    /** 参数名称 */
    val paramName: String? = null,

    /** 参数值 */
    val paramValue: String? = null,

    /** 默认参数值 */
    val defaultValue: String? = null,

    /** 模块 */
    val atomicServiceCode: String? = null,

    /** 仅启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysParamRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::paramName to OperatorEnum.ILIKE,
        ::paramValue to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}