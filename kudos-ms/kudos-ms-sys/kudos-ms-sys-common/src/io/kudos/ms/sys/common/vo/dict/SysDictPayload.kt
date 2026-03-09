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
    override val id: String = "",

    //region your codes 1

    /** 字典类型 */
    val dictType: String = "",

    /** 字典名称 */
    val dictName: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    val parentId: String? = null,

    /** 备注 */
    val remark: String? = null,

    val code: String? = null, //TODO

    val name: String? = null, //TODO

    val seqNo: Int? = null, //TODO


    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}