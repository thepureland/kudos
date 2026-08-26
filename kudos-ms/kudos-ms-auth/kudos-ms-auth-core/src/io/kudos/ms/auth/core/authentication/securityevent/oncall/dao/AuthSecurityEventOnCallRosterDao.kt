package io.kudos.ms.auth.core.authentication.securityevent.oncall.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterAuditRecord
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallRosterPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.table.AuthSecurityEventOnCallRosters
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthSecurityEventOnCallRosterDao :
    BaseCrudDao<String, AuthSecurityEventOnCallRosterPo, AuthSecurityEventOnCallRosters>() {

    open fun findByTenant(tenantId: String): List<AuthSecurityEventOnCallRosterPo> =
        search(
            Criteria(AuthSecurityEventOnCallRosterPo::tenantId eq tenantId),
            Order.asc(AuthSecurityEventOnCallRosterPo::rosterCode.name),
        )

    open fun findByCode(tenantId: String, rosterCode: String): AuthSecurityEventOnCallRosterPo? =
        search(
            Criteria(AuthSecurityEventOnCallRosterPo::tenantId eq tenantId)
                .addAnd(AuthSecurityEventOnCallRosterPo::rosterCode eq rosterCode)
        ).singleOrNull()

    /** Compare-and-set on the rotation's version, so two administrators cannot interleave one week's edits. */
    open fun updateWithVersion(
        roster: AuthSecurityEventOnCallRosterPo,
        expectedVersion: Long,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event_oncall_roster"
               set "display_name" = ?, "enabled" = ?, "config_version" = "config_version" + 1,
                   "update_user_id" = ?, "update_reason" = ?, "update_time" = ?
               where "id" = ? and "tenant_id" = ? and "config_version" = ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, roster.displayName)
            statement.setBoolean(2, roster.enabled)
            statement.setString(3, roster.updateUserId)
            statement.setString(4, roster.updateReason)
            statement.setTimestamp(5, Timestamp.valueOf(roster.updateTime))
            statement.setString(6, roster.id)
            statement.setString(7, roster.tenantId)
            statement.setLong(8, expectedVersion)
            statement.executeUpdate() == 1
        }
    }

    open fun insertChangeAudit(record: AuthSecurityEventOnCallRosterAuditRecord) {
        database().useConnection { connection ->
            connection.prepareStatement(
                """insert into "auth_security_event_oncall_roster_audit"
                   ("id", "tenant_id", "roster_id", "actor_user_id", "reason", "config_version",
                    "before_snapshot", "after_snapshot", "changed_at")
                   values (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent()
            ).use { statement ->
                statement.setString(1, record.id)
                statement.setString(2, record.tenantId)
                statement.setString(3, record.rosterId)
                statement.setString(4, record.actorUserId)
                statement.setString(5, record.reason)
                statement.setLong(6, record.configVersion)
                statement.setString(7, record.beforeSnapshot)
                statement.setString(8, record.afterSnapshot)
                statement.setTimestamp(9, Timestamp.valueOf(record.changedAt))
                statement.executeUpdate()
            }
        }
    }

    /** The tenant-scoped change trail of one rotation, newest first. */
    open fun findChangeAudits(
        tenantId: String,
        rosterId: String,
        limit: Int,
    ): List<AuthSecurityEventOnCallRosterAuditRecord> = database().useConnection { connection ->
        connection.prepareStatement(
            """select "id", "tenant_id", "roster_id", "actor_user_id", "reason", "config_version",
                      "before_snapshot", "after_snapshot", "changed_at"
               from "auth_security_event_oncall_roster_audit"
               where "tenant_id" = ? and "roster_id" = ?
               order by "config_version" desc, "changed_at" desc limit ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, tenantId)
            statement.setString(2, rosterId)
            statement.setInt(3, limit)
            statement.executeQuery().use { results ->
                buildList {
                    while (results.next()) {
                        add(
                            AuthSecurityEventOnCallRosterAuditRecord(
                                id = results.getString(1),
                                tenantId = results.getString(2),
                                rosterId = results.getString(3),
                                actorUserId = results.getString(4),
                                reason = results.getString(5),
                                configVersion = results.getLong(6),
                                beforeSnapshot = results.getString(7),
                                afterSnapshot = results.getString(8),
                                changedAt = results.getTimestamp(9).toLocalDateTime(),
                            )
                        )
                    }
                }
            }
        }
    }
}
