package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.payload.FormPayload


/**
 * 字典表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictPayload (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 字典类型 */
    var dictType: String? = null,

    /** 字典名称 */
    var dictName: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    var parentId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    var code: String? = null, //TODO

    var name: String? = null, //TODO

    var seqNo: Int? = null, //TODO


    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}