package io.kudos.ms.sys.common.tenant.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable

/**
 * Tenant-system relation cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantSystemCacheEntry(
    /** Primary key */
    override val id: String,
    /** Tenant id */
    val tenantId: String,
    /** System code */
    val systemCode: String
) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
