package io.kudos.ms.sys.common.vo.tenantlocale

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 租户-语言关系缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantLocaleCacheItem (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 租户id */
    var tenantId: String? = null,

    /** 语言代码 */
    var localeCode: String? = null,

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
        private const val serialVersionUID = 8583424316200364431L
    }

}