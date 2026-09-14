package io.kudos.ms.tag.core.runtime.subject.model.table

import io.kudos.ms.tag.core.runtime.subject.model.po.TagSubject
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object TagSubjects : Table<TagSubject>("tag_subject") {
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val displayName = varchar("display_name").bindTo { it.displayName }
    val profileJson = text("profile_json").bindTo { it.profileJson }
    val stateVersion = long("state_version").bindTo { it.stateVersion }
    val firstSeenTime = datetime("first_seen_time").bindTo { it.firstSeenTime }
    val updateTime = datetime("update_time").bindTo { it.updateTime }

    val id = subjectId.primaryKey()
    private val tenantKey = tenantId.primaryKey()
    private val subjectTypeKey = subjectType.primaryKey()
}
