package io.kudos.ms.tag.core.runtime.assignment.model.table

import io.kudos.ms.tag.core.runtime.assignment.model.po.TagManualAssignmentEvent
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object TagManualAssignmentEvents : Table<TagManualAssignmentEvent>("tag_manual_assignment_event") {
    val eventId = varchar("event_id").bindTo { it.eventId }
    val payloadChecksum = varchar("payload_checksum").bindTo { it.payloadChecksum }
    val requestId = varchar("request_id").bindTo { it.requestId }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val operation = varchar("operation").bindTo { it.operation }
    val manualSourceRef = varchar("manual_source_ref").bindTo { it.manualSourceRef }
    val operatorId = varchar("operator_id").bindTo { it.operatorId }
    val operatorName = varchar("operator_name").bindTo { it.operatorName }
    val reason = varchar("reason").bindTo { it.reason }
    val effectiveUntil = datetime("effective_until").bindTo { it.effectiveUntil }
    val occurredTime = datetime("occurred_time").bindTo { it.occurredTime }
    val receivedTime = datetime("received_time").bindTo { it.receivedTime }
    val processStatus = varchar("process_status").bindTo { it.processStatus }
    val id = eventId.primaryKey()
}
