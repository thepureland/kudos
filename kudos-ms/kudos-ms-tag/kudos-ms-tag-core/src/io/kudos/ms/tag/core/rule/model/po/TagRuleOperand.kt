package io.kudos.ms.tag.core.rule.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface TagRuleOperand : IDbEntity<String, TagRuleOperand> {
    companion object : DbEntityFactory<TagRuleOperand>()
    var nodeId: String
    var orderNum: Int
    var valueType: String
    var stringValue: String?
    var integerValue: Long?
    var decimalValue: BigDecimal?
    var booleanValue: Boolean?
    var dateValue: LocalDate?
    var datetimeValue: LocalDateTime?
}
