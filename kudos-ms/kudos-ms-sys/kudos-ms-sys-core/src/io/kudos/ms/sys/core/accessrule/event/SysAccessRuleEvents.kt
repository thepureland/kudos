package io.kudos.ms.sys.core.accessrule.event

/**
 * Domain events for access rules (`sys_access_rule`). **Dispatched only after the database change is committed**
 * via Spring `@TransactionalEventListener(AFTER_COMMIT)`, ensuring the cache is not polluted when the transaction rolls back.
 *
 * Events carry enough dimension information (systemCode, tenantId) for cache subscribers to complete evict / refresh
 * **in place**, without needing to re-query the database.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysAccessRuleEvent {
    val id: String
}

/** Insert completion event. Subscribers should write the cache entry by id. */
data class SysAccessRuleInserted(
    override val id: String,
    val systemCode: String,
    /** `null` indicates platform level (matches `tenant_id IS NULL` in the database) */
    val tenantId: String?,
) : SysAccessRuleEvent

/**
 * Update completion event. Carries the dimension keys before and after the change so subscribers can detect dimension migration:
 * - If (beforeSystemCode, beforeTenantId) differs from (systemCode, tenantId), the old dimension must also be refreshed.
 */
data class SysAccessRuleUpdated(
    override val id: String,
    val systemCode: String,
    val tenantId: String?,
    val beforeSystemCode: String?,
    val beforeTenantId: String?,
) : SysAccessRuleEvent {
    val dimensionChanged: Boolean
        get() = beforeSystemCode != null &&
                (beforeSystemCode != systemCode || beforeTenantId != tenantId)
}

/** Delete completion event. */
data class SysAccessRuleDeleted(
    override val id: String,
    val systemCode: String,
    val tenantId: String?,
) : SysAccessRuleEvent

/**
 * Batch delete completion event. Carries the primary key collection and all affected dimension keys.
 * Note: the id field returns the first primary key just to satisfy the [SysAccessRuleEvent] contract; subscribers should use [ids] and [dimensions].
 */
data class SysAccessRuleBatchDeleted(
    val ids: Collection<String>,
    /** All affected (systemCode, tenantId) dimension pairs, deduplicated */
    val dimensions: List<Pair<String, String?>>,
) : SysAccessRuleEvent {
    override val id: String get() = ids.first()
}
