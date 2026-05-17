package io.kudos.ms.sys.core.tenant.event

/**
 * 租户-系统关系（`sys_tenant_system`，join 表）领域事件。
 *
 * 缓存按 `tenantId`（主键）+ `systemCode`（副属性索引）两条投影：
 * - 关系变更影响 `tenantId` 视图（哪些 system 属于该 tenant）
 * - 也影响 `systemCode` 视图（哪些 tenant 用了该 system）
 *
 * 不同的服务操作会影响不同维度，事件层细分以减少不必要的全量淘汰。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysTenantSystemEvent

/** 单条租户-系统关系新建，新增到 cache。 */
data class SysTenantSystemBound(
    val id: String,
    val tenantId: String,
    val systemCode: String,
) : SysTenantSystemEvent

/** 多个租户维度的关系发生变更（解绑/按 tenant 删除），需按 tenant 维度淘汰。 */
data class SysTenantSystemTenantsChanged(
    val tenantIds: Collection<String>,
) : SysTenantSystemEvent

/** 多个系统维度的关系发生变更（批量绑定），需按 system 维度淘汰。 */
data class SysTenantSystemSystemsChanged(
    val systemCodes: Collection<String>,
) : SysTenantSystemEvent
