package io.kudos.ms.tag.core.runtime.assignment.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagManualAssignmentEvent : IDbEntity<String, TagManualAssignmentEvent> {
    companion object : DbEntityFactory<TagManualAssignmentEvent>()
    var eventId: String
    var payloadChecksum: String
    var requestId: String?
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var tagId: String
    var operation: String
    var manualSourceRef: String?
    var operatorId: String
    var operatorName: String?
    var reason: String?
    var effectiveUntil: LocalDateTime?
    var occurredTime: LocalDateTime
    var receivedTime: LocalDateTime
    var processStatus: String

    override var id: String
        get() = eventId
        set(value) { eventId = value }
}
