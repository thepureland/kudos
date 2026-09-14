package io.kudos.ms.tag.core.rule.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.rule.model.po.TagRuleDependency
import org.ktorm.schema.varchar

object TagRuleDependencies : StringIdTable<TagRuleDependency>("tag_rule_dependency") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val ruleId = varchar("rule_id").bindTo { it.ruleId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val dependencyType = varchar("dependency_type").bindTo { it.dependencyType }
    val attributeId = varchar("attribute_id").bindTo { it.attributeId }
    val referencedTagId = varchar("referenced_tag_id").bindTo { it.referencedTagId }
}
