package io.kudos.ms.sys.core.accessrule.event

/**
 * IP 访问规则（`sys_access_rule_ip`）领域事件。同样由 `@TransactionalEventListener(AFTER_COMMIT)` 派发。
 *
 * IP 缓存按「父规则的 systemCode + tenantId」聚合，所以事件直接携带这两个维度键，避免订阅方反查父规则。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysAccessRuleIpEvent {
    val id: String
    /** 父规则维度 */
    val parentSystemCode: String
    val parentTenantId: String?
}

/** IP 规则新增。若父规则 `active=false` 或 IP 自身 `active=false`，缓存可选择不写入。 */
data class SysAccessRuleIpInserted(
    override val id: String,
    override val parentSystemCode: String,
    override val parentTenantId: String?,
    val active: Boolean,
) : SysAccessRuleIpEvent

/** IP 规则更新（含 active 状态变更）。 */
data class SysAccessRuleIpUpdated(
    override val id: String,
    override val parentSystemCode: String,
    override val parentTenantId: String?,
    val active: Boolean,
) : SysAccessRuleIpEvent

/** IP 规则删除。 */
data class SysAccessRuleIpDeleted(
    override val id: String,
    override val parentSystemCode: String,
    override val parentTenantId: String?,
) : SysAccessRuleIpEvent

/** 批量删除：携带涉及的所有父规则维度，订阅方按维度键刷新 IP 缓存。 */
data class SysAccessRuleIpBatchDeleted(
    val ids: Collection<String>,
    val dimensions: List<Pair<String, String?>>,
) : SysAccessRuleIpEvent {
    override val id: String get() = ids.first()
    /** 批量事件没有单一父规则，取首维度满足契约 */
    override val parentSystemCode: String get() = dimensions.first().first
    override val parentTenantId: String? get() = dimensions.first().second
}
