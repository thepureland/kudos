package io.kudos.ams.sys.common.vo.module

import java.io.Serializable
import java.time.LocalDateTime


/**
 * 模块缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysModuleCacheItem (


    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建用户 */
    var createUser: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户 */
    var updateUser: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 3010819206274318035L
    }

}