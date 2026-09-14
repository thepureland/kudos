package io.kudos.ms.tag.core.runtime.job.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagRecalculationCandidate : IDbEntity<String, TagRecalculationCandidate> {
    companion object : DbEntityFactory<TagRecalculationCandidate>()
    var runId: String
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var tagId: String
    var ruleVersion: Long
    var evaluatedTime: LocalDateTime

    override var id: String
        get() = runId
        set(value) { runId = value }
}
