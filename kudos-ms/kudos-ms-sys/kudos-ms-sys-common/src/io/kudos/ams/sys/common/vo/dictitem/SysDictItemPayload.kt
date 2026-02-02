package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.payload.FormPayload


/**
 * 字典项表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemPayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 字典项代码 */
    var itemCode: String? = null,

    /** 字典项名称 */
    var itemName: String? = null,

    /** 字典id */
    var dictId: String? = null,

    /** 字典项排序 */
    var orderNum: Int? = null,

    /** 父id */
    var parentId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}