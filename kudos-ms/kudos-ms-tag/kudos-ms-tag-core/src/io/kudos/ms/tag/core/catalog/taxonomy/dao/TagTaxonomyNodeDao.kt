package io.kudos.ms.tag.core.catalog.taxonomy.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.catalog.taxonomy.model.po.TagTaxonomyNode
import io.kudos.ms.tag.core.catalog.taxonomy.model.table.TagTaxonomyNodes
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagTaxonomyNodeDao : BaseCrudDao<String, TagTaxonomyNode, TagTaxonomyNodes>() {
    open fun listBySubjectType(tenantId: String, subjectTypeCode: String): List<TagTaxonomyNode> =
        entitySequence().filter {
            (TagTaxonomyNodes.tenantId eq tenantId) and (TagTaxonomyNodes.subjectType eq subjectTypeCode)
        }.sortedBy { TagTaxonomyNodes.orderNum }.toList()
}
