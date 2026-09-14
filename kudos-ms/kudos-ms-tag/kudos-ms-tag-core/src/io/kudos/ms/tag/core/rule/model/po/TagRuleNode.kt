package io.kudos.ms.tag.core.rule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity

interface TagRuleNode : IDbEntity<String, TagRuleNode> {
    companion object : DbEntityFactory<TagRuleNode>()
    var ruleId: String
    var parentId: String?
    var nodeKind: String
    var orderNum: Int
    var attributeId: String?
    var referencedTagId: String?
    var operator: String?
}
