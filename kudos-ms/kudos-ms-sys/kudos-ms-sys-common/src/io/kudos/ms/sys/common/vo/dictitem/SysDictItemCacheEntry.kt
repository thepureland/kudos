package io.kudos.ms.sys.common.vo.dictitem

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 字典项缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemCacheEntry (

    /** 主键 */
    override val id: String,

    /** 字典项代码 */
    val itemCode: String,

    /** 字典项名称 */
    val itemName: String,

    /** 字典id */
    val dictId: String,

    /** 字典项排序 */
    val orderNum: Int?,

    /** 父id */
    val parentId: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean,

    /** 是否内置 */
    val builtIn: Boolean,

    /** 字典类型（来自 v_sys_dict_item 视图，用于 Hash 缓存副属性索引） */
    val dictType: String,

    /** 字典名称或其国际化key */
    val dictName: String,

    /** 原子服务编码（来自 v_sys_dict_item 视图，用于 Hash 缓存副属性索引） */
    val atomicServiceCode: String,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 3064983536187872915L
    }

}
