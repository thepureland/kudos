package io.kudos.ms.tag.core.rule.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.rule.model.PersistedRuleTree
import io.kudos.ms.tag.core.rule.model.po.TagRule
import io.kudos.ms.tag.core.rule.model.table.TagRules
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.notEq
import org.ktorm.entity.firstOrNull
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagRuleDao : BaseCrudDao<String, TagRule, TagRules>() {
    open fun findByTenantAndId(tenantId: String, ruleId: String): TagRule? =
        entitySequence().firstOrNull {
            (TagRules.id eq ruleId) and (TagRules.tenantId eq tenantId)
        }

    open fun listByTag(tenantId: String, tagId: String): List<TagRule> =
        entitySequence().filter {
            (TagRules.tenantId eq tenantId) and (TagRules.tagId eq tagId)
        }.sortedBy { TagRules.ruleVersion }.toList()

    open fun listNonRetired(tenantId: String): List<TagRule> =
        entitySequence().filter {
            (TagRules.tenantId eq tenantId) and (TagRules.status notEq "RETIRED")
        }.toList()

    open fun loadTree(tenantId: String, ruleId: String, version: Long): PersistedRuleTree? {
        val rule = entitySequence().firstOrNull {
            (TagRules.id eq ruleId) and (TagRules.tenantId eq tenantId) and (TagRules.ruleVersion eq version)
        } ?: return null
        val nodes = TagRuleNodeDao().listByRule(rule.id)
        return PersistedRuleTree(
            rule = rule,
            nodes = nodes,
            operands = nodes.flatMap { TagRuleOperandDao().listByNode(it.id) },
            dependencies = TagRuleDependencyDao().listByRule(rule.id),
        )
    }
}
