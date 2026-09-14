package io.kudos.ms.tag.core.rule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity

interface TagRuleDependency : IDbEntity<String, TagRuleDependency> {
    companion object : DbEntityFactory<TagRuleDependency>()
    var tenantId: String
    var ruleId: String
    var tagId: String
    var dependencyType: String
    var attributeId: String?
    var referencedTagId: String?
}
