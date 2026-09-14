package io.kudos.ms.tag.core.catalog.tagset.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality

interface TagSet : IManagedDbEntity<String, TagSet> {
    companion object : DbEntityFactory<TagSet>()
    var tenantId: String
    var subjectType: String
    var code: String
    var name: String
    var cardinality: TagAttributeCardinality
    var defaultTagId: String?
    var version: Long
}
