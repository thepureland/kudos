package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 域名缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainCacheItem (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 域名 */
    val domain: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,



    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 8344729285406513964L
    }

}