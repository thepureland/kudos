package io.kudos.ms.sys.common.vo.system

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 系统缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysSystemCacheItem (

    //region your codes 1

    override val id: String = "",

    /** 编码 */
    val code: String? = null,

    /** 名称 */
    val name: String? = null,

    /** 是否子系统 */
    val subSystem: Boolean? = null,

    /** 父系统编号 */
    val parentCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

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
