package io.kudos.ms.tag.core.rule.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TagRuleNodes : StringIdTable<TagRuleNode>("tag_rule_node") {
    val ruleId = varchar("rule_id").bindTo { it.ruleId }
    val parentId = varchar("parent_id").bindTo { it.parentId }
    val nodeKind = varchar("node_kind").bindTo { it.nodeKind }
    val orderNum = int("order_num").bindTo { it.orderNum }
    val attributeId = varchar("attribute_id").bindTo { it.attributeId }
    val referencedTagId = varchar("referenced_tag_id").bindTo { it.referencedTagId }
    val operator = varchar("operator").bindTo { it.operator }
}
