package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.result.IdJsonResult


/**
 * 字典项树节点记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemTreeRow (

    //region your codes 1

    /** 字典项ID */
    override val id: String = "",

    /** 字典项编号 */
    val itemCode: String = "",

    /** 字典项名称 */
    val itemName: String = "",

    /** 父项ID */
    val parentId: String? = null,

    /** 该字典编号在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 备注 */
    val remark: String? = null,

    /** 子字典项列表 */
    val children: MutableList<SysDictItemTreeRow>? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
