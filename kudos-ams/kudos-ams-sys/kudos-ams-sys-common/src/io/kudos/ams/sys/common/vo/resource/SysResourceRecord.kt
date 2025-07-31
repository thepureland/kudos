package io.kudos.ams.sys.common.vo.resource

import io.kudos.base.support.result.IdJsonResult


/**
 * 资源查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceRecord (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** url */
    var url: String? = null,

    /** 资源类型字典代码 */
    var resourceTypeDictCode: String? = null,

    /** 父id */
    var parentId: String? = null,

    /** 在同父节点下的排序号 */
    var orderNum: Int? = null,

    /** 图标 */
    var icon: String? = null,

    /** 子系统编码 */
    var subSystemCode: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}