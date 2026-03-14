package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 字典项缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemCacheEntry (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 字典项代码 */
    val itemCode: String = "",

    /** 字典项名称 */
    val itemName: String = "",

    /** 字典id */
    val dictId: String = "",

    /** 字典项排序 */
    val orderNum: Int? = null,

    /** 父id */
    val parentId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

    /** 字典类型（来自 v_sys_dict_item 视图，用于 Hash 缓存副属性索引） */
    val dictType: String = "",

    /** 字典名称或其国际化key */
    val dictName: String = "",

    /** 原子服务编码（来自 v_sys_dict_item 视图，用于 Hash 缓存副属性索引） */
    val atomicServiceCode: String = "",

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 3064983536187872915L
    }

    /** 返回字符串字段 trim 后的副本，用于与 H2 CHAR 右填充对齐，保证缓存 key 一致。 */
    fun trimmed(): SysDictItemCacheEntry = copy(
        id = id.trim(),
        itemCode = itemCode.trim(),
        itemName = itemName.trim(),
        dictId = dictId.trim(),
        parentId = parentId?.trim(),
        remark = remark?.trim(),
        dictType = dictType.trim(),
        atomicServiceCode = atomicServiceCode.trim()
    )

}