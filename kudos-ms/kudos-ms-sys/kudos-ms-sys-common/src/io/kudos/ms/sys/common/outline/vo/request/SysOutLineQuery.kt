package io.kudos.ms.sys.common.outline.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.outline.vo.response.SysOutLineRow
import kotlin.reflect.KProperty0


/**
 * 出网白名单列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineQuery(

    /** 名称 */
    val name: String? = null,

    /** 主机名 */
    val host: String? = null,

    /** 协议 */
    val protocol: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 仅启用 */
    val active: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysOutLineRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::name to OperatorEnum.ILIKE,
        ::host to OperatorEnum.ILIKE,
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}
