package io.kudos.ms.tag.core.catalog.tag.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

interface TagDefinition : IManagedDbEntity<String, TagDefinition> {
    companion object : DbEntityFactory<TagDefinition>()
    var tenantId: String
    var subjectType: String
    var code: String
    var name: String
    var description: String?
    var tagSetId: String?
    var setPriority: Int
    var publishedRuleId: String?
    var manualAssignable: Boolean
    var version: Long
}
