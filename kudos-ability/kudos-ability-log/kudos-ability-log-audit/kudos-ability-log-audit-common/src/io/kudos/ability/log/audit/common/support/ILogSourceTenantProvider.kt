package io.kudos.ability.log.audit.common.support

/**
 * Resolution protocol for the audit source tenant.
 *
 * In multi-tenant scenarios, the `tenantId` in a business request may differ from
 * the "tenant the audit log should be attributed to" — for example, when a
 * platform tenant operates on behalf of a customer tenant, audits must be
 * attributed to the customer tenant. The business side implements this interface
 * to define the mapping rule.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface ILogSourceTenantProvider {
    /**
     * Resolves "the tenant the log should be attributed to" by the tenant and user
     * id from the request context.
     *
     * @param tenantId tenant id in the request context
     * @param userId current operating user id
     * @return source tenant id the audit log should be attributed to;
     *         null means use the default (fall back to tenantId)
     * @author K
     * @since 1.0.0
     */
    fun getSourceTenant(tenantId: String?, userId: String?): String?
}
