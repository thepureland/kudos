package io.kudos.ms.sys.common.vo.resource

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 资源查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceQuery (

    //region your codes 1

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

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = SysResourceRow::class

    override var operators: Map<String, OperatorEnum>? = mapOf(::name.name to OperatorEnum.ILIKE)

    //endregion your codes 3

}