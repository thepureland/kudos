package io.kudos.ms.tag.core.runtime.job.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagRecalculationJob : IDbEntity<String, TagRecalculationJob> {
    companion object : DbEntityFactory<TagRecalculationJob>()
    var jobKey: String
    var tenantId: String
    var jobType: String
    var tagId: String
    var ruleVersion: Long
    var subjectType: String
    var subjectId: String?
    var cursorSubjectId: String?
    var status: String
    var priority: Int
    var requestedVersion: Long
    var processedVersion: Long
    var attemptCount: Int
    var maxAttempts: Int
    var availableTime: LocalDateTime
    var leaseOwner: String?
    var leaseUntil: LocalDateTime?
    var processedCount: Long
    var lastErrorCode: String?
    var lastErrorMessage: String?
    var createTime: LocalDateTime
    var startTime: LocalDateTime?
    var completeTime: LocalDateTime?
    var updateTime: LocalDateTime
    var version: Long
}
