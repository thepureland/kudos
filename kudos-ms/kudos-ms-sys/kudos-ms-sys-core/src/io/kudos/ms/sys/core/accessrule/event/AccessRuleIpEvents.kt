package io.kudos.ms.sys.core.accessrule.event

/**
 * IP access rule (`sys_access_rule_ip`) domain events. Also dispatched via `@TransactionalEventListener(AFTER_COMMIT)`.
 *
 * The IP cache is aggregated by "parent rule's systemCode + tenantId", so events carry these two dimension keys directly to spare subscribers a parent-rule lookup.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysAccessRuleIpEvent {
    val id: String
    /** Parent rule dimensions */
    val parentSystemCode: String
    val parentTenantId: String?
}

/** IP rule inserted. If the parent rule is `active=false` or the IP itself is `active=false`, the cache may skip the write. */
data class SysAccessRuleIpInserted(
    override val id: String,
    override val parentSystemCode: String,
    override val parentTenantId: String?,
    val active: Boolean,
) : SysAccessRuleIpEvent

/** IP rule updated (including active-state changes). */
data class SysAccessRuleIpUpdated(
    override val id: String,
    override val parentSystemCode: String,
    override val parentTenantId: String?,
    val active: Boolean,
) : SysAccessRuleIpEvent

/** IP rule deleted. */
data class SysAccessRuleIpDeleted(
    override val id: String,
    override val parentSystemCode: String,
    override val parentTenantId: String?,
) : SysAccessRuleIpEvent

/** Batch delete: carries every affected parent-rule dimension so subscribers can refresh the IP cache by dimension key. */
data class SysAccessRuleIpBatchDeleted(
    val ids: Collection<String>,
    val dimensions: List<Pair<String, String?>>,
) : SysAccessRuleIpEvent {
    override val id: String get() = ids.first()
    /** Batch events have no single parent rule; pick the first dimension to satisfy the contract */
    override val parentSystemCode: String get() = dimensions.first().first
    override val parentTenantId: String? get() = dimensions.first().second
}
