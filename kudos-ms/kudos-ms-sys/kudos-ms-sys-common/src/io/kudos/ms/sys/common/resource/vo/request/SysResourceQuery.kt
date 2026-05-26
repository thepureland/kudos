package io.kudos.ms.sys.common.resource.vo.request

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.resource.vo.response.SysResourceRow
import kotlin.reflect.KProperty0


/**
 * Resource list query criteria request VO.
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceQuery (

    /** Name */
    val name: String? = null,

    /** url */
    val url: String? = null,

    /** Resource type dict code */
    val resourceTypeDictCode: String? = null,

    /** Sub-system code */
    val subSystemCode: String? = null,

    /** Active only */
    var active: Boolean? = true,

    /** Parent resource id */
    val parentId: String? = null,

    /** Tree level */
    val level: Int? = null

) : ListSearchPayload() {

    override fun getReturnEntityClass() = SysResourceRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::name to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}