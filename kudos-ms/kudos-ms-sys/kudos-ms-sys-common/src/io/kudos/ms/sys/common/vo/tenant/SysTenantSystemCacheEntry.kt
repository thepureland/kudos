package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable

/**
 * 租户-系统关系缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSystemCacheEntry(
    /** 主键 */
    override val id: String = "",
    /** 租户id */
    val tenantId: String = "",
    /** 系统编码 */
    val systemCode: String = ""
) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}