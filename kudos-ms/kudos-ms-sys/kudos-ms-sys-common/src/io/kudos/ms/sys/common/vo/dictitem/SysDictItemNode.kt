package io.kudos.ms.sys.common.vo.dictitem

/**
 * 字典项节点
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemNode(

    /** 主键 */
    val id: String,

    /** 字典项代码 */
    val itemCode: String = "",

    /** 字典项名称 */
    val itemName: String = "",

)