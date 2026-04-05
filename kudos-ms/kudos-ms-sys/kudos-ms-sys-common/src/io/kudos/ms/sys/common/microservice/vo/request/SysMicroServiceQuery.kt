package io.kudos.ms.sys.common.microservice.vo.request
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.microservice.vo.response.SysMicroServiceRow
import kotlin.reflect.KProperty0


/**
 * 微服务列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceQuery (

    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否为原子服务 */
    val atomicService: Boolean? = null,

    /** 仅启用 */
    val active: Boolean? = true,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysMicroServiceRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(
        ::code to OperatorEnum.ILIKE,
        ::name to OperatorEnum.ILIKE
    )

    override fun isUnpagedSearchAllowed(): Boolean = true

}