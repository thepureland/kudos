package io.kudos.ms.sys.common.vo.domain

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 域名缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 域名 */
    val domain: String = "",

    /** 系统编码 */
    val systemCode: String = "",

    /** 租户id */
    val tenantId: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 8344729285406513964L
    }

}