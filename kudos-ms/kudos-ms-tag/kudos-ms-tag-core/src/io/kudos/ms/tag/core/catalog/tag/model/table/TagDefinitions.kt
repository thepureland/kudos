package io.kudos.ms.tag.core.catalog.tag.model.table

import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.persistence.TagManagedTable
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagDefinitions : TagManagedTable<TagDefinition>("tag_definition") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val code = varchar("code").bindTo { it.code }
    val name = varchar("name").bindTo { it.name }
    val description = varchar("description").bindTo { it.description }
    val tagSetId = varchar("tag_set_id").bindTo { it.tagSetId }
    val setPriority = int("set_priority").bindTo { it.setPriority }
    val publishedRuleId = varchar("published_rule_id").bindTo { it.publishedRuleId }
    val manualAssignable = boolean("manual_assignable").bindTo { it.manualAssignable }
    val version = long("version").bindTo { it.version }
}
