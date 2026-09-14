package io.kudos.ms.tag.core.rule.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand
import io.kudos.ms.tag.core.rule.model.table.TagRuleOperands
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagRuleOperandDao : BaseCrudDao<String, TagRuleOperand, TagRuleOperands>() {
    open fun listByNode(nodeId: String): List<TagRuleOperand> =
        entitySequence().filter { TagRuleOperands.nodeId eq nodeId }
            .sortedBy { TagRuleOperands.orderNum }.toList()
}
