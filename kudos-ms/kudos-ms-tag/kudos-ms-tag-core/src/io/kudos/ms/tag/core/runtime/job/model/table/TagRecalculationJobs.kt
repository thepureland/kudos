package io.kudos.ms.tag.core.runtime.job.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationJob
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagRecalculationJobs : StringIdTable<TagRecalculationJob>("tag_recalculation_job") {
    val jobKey = varchar("job_key").bindTo { it.jobKey }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val jobType = varchar("job_type").bindTo { it.jobType }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val ruleVersion = long("rule_version").bindTo { it.ruleVersion }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val cursorSubjectId = varchar("cursor_subject_id").bindTo { it.cursorSubjectId }
    val status = varchar("status").bindTo { it.status }
    val priority = int("priority").bindTo { it.priority }
    val requestedVersion = long("requested_version").bindTo { it.requestedVersion }
    val processedVersion = long("processed_version").bindTo { it.processedVersion }
    val attemptCount = int("attempt_count").bindTo { it.attemptCount }
    val maxAttempts = int("max_attempts").bindTo { it.maxAttempts }
    val availableTime = datetime("available_time").bindTo { it.availableTime }
    val leaseOwner = varchar("lease_owner").bindTo { it.leaseOwner }
    val leaseUntil = datetime("lease_until").bindTo { it.leaseUntil }
    val processedCount = long("processed_count").bindTo { it.processedCount }
    val lastErrorCode = varchar("last_error_code").bindTo { it.lastErrorCode }
    val lastErrorMessage = varchar("last_error_message").bindTo { it.lastErrorMessage }
    val createTime = datetime("create_time").bindTo { it.createTime }
    val startTime = datetime("start_time").bindTo { it.startTime }
    val completeTime = datetime("complete_time").bindTo { it.completeTime }
    val updateTime = datetime("update_time").bindTo { it.updateTime }
    val version = long("version").bindTo { it.version }
}
