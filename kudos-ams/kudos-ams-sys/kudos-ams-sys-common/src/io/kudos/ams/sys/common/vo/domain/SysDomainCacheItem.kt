package io.kudos.ams.sys.common.vo.domain

import java.io.Serializable
import io.kudos.base.support.IIdEntity
import java.time.LocalDateTime


/**
 * 域名缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainCacheItem (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 域名 */
    var domain: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 租户id */
    var tenantId: String? = null,

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
        private const val serialVersionUID = 8344729285406513964L
    }

}