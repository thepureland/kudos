package io.kudos.ms.tag.core.catalog.attribute.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType

interface TagAttributeDefinition : IManagedDbEntity<String, TagAttributeDefinition> {
    companion object : DbEntityFactory<TagAttributeDefinition>()
    var tenantId: String
    var subjectType: String
    var code: String
    var name: String
    var description: String?
    var valueType: TagAttributeType
    var cardinality: TagAttributeCardinality
    var version: Long
}
