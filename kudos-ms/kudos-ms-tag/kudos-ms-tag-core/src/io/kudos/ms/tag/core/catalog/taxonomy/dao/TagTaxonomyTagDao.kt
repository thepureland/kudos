package io.kudos.ms.tag.core.catalog.taxonomy.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.catalog.taxonomy.model.po.TagTaxonomyTag
import io.kudos.ms.tag.core.catalog.taxonomy.model.table.TagTaxonomyTags
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagTaxonomyTagDao : BaseCrudDao<String, TagTaxonomyTag, TagTaxonomyTags>() {
    open fun listByNode(nodeId: String): List<TagTaxonomyTag> =
        entitySequence().filter { TagTaxonomyTags.nodeId eq nodeId }
            .sortedBy { TagTaxonomyTags.orderNum }.toList()
}
