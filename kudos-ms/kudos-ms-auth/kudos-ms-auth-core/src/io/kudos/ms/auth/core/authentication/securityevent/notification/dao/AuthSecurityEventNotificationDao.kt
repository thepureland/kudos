package io.kudos.ms.auth.core.authentication.securityevent.notification.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.table.AuthSecurityEventNotifications
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthSecurityEventNotificationDao :
    BaseCrudDao<String, AuthSecurityEventNotification, AuthSecurityEventNotifications>() {

    open fun findClaimableIds(now: LocalDateTime, limit: Int): List<String> =
        database().useConnection { connection ->
            connection.prepareStatement(
                """select "id" from "auth_security_event_notification"
                   where "next_attempt_at" <= ?
                     and ("status" = 'PENDING' or ("status" = 'PROCESSING' and "lease_until" < ?))
                   order by "next_attempt_at", "create_time", "id" limit ?""".trimIndent()
            ).use { statement ->
                statement.setTimestamp(1, Timestamp.valueOf(now))
                statement.setTimestamp(2, Timestamp.valueOf(now))
                statement.setInt(3, limit)
                statement.executeQuery().use { results ->
                    buildList {
                        while (results.next()) add(results.getString(1))
                    }
                }
            }
        }

    open fun claim(
        id: String,
        workerId: String,
        now: LocalDateTime,
        leaseUntil: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event_notification"
               set "status" = 'PROCESSING', "attempt_count" = "attempt_count" + 1,
                   "lease_owner" = ?, "lease_until" = ?, "update_time" = ?
               where "id" = ? and "next_attempt_at" <= ?
                 and ("status" = 'PENDING' or ("status" = 'PROCESSING' and "lease_until" < ?))""".trimIndent()
        ).use { statement ->
            statement.setString(1, workerId)
            statement.setTimestamp(2, Timestamp.valueOf(leaseUntil))
            statement.setTimestamp(3, Timestamp.valueOf(now))
            statement.setString(4, id)
            statement.setTimestamp(5, Timestamp.valueOf(now))
            statement.setTimestamp(6, Timestamp.valueOf(now))
            statement.executeUpdate() == 1
        }
    }

    open fun complete(id: String, workerId: String, deliveredAt: LocalDateTime): Boolean =
        updateDeliveryState(id, workerId, "DELIVERED", null, null, deliveredAt, deliveredAt)

    open fun fail(
        id: String,
        workerId: String,
        status: String,
        nextAttemptAt: LocalDateTime?,
        errorCode: String,
        failedAt: LocalDateTime,
    ): Boolean = updateDeliveryState(id, workerId, status, nextAttemptAt, errorCode, null, failedAt)

    open fun findDead(tenantId: String, eventId: String?, limit: Int): List<AuthSecurityEventNotification> {
        val criteria = Criteria(AuthSecurityEventNotification::tenantId eq tenantId)
            .addAnd(AuthSecurityEventNotification::status eq "DEAD")
        eventId?.let { criteria.addAnd(AuthSecurityEventNotification::eventId eq it) }
        return pagingSearch(criteria, 1, limit, Order.desc(AuthSecurityEventNotification::updateTime.name))
    }

    open fun findByTenantAndId(tenantId: String, notificationId: String): AuthSecurityEventNotification? =
        search(
            Criteria(AuthSecurityEventNotification::tenantId eq tenantId)
                .addAnd(AuthSecurityEventNotification::id eq notificationId)
        ).singleOrNull()

    open fun replay(
        tenantId: String,
        notificationId: String,
        replayedAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event_notification"
               set "status" = 'PENDING', "attempt_count" = 0, "next_attempt_at" = ?,
                   "lease_owner" = null, "lease_until" = null, "delivered_at" = null,
                   "last_error_code" = null, "replay_count" = "replay_count" + 1, "update_time" = ?
               where "tenant_id" = ? and "id" = ? and "status" = 'DEAD'""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(replayedAt))
            statement.setTimestamp(2, Timestamp.valueOf(replayedAt))
            statement.setString(3, tenantId)
            statement.setString(4, notificationId)
            statement.executeUpdate() == 1
        }
    }

    open fun insertReplayAudit(
        id: String,
        tenantId: String,
        notificationId: String,
        actorUserId: String,
        reason: String,
        replayedAt: LocalDateTime,
    ) {
        database().useConnection { connection ->
            connection.prepareStatement(
                """insert into "auth_security_event_notification_replay"
                   ("id", "tenant_id", "notification_id", "actor_user_id", "reason", "replayed_at")
                   values (?, ?, ?, ?, ?, ?)""".trimIndent()
            ).use { statement ->
                statement.setString(1, id)
                statement.setString(2, tenantId)
                statement.setString(3, notificationId)
                statement.setString(4, actorUserId)
                statement.setString(5, reason)
                statement.setTimestamp(6, Timestamp.valueOf(replayedAt))
                statement.executeUpdate()
            }
        }
    }

    private fun updateDeliveryState(
        id: String,
        workerId: String,
        status: String,
        nextAttemptAt: LocalDateTime?,
        errorCode: String?,
        deliveredAt: LocalDateTime?,
        updatedAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event_notification"
               set "status" = ?, "next_attempt_at" = ?, "lease_owner" = null, "lease_until" = null,
                   "delivered_at" = ?, "last_error_code" = ?, "update_time" = ?
               where "id" = ? and "status" = 'PROCESSING' and "lease_owner" = ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, status)
            statement.setTimestamp(2, nextAttemptAt?.let(Timestamp::valueOf))
            statement.setTimestamp(3, deliveredAt?.let(Timestamp::valueOf))
            statement.setString(4, errorCode)
            statement.setTimestamp(5, Timestamp.valueOf(updatedAt))
            statement.setString(6, id)
            statement.setString(7, workerId)
            statement.executeUpdate() == 1
        }
    }
}
