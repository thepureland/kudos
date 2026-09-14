package io.kudos.ms.tag.core.runtime.assignment.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagAssignmentEvent : IDbEntity<String, TagAssignmentEvent> {
    companion object : DbEntityFactory<TagAssignmentEvent>()
    var eventId: String
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var tagId: String
    var operation: String
    var causeType: String
    var causeRef: String?
    var assignmentVersion: Long
    var occurredTime: LocalDateTime

    override var id: String
        get() = eventId
        set(value) { eventId = value }
}
