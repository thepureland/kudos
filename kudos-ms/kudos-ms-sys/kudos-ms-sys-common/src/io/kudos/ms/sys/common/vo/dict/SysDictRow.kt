package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.result.IdJsonResult


/**
 * 字典查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictRow (


    /** 主键 */
    override val id: String = "",

    /** 字典类型 */
    val dictType: String = "",

    /** 字典名称 */
    val dictName: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    val parentId: String? = null,

    val parentCode: String? = null,

    val parentIds: List<String>? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

    val itemId: String? = null, //TODO

    val itemCode: String? = null, //TODO

    val itemName: String? = null, //TODO

    val seqNo: Int? = null, //TODO

) : IdJsonResult<String>() {


    constructor() : this("")


}