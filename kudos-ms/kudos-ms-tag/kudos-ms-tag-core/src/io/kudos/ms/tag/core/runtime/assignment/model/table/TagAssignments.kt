package io.kudos.ms.tag.core.runtime.assignment.model.table

import io.kudos.ms.tag.core.runtime.assignment.model.po.TagAssignment
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagAssignments : Table<TagAssignment>("tag_assignment") {
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val exclusiveSetId = varchar("exclusive_set_id").bindTo { it.exclusiveSetId }
    val assignmentVersion = long("assignment_version").bindTo { it.assignmentVersion }
    val evaluatedRuleVersion = long("evaluated_rule_version").bindTo { it.evaluatedRuleVersion }
    val materializedTime = datetime("materialized_time").bindTo { it.materializedTime }
    val effectiveFrom = datetime("effective_from").bindTo { it.effectiveFrom }
    val effectiveUntil = datetime("effective_until").bindTo { it.effectiveUntil }
    val updateTime = datetime("update_time").bindTo { it.updateTime }

    val id = tagId.primaryKey()
    private val tenantKey = tenantId.primaryKey()
    private val subjectTypeKey = subjectType.primaryKey()
    private val subjectKey = subjectId.primaryKey()
}
