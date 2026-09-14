package io.kudos.ms.tag.core.runtime.assignment.model.table

import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignmentEvent
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagAssignmentEvents : Table<TagAssignmentEvent>("tag_assignment_event") {
    val eventId = varchar("event_id").bindTo { it.eventId }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val operation = varchar("operation").bindTo { it.operation }
    val causeType = varchar("cause_type").bindTo { it.causeType }
    val causeRef = varchar("cause_ref").bindTo { it.causeRef }
    val assignmentVersion = long("assignment_version").bindTo { it.assignmentVersion }
    val occurredTime = datetime("occurred_time").bindTo { it.occurredTime }
    val id = eventId.primaryKey()
}
