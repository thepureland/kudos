package io.kudos.ms.tag.core.catalog.taxonomy.model.table

import io.kudos.ms.tag.core.catalog.taxonomy.model.po.TagTaxonomyNode
import io.kudos.ms.tag.core.persistence.TagManagedTable
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagTaxonomyNodes : TagManagedTable<TagTaxonomyNode>("tag_taxonomy_node") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val parentId = varchar("parent_id").bindTo { it.parentId }
    val code = varchar("code").bindTo { it.code }
    val name = varchar("name").bindTo { it.name }
    val orderNum = int("order_num").bindTo { it.orderNum }
    val version = long("version").bindTo { it.version }
}
