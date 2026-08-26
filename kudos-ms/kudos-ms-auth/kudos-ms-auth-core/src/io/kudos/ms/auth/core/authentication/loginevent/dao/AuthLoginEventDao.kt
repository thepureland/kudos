package io.kudos.ms.auth.core.authentication.loginevent.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.loginevent.model.po.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.model.table.AuthLoginEvents
import org.springframework.stereotype.Repository

@Repository
open class AuthLoginEventDao : BaseCrudDao<String, AuthLoginEvent, AuthLoginEvents>() {

    /**
     * Newest first, always scoped to one tenant.
     *
     * An attempt that never resolved a tenant is recorded with a null `tenant_id` and is therefore invisible
     * here. That is deliberate: it cannot be attributed to anyone, so showing it to a tenant administrator
     * would put other tenants' noise — and their attempted usernames' hashes — in front of them. Such rows
     * remain available for platform-level analysis directly against the table.
     */
    open fun findRecent(
        tenantId: String,
        userId: String?,
        identifierHash: String?,
        successOnly: Boolean?,
        limit: Int,
    ): List<AuthLoginEvent> {
        val criteria = Criteria(AuthLoginEvent::tenantId eq tenantId)
        userId?.let { criteria.addAnd(AuthLoginEvent::userId eq it) }
        identifierHash?.let { criteria.addAnd(AuthLoginEvent::identifierHash eq it) }
        successOnly?.let { criteria.addAnd(AuthLoginEvent::success eq it) }
        return pagingSearch(criteria, 1, limit, Order.desc(AuthLoginEvent::occurredAt.name))
    }

    open fun findByTransaction(transactionId: String): AuthLoginEvent? =
        search(Criteria(AuthLoginEvent::transactionId eq transactionId)).singleOrNull()
}
