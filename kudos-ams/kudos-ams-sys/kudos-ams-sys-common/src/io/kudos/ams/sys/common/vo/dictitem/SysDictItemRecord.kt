package io.kudos.ams.sys.common.vo.dictitem

import io.kudos.base.support.result.IJsonResult
import io.kudos.base.support.result.IdJsonResult


/**
 * 字典项查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemRecord (

    //region your codes 1

    /** 模块编码 */
    var moduleCode: String? = null,

    /** 字典id */
    var dictId: String? = null,

    /** 字典类型 */
    var dictType: String? = null,

    /** 字典名称 */
    var dictName: String? = null,

    /** 字典项ID */
    var itemId: String? = null,

    /** 字典项编号 */
    var itemCode: String? = null,

    /** 父项ID */
    var parentId: String? = null,

    /** 字典项名称，或其国际化key */
    var itemName: String? = null,

    /** 该字典编号在同父节点下的排序号 */
    var orderNum: Int? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 备注 */
    var remark: String? = null,

    /** 父项编号 */
    var parentCode: String? = null,

    /** 所有父项ID */
    var parentIds: List<String>? = null

    //endregion your codes 1
//region your codes 2
) : IJsonResult {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}