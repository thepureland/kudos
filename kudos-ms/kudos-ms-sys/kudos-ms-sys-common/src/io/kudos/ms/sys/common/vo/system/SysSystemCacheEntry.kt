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

    //region your codes 1

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

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 8383289873002046675L
    }

}
