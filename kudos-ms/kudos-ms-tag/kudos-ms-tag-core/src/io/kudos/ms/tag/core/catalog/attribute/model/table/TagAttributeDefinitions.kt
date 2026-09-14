package io.kudos.ms.tag.core.catalog.attribute.model.table

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.persistence.TagManagedTable
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagAttributeDefinitions : TagManagedTable<TagAttributeDefinition>("tag_attribute_definition") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val code = varchar("code").bindTo { it.code }
    val name = varchar("name").bindTo { it.name }
    val description = varchar("description").bindTo { it.description }
    val valueType = varchar("value_type").transform({ TagAttributeType.valueOf(it) }, { it.name }).bindTo { it.valueType }
    val cardinality = varchar("cardinality").transform({ TagAttributeCardinality.valueOf(it) }, { it.name }).bindTo { it.cardinality }
    val version = long("version").bindTo { it.version }
}
