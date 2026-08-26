package io.kudos.ms.auth.core.provider.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.table.AuthIdentityProviders
import org.springframework.stereotype.Repository

@Repository
open class AuthIdentityProviderDao :
    BaseCrudDao<String, AuthIdentityProvider, AuthIdentityProviders>() {

    open fun findActiveByTenantId(tenantId: String): List<AuthIdentityProvider> =
        search(
            Criteria(AuthIdentityProvider::tenantId eq tenantId)
                .addAnd(AuthIdentityProvider::active eq true)
        )

    open fun findByTenantId(tenantId: String): List<AuthIdentityProvider> =
        search(Criteria(AuthIdentityProvider::tenantId eq tenantId))

    open fun findByTenantIdAndCode(tenantId: String, code: String): AuthIdentityProvider? =
        search(
            Criteria(AuthIdentityProvider::tenantId eq tenantId)
                .addAnd(AuthIdentityProvider::code eq code)
        ).firstOrNull()

    open fun findActiveById(id: String): AuthIdentityProvider? =
        search(
            Criteria(AuthIdentityProvider::id eq id)
                .addAnd(AuthIdentityProvider::active eq true)
        ).firstOrNull()

    open fun findAllActive(): List<AuthIdentityProvider> =
        search(Criteria(AuthIdentityProvider::active eq true))
}
