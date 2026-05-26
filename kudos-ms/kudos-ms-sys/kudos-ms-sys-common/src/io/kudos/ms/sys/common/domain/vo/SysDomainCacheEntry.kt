package io.kudos.ms.sys.common.domain.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Domain cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysDomainCacheEntry (

    /** Primary key */
    override val id: String,

    /** Domain */
    val domain: String,

    /** System code */
    val systemCode: String,

    /** Tenant id; `null` means platform-level (maps to `tenant_id IS NULL` in the DB) */
    val tenantId: String?,

    /** Remark */
    val remark: String?,

    /** Whether active */
    val active: Boolean,

    /** Whether built-in */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8344729285406513964L
    }

}
