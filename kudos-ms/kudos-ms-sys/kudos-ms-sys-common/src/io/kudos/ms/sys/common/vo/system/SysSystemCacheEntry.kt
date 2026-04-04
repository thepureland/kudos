package io.kudos.ms.sys.common.vo.system

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 系统缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemCacheEntry (

    override val id: String,

    /** 编码 */
    val code: String,

    /** 名称 */
    val name: String,

    /** 是否子系统 */
    val subSystem: Boolean,

    /** 父系统编号 */
    val parentCode: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean,

    /** 是否内置 */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8383289873002046675L
    }

}
