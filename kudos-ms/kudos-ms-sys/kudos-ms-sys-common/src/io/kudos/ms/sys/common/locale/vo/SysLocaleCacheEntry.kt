package io.kudos.ms.sys.common.locale.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 语言/区域字典缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleCacheEntry(

    /** 主键 */
    override val id: String,

    /** 语言代码(如 zh_CN) */
    val code: String,

    /** 显示名称 */
    val displayName: String,

    /** 英文名称 */
    val englishName: String,

    /** 排序号 */
    val sortNo: Int,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean,

    /** 是否内置 */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5184729285406513962L
    }

}
