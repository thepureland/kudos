package io.kudos.ms.tag.core.rule.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import io.kudos.ms.tag.core.rule.model.table.TagRuleNodes
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagRuleNodeDao : BaseCrudDao<String, TagRuleNode, TagRuleNodes>() {
    open fun listByRule(ruleId: String): List<TagRuleNode> =
        entitySequence().filter { TagRuleNodes.ruleId eq ruleId }
            .sortedBy { TagRuleNodes.orderNum }.toList()
}
