package io.kudos.ms.auth.core.authentication.securityevent.oncall.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallShiftPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.table.AuthSecurityEventOnCallShifts
import org.springframework.stereotype.Repository

@Repository
open class AuthSecurityEventOnCallShiftDao :
    BaseCrudDao<String, AuthSecurityEventOnCallShiftPo, AuthSecurityEventOnCallShifts>() {

    open fun findByTenant(tenantId: String): List<AuthSecurityEventOnCallShiftPo> =
        search(
            Criteria(AuthSecurityEventOnCallShiftPo::tenantId eq tenantId),
            Order.asc(AuthSecurityEventOnCallShiftPo::startAt.name),
            Order.asc(AuthSecurityEventOnCallShiftPo::tier.name),
        )

    /**
     * Clears a rotation before it is rewritten.
     *
     * Scoped by tenant as well as roster: the roster id already implies the tenant, and saying so in the
     * predicate keeps a mistaken id from ever reaching another tenant's rows.
     */
    open fun deleteByRoster(tenantId: String, rosterId: String): Int =
        database().useConnection { connection ->
            connection.prepareStatement(
                """delete from "auth_security_event_oncall_shift"
                   where "tenant_id" = ? and "roster_id" = ?""".trimIndent()
            ).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, rosterId)
                statement.executeUpdate()
            }
        }
}
