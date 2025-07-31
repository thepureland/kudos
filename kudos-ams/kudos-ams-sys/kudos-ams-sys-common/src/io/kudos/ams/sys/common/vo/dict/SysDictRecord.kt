package io.kudos.ams.sys.common.vo.dict

import io.kudos.base.support.result.IdJsonResult


/**
 * 字典查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictRecord (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 字典类型 */
    var dictType: String? = null,

    /** 字典名称 */
    var dictName: String? = null,

    /** 模块编码 */
    var moduleCode: String? = null,

    var parentId: String? = null,

    var parentCode: String? = null,

    var parentIds: List<String>? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    var itemId: String? = null, //TODO

    var itemCode: String? = null, //TODO

    var itemName: String? = null, //TODO

    var seqNo: Int? = null, //TODO

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}