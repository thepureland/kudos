package io.kudos.ability.log.audit.common.support

interface ILogSourceTenantProvider {
    fun getSourceTenant(tenantId: String?, userId: String?): String?
}
