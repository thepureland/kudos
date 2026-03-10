package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.result.IJsonResult


/**
 * 字典项查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemRow (

    //region your codes 1

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 字典id */
    val dictId: String = "",

    /** 字典类型 */
    val dictType: String = "",

    /** 字典名称 */
    val dictName: String = "",

    /** 字典项ID */
    val itemId: String = "",

    /** 字典项编号 */
    val itemCode: String = "",

    /** 父项ID */
    val parentId: String? = null,

    /** 字典项名称，或其国际化key */
    val itemName: String = "",

    /** 该字典编号在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 备注 */
    val remark: String? = null,

    /** 父项编号 */
    var parentCode: String? = null,

    /** 所有父项ID */
    var parentIds: List<String>? = null

    //endregion your codes 1
//region your codes 2
) : IJsonResult {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}