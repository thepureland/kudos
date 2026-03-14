package io.kudos.ms.sys.common.vo.system

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 系统缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemCacheEntry (


    override val id: String = "",

    /** 编码 */
    val code: String = "",

    /** 名称 */
    val name: String = "",

    /** 是否子系统 */
    val subSystem: Boolean = true,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 8383289873002046675L
    }

}
