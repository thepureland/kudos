package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAuditRecord
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.po.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.table.AuthSecurityEventNotificationRoutes
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
open class AuthSecurityEventNotificationRouteDao :
    BaseCrudDao<String, AuthSecurityEventNotificationRoute, AuthSecurityEventNotificationRoutes>() {

    /** The tenant's whole rule set; small and bounded by notification type × delivery shape. */
    open fun findByTenant(tenantId: String): List<AuthSecurityEventNotificationRoute> =
        search(
            Criteria(AuthSecurityEventNotificationRoute::tenantId eq tenantId),
            Order.asc(AuthSecurityEventNotificationRoute::notificationType.name),
            Order.asc(AuthSecurityEventNotificationRoute::appliesTo.name),
        )

    open fun findByScope(
        tenantId: String,
        notificationType: String,
        appliesTo: String,
    ): AuthSecurityEventNotificationRoute? =
        search(
            Criteria(AuthSecurityEventNotificationRoute::tenantId eq tenantId)
                .addAnd(AuthSecurityEventNotificationRoute::notificationType eq notificationType)
                .addAnd(AuthSecurityEventNotificationRoute::appliesTo eq appliesTo)
        ).singleOrNull()

    /**
     * Compare-and-set on the configuration version, so a stale management page cannot silently overwrite a
     * concurrent change by another administrator.
     */
    open fun updateWithVersion(
        route: AuthSecurityEventNotificationRoute,
        expectedVersion: Long,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event_notification_route"
               set "route_code" = ?, "destination" = ?, "channels" = ?, "responder_user_ids" = ?,
                   "include_assignee" = ?, "enabled" = ?, "fallback_behavior" = ?,
                   "config_version" = "config_version" + 1,
                   "update_user_id" = ?, "update_reason" = ?, "update_time" = ?
               where "id" = ? and "tenant_id" = ? and "config_version" = ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, route.routeCode)
            statement.setString(2, route.destination)
            statement.setString(3, route.channels)
            statement.setString(4, route.responderUserIds)
            statement.setBoolean(5, route.includeAssignee)
            statement.setBoolean(6, route.enabled)
            statement.setString(7, route.fallbackBehavior)
            statement.setString(8, route.updateUserId)
            statement.setString(9, route.updateReason)
            statement.setTimestamp(10, Timestamp.valueOf(route.updateTime))
            statement.setString(11, route.id)
            statement.setString(12, route.tenantId)
            statement.setLong(13, expectedVersion)
            statement.executeUpdate() == 1
        }
    }

    /** The tenant-scoped change trail of one rule, newest first. */
    open fun findChangeAudits(
        tenantId: String,
        routeId: String,
        limit: Int,
    ): List<AuthSecurityEventNotificationRouteAuditRecord> = database().useConnection { connection ->
        connection.prepareStatement(
            """select "id", "tenant_id", "route_id", "actor_user_id", "reason", "config_version",
                      "before_snapshot", "after_snapshot", "changed_at"
               from "auth_security_event_notification_route_audit"
               where "tenant_id" = ? and "route_id" = ?
               order by "config_version" desc, "changed_at" desc limit ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, tenantId)
            statement.setString(2, routeId)
            statement.setInt(3, limit)
            statement.executeQuery().use { results ->
                buildList {
                    while (results.next()) {
                        add(
                            AuthSecurityEventNotificationRouteAuditRecord(
                                id = results.getString(1),
                                tenantId = results.getString(2),
                                routeId = results.getString(3),
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

    /** Appends the change to the tenant-scoped audit trail; the raw rule text is kept as stored. */
    open fun insertChangeAudit(record: AuthSecurityEventNotificationRouteAuditRecord) {
        database().useConnection { connection ->
            connection.prepareStatement(
                """insert into "auth_security_event_notification_route_audit"
                   ("id", "tenant_id", "route_id", "actor_user_id", "reason", "config_version",
                    "before_snapshot", "after_snapshot", "changed_at")
                   values (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent()
            ).use { statement ->
                statement.setString(1, record.id)
                statement.setString(2, record.tenantId)
                statement.setString(3, record.routeId)
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
}
