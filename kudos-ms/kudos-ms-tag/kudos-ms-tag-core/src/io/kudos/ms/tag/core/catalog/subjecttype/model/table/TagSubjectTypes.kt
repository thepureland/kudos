package io.kudos.ms.tag.core.catalog.subjecttype.model.table

import io.kudos.ms.tag.core.catalog.subjecttype.model.po.TagSubjectType
import io.kudos.ms.tag.core.persistence.TagManagedTable
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagSubjectTypes : TagManagedTable<TagSubjectType>("tag_subject_type") {
    val code = varchar("code").bindTo { it.code }
    val name = varchar("name").bindTo { it.name }
    val ownerServiceCode = varchar("owner_service_code").bindTo { it.ownerServiceCode }
    val description = varchar("description").bindTo { it.description }
    val version = long("version").bindTo { it.version }
}
