package io.kudos.ms.tag.core.rule.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand
import org.ktorm.schema.boolean
import org.ktorm.schema.date
import org.ktorm.schema.datetime
import org.ktorm.schema.decimal
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagRuleOperands : StringIdTable<TagRuleOperand>("tag_rule_operand") {
    val nodeId = varchar("node_id").bindTo { it.nodeId }
    val orderNum = int("order_num").bindTo { it.orderNum }
    val valueType = varchar("value_type").bindTo { it.valueType }
    val stringValue = varchar("string_value").bindTo { it.stringValue }
    val integerValue = long("integer_value").bindTo { it.integerValue }
    val decimalValue = decimal("decimal_value").bindTo { it.decimalValue }
    val booleanValue = boolean("boolean_value").bindTo { it.booleanValue }
    val dateValue = date("date_value").bindTo { it.dateValue }
    val datetimeValue = datetime("datetime_value").bindTo { it.datetimeValue }
}
