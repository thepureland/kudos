package io.kudos.ms.auth.core.authentication.securityevent.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.po.AuthSecurityEvent
import io.kudos.ms.auth.core.authentication.securityevent.model.table.AuthSecurityEvents
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthSecurityEventDao : BaseCrudDao<String, AuthSecurityEvent, AuthSecurityEvents>() {

    open fun incrementOccurrence(command: AuthSecurityEventRecordCommand, updatedAt: LocalDateTime): Boolean =
        database().useConnection { connection ->
            connection.prepareStatement(
                """update "auth_security_event"
                   set "occurrence_count" = "occurrence_count" + 1,
                       "first_occurred_at" = case when "first_occurred_at" > ? then ? else "first_occurred_at" end,
                       "last_occurred_at" = case when "last_occurred_at" < ? then ? else "last_occurred_at" end,
                       "update_time" = ?
                   where "tenant_id" = ? and "event_type" = ?
                     and "deduplication_key" = ? and "bucket_start" = ?""".trimIndent()
            ).use { statement ->
                val occurredAt = Timestamp.valueOf(command.occurredAt)
                statement.setTimestamp(1, occurredAt)
                statement.setTimestamp(2, occurredAt)
                statement.setTimestamp(3, occurredAt)
                statement.setTimestamp(4, occurredAt)
                statement.setTimestamp(5, Timestamp.valueOf(updatedAt))
                statement.setString(6, command.tenantId)
                statement.setString(7, command.eventType.name)
                statement.setString(8, command.deduplicationKey)
                statement.setTimestamp(9, Timestamp.valueOf(command.bucketStart))
                statement.executeUpdate() == 1
            }
        }

    open fun findRecent(
        tenantId: String,
        userId: String?,
        riskLevel: String?,
        status: String?,
        assigneeUserId: String?,
        overdueBefore: LocalDateTime?,
        limit: Int,
    ): List<AuthSecurityEvent> {
        val criteria = Criteria(AuthSecurityEvent::tenantId eq tenantId).apply {
            userId?.let { addAnd(AuthSecurityEvent::userId eq it) }
            riskLevel?.let { addAnd(AuthSecurityEvent::riskLevel eq it) }
            status?.let { addAnd(AuthSecurityEvent::status eq it) }
            assigneeUserId?.let { addAnd(AuthSecurityEvent::assignedTo eq it) }
            overdueBefore?.let {
                addAnd(AuthSecurityEvent::status.name, OperatorEnum.NE, "CLOSED")
                addAnd(AuthSecurityEvent::dueAt.name, OperatorEnum.LT, it)
            }
        }
        return pagingSearch(criteria, 1, limit, Order.desc(AuthSecurityEvent::lastOccurredAt.name))
    }

    open fun findByTenantAndId(tenantId: String, eventId: String): AuthSecurityEvent? =
        search(
            Criteria(AuthSecurityEvent::tenantId eq tenantId)
                .addAnd(AuthSecurityEvent::id eq eventId)
        ).singleOrNull()

    open fun findEscalationDue(dueAt: LocalDateTime, limit: Int): List<AuthSecurityEvent> {
        val criteria = Criteria()
            .addAnd(AuthSecurityEvent::status.name, OperatorEnum.NE, "CLOSED")
            .addAnd(AuthSecurityEvent::nextEscalationAt.name, OperatorEnum.LE, dueAt)
        return pagingSearch(criteria, 1, limit, Order.asc(AuthSecurityEvent::nextEscalationAt.name))
    }

    open fun acknowledge(
        tenantId: String,
        eventId: String,
        expectedVersion: Long,
        actorUserId: String,
        reason: String,
        occurredAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event"
               set "status" = 'ACKNOWLEDGED', "workflow_version" = "workflow_version" + 1,
                   "acknowledged_by" = ?, "acknowledged_at" = ?, "acknowledge_reason" = ?, "update_time" = ?
               where "tenant_id" = ? and "id" = ? and "status" = 'OPEN' and "workflow_version" = ?"""
                .trimIndent()
        ).use { statement ->
            statement.setString(1, actorUserId)
            statement.setTimestamp(2, Timestamp.valueOf(occurredAt))
            statement.setString(3, reason)
            statement.setTimestamp(4, Timestamp.valueOf(occurredAt))
            statement.setString(5, tenantId)
            statement.setString(6, eventId)
            statement.setLong(7, expectedVersion)
            statement.executeUpdate() == 1
        }
    }

    open fun close(
        tenantId: String,
        eventId: String,
        expectedVersion: Long,
        actorUserId: String,
        resolution: String,
        reason: String,
        occurredAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event"
               set "status" = 'CLOSED', "workflow_version" = "workflow_version" + 1,
                   "resolution" = ?, "closed_by" = ?, "closed_at" = ?, "close_reason" = ?, "update_time" = ?
               where "tenant_id" = ? and "id" = ? and "status" = 'ACKNOWLEDGED'
                 and "workflow_version" = ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, resolution)
            statement.setString(2, actorUserId)
            statement.setTimestamp(3, Timestamp.valueOf(occurredAt))
            statement.setString(4, reason)
            statement.setTimestamp(5, Timestamp.valueOf(occurredAt))
            statement.setString(6, tenantId)
            statement.setString(7, eventId)
            statement.setLong(8, expectedVersion)
            statement.executeUpdate() == 1
        }
    }

    open fun assign(
        tenantId: String,
        eventId: String,
        expectedVersion: Long,
        actorUserId: String,
        assigneeUserId: String,
        reason: String,
        occurredAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event"
               set "workflow_version" = "workflow_version" + 1,
                   "assigned_to" = ?, "assigned_by" = ?, "assigned_at" = ?,
                   "assignment_reason" = ?, "update_time" = ?
               where "tenant_id" = ? and "id" = ? and "status" <> 'CLOSED'
                 and "workflow_version" = ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, assigneeUserId)
            statement.setString(2, actorUserId)
            statement.setTimestamp(3, Timestamp.valueOf(occurredAt))
            statement.setString(4, reason)
            statement.setTimestamp(5, Timestamp.valueOf(occurredAt))
            statement.setString(6, tenantId)
            statement.setString(7, eventId)
            statement.setLong(8, expectedVersion)
            statement.executeUpdate() == 1
        }
    }

    open fun escalate(
        eventId: String,
        expectedLevel: Int,
        escalatedAt: LocalDateTime,
        nextEscalationAt: LocalDateTime?,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_security_event"
               set "escalation_level" = "escalation_level" + 1,
                   "last_escalated_at" = ?, "next_escalation_at" = ?, "update_time" = ?
               where "id" = ? and "status" <> 'CLOSED' and "escalation_level" = ?
                 and "next_escalation_at" is not null and "next_escalation_at" <= ?""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(escalatedAt))
            statement.setTimestamp(2, nextEscalationAt?.let(Timestamp::valueOf))
            statement.setTimestamp(3, Timestamp.valueOf(escalatedAt))
            statement.setString(4, eventId)
            statement.setInt(5, expectedLevel)
            statement.setTimestamp(6, Timestamp.valueOf(escalatedAt))
            statement.executeUpdate() == 1
        }
    }
}
