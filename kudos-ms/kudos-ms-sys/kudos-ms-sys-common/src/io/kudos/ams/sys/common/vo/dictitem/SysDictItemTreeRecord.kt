package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.result.IdJsonResult


/**
 * 字典项树节点记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemTreeRecord (

    //region your codes 1

    /** 字典项ID */
    override var id: String? = null,

    /** 字典项编号 */
    var itemCode: String? = null,

    /** 字典项名称 */
    var itemName: String? = null,

    /** 父项ID */
    var parentId: String? = null,

    /** 该字典编号在同父节点下的排序号 */
    var orderNum: Int? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 备注 */
    var remark: String? = null,

    /** 子字典项列表 */
    var children: MutableList<SysDictItemTreeRecord>? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}
