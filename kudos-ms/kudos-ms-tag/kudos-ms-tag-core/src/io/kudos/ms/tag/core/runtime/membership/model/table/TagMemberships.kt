package io.kudos.ms.tag.core.runtime.membership.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.runtime.membership.model.po.TagMembership
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagMemberships : StringIdTable<TagMembership>("tag_membership") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val sourceType = varchar("source_type").bindTo { it.sourceType }
    val sourceRef = varchar("source_ref").bindTo { it.sourceRef }
    val active = boolean("active").bindTo { it.active }
    val ruleVersion = long("rule_version").bindTo { it.ruleVersion }
    val effectiveFrom = datetime("effective_from").bindTo { it.effectiveFrom }
    val effectiveUntil = datetime("effective_until").bindTo { it.effectiveUntil }
    val membershipVersion = long("membership_version").bindTo { it.membershipVersion }
    val sourceEventId = varchar("source_event_id").bindTo { it.sourceEventId }
    val updateTime = datetime("update_time").bindTo { it.updateTime }
}
