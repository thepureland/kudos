package io.kudos.ms.sys.core.accessrule.event

/**
 * 访问规则（`sys_access_rule`）领域事件。**只在数据库变更已提交后**由 Spring `@TransactionalEventListener(AFTER_COMMIT)`
 * 派发，确保事务回滚时不会污染缓存。
 *
 * 事件携带足够的维度信息（systemCode、tenantId），让缓存订阅者可以**就地**完成 evict / refresh，无需再次反查 DB。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysAccessRuleEvent {
    val id: String
}

/** 新增完成事件。订阅方应按 id 把缓存项写入。 */
data class SysAccessRuleInserted(
    override val id: String,
    val systemCode: String,
    /** `null` 表示平台级（与库中 `tenant_id IS NULL` 对应） */
    val tenantId: String?,
) : SysAccessRuleEvent

/**
 * 更新完成事件。携带变更前后维度键，便于订阅方判断维度迁移：
 * - 若 (beforeSystemCode, beforeTenantId) 与 (systemCode, tenantId) 不同，需同时刷新旧维度。
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

/** 删除完成事件。 */
data class SysAccessRuleDeleted(
    override val id: String,
    val systemCode: String,
    val tenantId: String?,
) : SysAccessRuleEvent

/**
 * 批量删除完成事件。携带主键集合与涉及的所有维度键。
 * 注意：id 字段返回首个主键，仅满足 [SysAccessRuleEvent] 契约；订阅方应使用 [ids] 与 [dimensions]。
 */
data class SysAccessRuleBatchDeleted(
    val ids: Collection<String>,
    /** 受影响的所有（systemCode, tenantId）维度对，已去重 */
    val dimensions: List<Pair<String, String?>>,
) : SysAccessRuleEvent {
    override val id: String get() = ids.first()
}
