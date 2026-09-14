package io.kudos.ms.tag.core.runtime.assignment.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagAssignment : IDbEntity<String, TagAssignment> {
    companion object : DbEntityFactory<TagAssignment>()
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var tagId: String
    var exclusiveSetId: String?
    var assignmentVersion: Long
    var evaluatedRuleVersion: Long?
    var materializedTime: LocalDateTime
    var effectiveFrom: LocalDateTime?
    var effectiveUntil: LocalDateTime?
    var updateTime: LocalDateTime

    override var id: String
        get() = tagId
        set(value) { tagId = value }
}
