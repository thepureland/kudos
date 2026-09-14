package io.kudos.ms.tag.core.runtime.job.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationJob
import io.kudos.ms.tag.core.runtime.job.model.table.TagRecalculationJobs
import io.kudos.ms.tag.core.runtime.rdb.RecalculationLeaseDialect
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.filter
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDateTime

@Repository
open class TagRecalculationJobDao : BaseCrudDao<String, TagRecalculationJob, TagRecalculationJobs>() {
    open fun insertJob(job: TagRecalculationJob): Boolean = database().insert(TagRecalculationJobs) {
        set(TagRecalculationJobs.id, job.id)
        set(TagRecalculationJobs.jobKey, job.jobKey)
        set(TagRecalculationJobs.tenantId, job.tenantId)
        set(TagRecalculationJobs.jobType, job.jobType)
        set(TagRecalculationJobs.tagId, job.tagId)
        set(TagRecalculationJobs.ruleVersion, job.ruleVersion)
        set(TagRecalculationJobs.subjectType, job.subjectType)
        set(TagRecalculationJobs.subjectId, job.subjectId)
        set(TagRecalculationJobs.cursorSubjectId, job.cursorSubjectId)
        set(TagRecalculationJobs.status, job.status)
        set(TagRecalculationJobs.priority, job.priority)
        set(TagRecalculationJobs.requestedVersion, job.requestedVersion)
        set(TagRecalculationJobs.processedVersion, job.processedVersion)
        set(TagRecalculationJobs.attemptCount, job.attemptCount)
        set(TagRecalculationJobs.maxAttempts, job.maxAttempts)
        set(TagRecalculationJobs.availableTime, job.availableTime)
        set(TagRecalculationJobs.leaseOwner, job.leaseOwner)
        set(TagRecalculationJobs.leaseUntil, job.leaseUntil)
        set(TagRecalculationJobs.processedCount, job.processedCount)
        set(TagRecalculationJobs.lastErrorCode, job.lastErrorCode)
        set(TagRecalculationJobs.lastErrorMessage, job.lastErrorMessage)
        set(TagRecalculationJobs.createTime, job.createTime)
        set(TagRecalculationJobs.startTime, job.startTime)
        set(TagRecalculationJobs.completeTime, job.completeTime)
        set(TagRecalculationJobs.updateTime, job.updateTime)
        set(TagRecalculationJobs.version, job.version)
    } == 1

    open fun findByJobKey(jobKey: String): TagRecalculationJob? =
        entitySequence().firstOrNull { TagRecalculationJobs.jobKey eq jobKey }

    open fun listByTenant(tenantId: String): List<TagRecalculationJob> =
        entitySequence().filter { TagRecalculationJobs.tenantId eq tenantId }.toList()

    open fun databaseProductName(): String = database().useConnection { it.metaData.databaseProductName }

    open fun failExhaustedLeases(now: LocalDateTime): Int = database().useConnection { connection ->
        connection.prepareStatement(
            """update tag_recalculation_job set status = 'FAILED', lease_owner = null, lease_until = null,
                complete_time = ?, last_error_code = 'LEASE_EXPIRED_MAX_ATTEMPTS',
                last_error_message = 'Lease expired after the maximum number of attempts.',
                update_time = ?, version = version + 1
                where status = 'RUNNING' and lease_until <= ? and attempt_count >= max_attempts""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setTimestamp(2, Timestamp.valueOf(now))
            statement.setTimestamp(3, Timestamp.valueOf(now))
            statement.executeUpdate()
        }
    }

    open fun requestAgain(id: String, requestedVersion: Long, now: LocalDateTime): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update tag_recalculation_job set
                requested_version = case when requested_version + 1 > ? then requested_version + 1 else ? end,
                status = case when status = 'RUNNING' then status else 'PENDING' end,
                available_time = case when status = 'RUNNING' then available_time else ? end,
                lease_owner = case when status = 'RUNNING' then lease_owner else null end,
                lease_until = case when status = 'RUNNING' then lease_until else null end,
                attempt_count = case when status in ('SUCCEEDED', 'FAILED', 'CANCELLED') then 0 else attempt_count end,
                complete_time = ?, update_time = ?,
                version = case when status = 'RUNNING' then version else version + 1 end
                where id = ?""".trimIndent()
        ).use { statement ->
            statement.setLong(1, requestedVersion)
            statement.setLong(2, requestedVersion)
            statement.setTimestamp(3, Timestamp.valueOf(now))
            statement.setNull(4, Types.TIMESTAMP)
            statement.setTimestamp(5, Timestamp.valueOf(now))
            statement.setString(6, id)
            statement.executeUpdate() == 1
        }
    }

    open fun lease(
        dialect: RecalculationLeaseDialect,
        workerId: String,
        now: LocalDateTime,
        leaseUntil: LocalDateTime,
        limit: Int,
    ): List<String> = database().useConnection { connection ->
        dialect.selectCandidateIds(connection, now, limit).filter { id ->
            connection.prepareStatement(
                """update tag_recalculation_job set status = 'RUNNING', lease_owner = ?, lease_until = ?,
                    attempt_count = attempt_count + 1, start_time = coalesce(start_time, ?), update_time = ?, version = version + 1
                    where id = ? and attempt_count < max_attempts and (
                        (status in ('PENDING', 'RETRY_WAIT') and available_time <= ?)
                        or (status = 'RUNNING' and lease_until <= ?)
                    )""".trimIndent()
            ).use { statement ->
                statement.setString(1, workerId)
                statement.setTimestamp(2, Timestamp.valueOf(leaseUntil))
                statement.setTimestamp(3, Timestamp.valueOf(now))
                statement.setTimestamp(4, Timestamp.valueOf(now))
                statement.setString(5, id)
                statement.setTimestamp(6, Timestamp.valueOf(now))
                statement.setTimestamp(7, Timestamp.valueOf(now))
                statement.executeUpdate() == 1
            }
        }
    }

    open fun complete(
        id: String, workerId: String, leaseVersion: Long, processedVersion: Long, processedCount: Long, now: LocalDateTime,
    ): Boolean = completeTransition(id, workerId, leaseVersion, processedVersion, processedCount, now, true) ||
        completeTransition(id, workerId, leaseVersion, processedVersion, processedCount, now, false)

    private fun completeTransition(
        id: String,
        workerId: String,
        leaseVersion: Long,
        processedVersion: Long,
        processedCount: Long,
        now: LocalDateTime,
        successful: Boolean,
    ): Boolean = database().useConnection { connection ->
        val requestedPredicate = if (successful) "requested_version <= ?" else "requested_version > ?"
        connection.prepareStatement(
            """update tag_recalculation_job set processed_version = ?, processed_count = processed_count + ?,
                status = ?, available_time = ?, lease_owner = null, lease_until = null, complete_time = ?,
                last_error_code = null, last_error_message = null, update_time = ?, version = version + 1
                where id = ? and status = 'RUNNING' and lease_owner = ? and version = ? and lease_until > ?
                    and processed_version <= ? and requested_version >= ? and $requestedPredicate""".trimIndent()
        ).use { statement ->
            statement.setLong(1, processedVersion)
            statement.setLong(2, processedCount)
            statement.setString(3, if (successful) "SUCCEEDED" else "PENDING")
            statement.setTimestamp(4, Timestamp.valueOf(now))
            if (successful) statement.setTimestamp(5, Timestamp.valueOf(now)) else statement.setNull(5, Types.TIMESTAMP)
            statement.setTimestamp(6, Timestamp.valueOf(now))
            statement.setString(7, id)
            statement.setString(8, workerId)
            statement.setLong(9, leaseVersion)
            statement.setTimestamp(10, Timestamp.valueOf(now))
            statement.setLong(11, processedVersion)
            statement.setLong(12, processedVersion)
            statement.setLong(13, processedVersion)
            statement.executeUpdate() == 1
        }
    }

    open fun fail(
        id: String, workerId: String, leaseVersion: Long, errorCode: String, errorMessage: String?,
        now: LocalDateTime, retryAt: LocalDateTime, terminal: Boolean,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update tag_recalculation_job set
                status = ?, available_time = ?, complete_time = ?,
                lease_owner = null, lease_until = null, last_error_code = ?, last_error_message = ?,
                update_time = ?, version = version + 1
                where id = ? and status = 'RUNNING' and lease_owner = ? and version = ? and lease_until > ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, if (terminal) "FAILED" else "RETRY_WAIT")
            statement.setTimestamp(2, Timestamp.valueOf(if (terminal) now else retryAt))
            if (terminal) statement.setTimestamp(3, Timestamp.valueOf(now)) else statement.setNull(3, Types.TIMESTAMP)
            statement.setString(4, errorCode)
            statement.setString(5, errorMessage)
            statement.setTimestamp(6, Timestamp.valueOf(now))
            statement.setString(7, id)
            statement.setString(8, workerId)
            statement.setLong(9, leaseVersion)
            statement.setTimestamp(10, Timestamp.valueOf(now))
            statement.executeUpdate() == 1
        }
    }

    open fun cancel(id: String, now: LocalDateTime): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update tag_recalculation_job set status = 'CANCELLED', lease_owner = null, lease_until = null,
                complete_time = ?, update_time = ?, version = version + 1
                where id = ? and status in ('PENDING', 'RUNNING', 'RETRY_WAIT')""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setTimestamp(2, Timestamp.valueOf(now))
            statement.setString(3, id)
            statement.executeUpdate() == 1
        }
    }

    open fun advanceFullRebuild(
        id: String,
        workerId: String,
        cursorSubjectId: String?,
        processedCount: Long,
        exhausted: Boolean,
        now: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update tag_recalculation_job set cursor_subject_id = ?, processed_count = processed_count + ?,
                status = ?, lease_owner = ?, lease_until = ?, update_time = ?, version = version + 1
                where id = ? and job_type = 'RULE_FULL_REBUILD' and status = 'RUNNING' and lease_owner = ?
                    and lease_until > ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, cursorSubjectId)
            statement.setLong(2, processedCount)
            statement.setString(3, if (exhausted) "RUNNING" else "PENDING")
            statement.setString(4, if (exhausted) workerId else null)
            if (exhausted) statement.setTimestamp(5, get(id)?.leaseUntil?.let(Timestamp::valueOf)) else statement.setNull(5, Types.TIMESTAMP)
            statement.setTimestamp(6, Timestamp.valueOf(now))
            statement.setString(7, id)
            statement.setString(8, workerId)
            statement.setTimestamp(9, Timestamp.valueOf(now))
            statement.executeUpdate() == 1
        }
    }

    open fun completePromotion(id: String, now: LocalDateTime): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update tag_recalculation_job set status = 'SUCCEEDED', processed_version = requested_version,
                lease_owner = null, lease_until = null, complete_time = ?, update_time = ?, version = version + 1
                where id = ? and job_type = 'RULE_FULL_REBUILD' and status = 'RUNNING'""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setTimestamp(2, Timestamp.valueOf(now))
            statement.setString(3, id)
            statement.executeUpdate() == 1
        }
    }
}
