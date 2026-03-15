package io.kudos.ms.sys.common.vo.resource

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.model.payload.ListSearchPayload
import kotlin.reflect.KProperty0


/**
 * 资源查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceQuery (


    /** 名称 */
    val name: String? = null,

    /** url */
    val url: String? = null,

    /** 资源类型字典代码 */
    val resourceTypeDictCode: String? = null,

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 仅启用 */
    var active: Boolean? = true,

    /** 父资源id */
    val parentId: String? = null,

    /** 树层级 */
    val level: Int? = null

) : ListSearchPayload() {

    constructor() : this("")

    override fun getReturnEntityClass() = SysResourceRow::class

    override fun getOperators(): Map<KProperty0<*>, OperatorEnum> = mapOf(::name to OperatorEnum.ILIKE)

    override fun isUnpagedSearchAllowed(): Boolean = true

}