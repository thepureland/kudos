package io.kudos.ms.tag.core.catalog.taxonomy.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.catalog.taxonomy.model.po.TagTaxonomyTag
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TagTaxonomyTags : StringIdTable<TagTaxonomyTag>("tag_taxonomy_tag") {
    val nodeId = varchar("node_id").bindTo { it.nodeId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val orderNum = int("order_num").bindTo { it.orderNum }
    val createTime = datetime("create_time").bindTo { it.createTime }
}
