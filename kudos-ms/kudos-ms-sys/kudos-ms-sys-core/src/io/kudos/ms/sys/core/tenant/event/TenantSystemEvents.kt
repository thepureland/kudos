package io.kudos.ms.sys.core.tenant.event

/**
 * Tenant-system relationship (`sys_tenant_system` join table) domain events.
 *
 * The cache has two projections: `tenantId` (primary key) + `systemCode` (secondary index):
 * - Relationship changes affect the `tenantId` view (which systems belong to this tenant)
 * - And also the `systemCode` view (which tenants use this system)
 *
 * Different service operations affect different dimensions; events are split so we avoid unnecessary full evictions.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysTenantSystemEvent

/** A single tenant-system relationship was created; add to cache. */
data class SysTenantSystemBound(
    val id: String,
    val tenantId: String,
    val systemCode: String,
) : SysTenantSystemEvent

/** Relationships across multiple tenant dimensions changed (unbind / delete by tenant); evict by tenant dimension. */
data class SysTenantSystemTenantsChanged(
    val tenantIds: Collection<String>,
) : SysTenantSystemEvent

/** Relationships across multiple system dimensions changed (batch bind); evict by system dimension. */
data class SysTenantSystemSystemsChanged(
    val systemCodes: Collection<String>,
) : SysTenantSystemEvent
