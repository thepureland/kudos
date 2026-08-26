package io.kudos.ms.auth.core.authentication.credentialrevocation.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.po.AuthCredentialRevocation
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.table.AuthCredentialRevocations
import org.springframework.stereotype.Repository

@Repository
open class AuthCredentialRevocationDao :
    BaseCrudDao<String, AuthCredentialRevocation, AuthCredentialRevocations>() {

    open fun findRecent(
        tenantId: String,
        userId: String?,
        limit: Int,
    ): List<AuthCredentialRevocation> {
        val criteria = Criteria(AuthCredentialRevocation::tenantId eq tenantId)
        userId?.let { criteria.addAnd(AuthCredentialRevocation::userId eq it) }
        return pagingSearch(criteria, 1, limit, Order.desc(AuthCredentialRevocation::revokedAt.name))
    }
}
