package io.kudos.ms.tag.core.runtime.job.model.table

import io.kudos.ms.tag.core.runtime.job.model.po.TagRecalculationCandidate
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagRecalculationCandidates : Table<TagRecalculationCandidate>("tag_recalculation_candidate") {
    val runId = varchar("run_id").bindTo { it.runId }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val ruleVersion = long("rule_version").bindTo { it.ruleVersion }
    val evaluatedTime = datetime("evaluated_time").bindTo { it.evaluatedTime }

    val id = runId.primaryKey()
    private val subjectTypeKey = subjectType.primaryKey()
    private val subjectKey = subjectId.primaryKey()
    private val tagKey = tagId.primaryKey()
}
