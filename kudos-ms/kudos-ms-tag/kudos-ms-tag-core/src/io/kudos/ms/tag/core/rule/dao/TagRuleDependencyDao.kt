package io.kudos.ms.tag.core.rule.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.tag.core.rule.model.po.TagRuleDependency
import io.kudos.ms.tag.core.rule.model.table.TagRuleDependencies
import org.ktorm.dsl.eq
import org.ktorm.entity.filter
import org.ktorm.entity.sortedBy
import org.ktorm.entity.toList
import org.springframework.stereotype.Repository

@Repository
open class TagRuleDependencyDao : BaseCrudDao<String, TagRuleDependency, TagRuleDependencies>() {
    open fun listByRule(ruleId: String): List<TagRuleDependency> =
        entitySequence().filter { TagRuleDependencies.ruleId eq ruleId }
            .sortedBy { TagRuleDependencies.id }.toList()

    open fun listByTenant(tenantId: String): List<TagRuleDependency> =
        entitySequence().filter { TagRuleDependencies.tenantId eq tenantId }.toList()
}
