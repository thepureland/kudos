package io.kudos.ms.sys.common.vo.resource

import io.kudos.base.support.result.IdJsonResult


/**
 * 资源查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceRecord (

    //region your codes 1

    /** 主键 */
    override val id: String = "",

    /** 名称 */
    val name: String? = null,

    /** url */
    val url: String? = null,

    /** 资源类型字典代码 */
    val resourceTypeDictCode: String? = null,

    /** 父id */
    val parentId: String? = null,

    /** 在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 图标 */
    val icon: String? = null,

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}