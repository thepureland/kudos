package io.kudos.ms.tag.core.catalog.taxonomy.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

interface TagTaxonomyTag : IDbEntity<String, TagTaxonomyTag> {
    companion object : DbEntityFactory<TagTaxonomyTag>()
    var nodeId: String
    var tagId: String
    var orderNum: Int
    var createTime: LocalDateTime
}
