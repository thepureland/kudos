package io.kudos.ms.tag.core.catalog.tagset.model.table

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.core.catalog.tagset.model.po.TagSet
import io.kudos.ms.tag.core.persistence.TagManagedTable
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagSets : TagManagedTable<TagSet>("tag_set") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val code = varchar("code").bindTo { it.code }
    val name = varchar("name").bindTo { it.name }
    val cardinality = varchar("cardinality").transform({ TagAttributeCardinality.valueOf(it) }, { it.name }).bindTo { it.cardinality }
    val defaultTagId = varchar("default_tag_id").bindTo { it.defaultTagId }
    val version = long("version").bindTo { it.version }
}
