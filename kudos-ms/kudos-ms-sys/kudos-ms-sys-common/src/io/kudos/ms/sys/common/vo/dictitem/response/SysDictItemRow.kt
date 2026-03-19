package io.kudos.ms.sys.common.vo.dictitem.response


/**
 * 字典项列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemRow (

    /** 字典项id */
    val id: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 字典id */
    val dictId: String = "",

    /** 字典类型 */
    val dictType: String = "",

    /** 字典名称 */
    val dictName: String = "",

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

)