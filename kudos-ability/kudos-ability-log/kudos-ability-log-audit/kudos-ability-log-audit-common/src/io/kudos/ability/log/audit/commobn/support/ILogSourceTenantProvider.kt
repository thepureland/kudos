package io.kudos.ability.log.audit.commobn.support

interface ILogSourceTenantProvider {
    fun getSourceTenant(tenantId: String?, userId: String?): String?
}
