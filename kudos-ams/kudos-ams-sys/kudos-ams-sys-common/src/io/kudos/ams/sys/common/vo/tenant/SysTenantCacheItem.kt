package io.kudos.ams.sys.common.vo.tenant

import java.io.Serializable
import io.kudos.base.support.IIdEntity
import java.time.LocalDateTime


/**
 * 租户缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantCacheItem (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** 时区 */
    var timezone: String? = null,

    /** 默认语言编码 */
    var defaultLanguageCode: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 2728865406469746023L
    }

}