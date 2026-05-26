package io.kudos.ms.sys.common.accessrule.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Value object for access rules in distributed/local cache (mirrors the core fields of table `sys_access_rule`).
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleCacheEntry (

    /** Primary key. */
    override val id: String,

    /**
     * Tenant id; an **empty string** denotes platform-level (corresponds to `tenant_id IS NULL` in the DB), consistent with the hash secondary index value.
     */
    val tenantId: String,

    /** System code (sub-system / system dimension `system_code`). */
    val systemCode: String,

    /** Rule type dictionary code. */
    val accessRuleTypeDictCode: String,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8253788046293050901L
    }

}
