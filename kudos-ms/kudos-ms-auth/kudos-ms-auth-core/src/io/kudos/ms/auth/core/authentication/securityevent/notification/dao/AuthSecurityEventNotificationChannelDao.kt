package io.kudos.ms.auth.core.authentication.securityevent.notification.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotificationChannel
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.table.AuthSecurityEventNotificationChannels
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthSecurityEventNotificationChannelDao :
    BaseCrudDao<String, AuthSecurityEventNotificationChannel, AuthSecurityEventNotificationChannels>() {

    open fun findByNotification(notificationId: String): List<AuthSecurityEventNotificationChannel> =
        database().useConnection { connection ->
            connection.prepareStatement(
                """select "id", "tenant_id", "notification_id", "channel", "status", "attempt_count",
                          "last_error_code", "settled_at"
                   from "auth_security_event_notification_channel"
                   where "notification_id" = ?
                   order by "channel"""".trimIndent()
            ).use { statement ->
                statement.setString(1, notificationId)
                statement.executeQuery().use { results ->
                    buildList {
                        while (results.next()) {
                            add(
                                AuthSecurityEventNotificationChannel {
                                    id = results.getString(1)
                                    tenantId = results.getString(2)
                                    // Qualified: the enclosing function's parameter shadows the property.
                                    this.notificationId = results.getString(3)
                                    channel = results.getString(4)
                                    status = results.getString(5)
                                    attemptCount = results.getInt(6)
                                    lastErrorCode = results.getString(7)
                                    settledAt = results.getTimestamp(8).toLocalDateTime()
                                }
                            )
                        }
                    }
                }
            }
        }

    /**
     * Records a terminal channel outcome, tolerating the row already being there.
     *
     * A worker that crashed between publishing and settling comes back to find the same channel pending; the
     * unique constraint is what makes the second settle harmless instead of a duplicate ledger entry, and the
     * first recorded outcome is the one that stands.
     *
     * @return true when this call wrote the outcome, false when it was already settled
     */
    open fun settleIfAbsent(
        id: String,
        tenantId: String,
        notificationId: String,
        channel: String,
        status: String,
        attemptCount: Int,
        errorCode: String?,
        settledAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """insert into "auth_security_event_notification_channel"
               ("id", "tenant_id", "notification_id", "channel", "status", "attempt_count",
                "last_error_code", "settled_at")
               select ?, ?, ?, ?, ?, ?, ?, ?
               where not exists (
                   select 1 from "auth_security_event_notification_channel"
                   where "notification_id" = ? and "channel" = ?
               )""".trimIndent()
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, tenantId)
            statement.setString(3, notificationId)
            statement.setString(4, channel)
            statement.setString(5, status)
            statement.setInt(6, attemptCount)
            statement.setString(7, errorCode)
            statement.setTimestamp(8, Timestamp.valueOf(settledAt))
            statement.setString(9, notificationId)
            statement.setString(10, channel)
            statement.executeUpdate() == 1
        }
    }

    /**
     * Clears the ledger of one notification so an administrator's replay starts from a clean slate.
     *
     * Replay exists because somebody decided the previous outcome was wrong — including a channel that was
     * permanently refused — so carrying the old settlements forward would make the replay a no-op.
     */
    open fun deleteByNotification(tenantId: String, notificationId: String): Int =
        database().useConnection { connection ->
            connection.prepareStatement(
                """delete from "auth_security_event_notification_channel"
                   where "tenant_id" = ? and "notification_id" = ?""".trimIndent()
            ).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, notificationId)
                statement.executeUpdate()
            }
        }
}
