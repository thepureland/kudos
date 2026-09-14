package io.kudos.ms.tag.core.catalog.taxonomy.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

interface TagTaxonomyNode : IManagedDbEntity<String, TagTaxonomyNode> {
    companion object : DbEntityFactory<TagTaxonomyNode>()
    var tenantId: String
    var subjectType: String
    var parentId: String?
    var code: String
    var name: String
    var orderNum: Int
    var version: Long
}
