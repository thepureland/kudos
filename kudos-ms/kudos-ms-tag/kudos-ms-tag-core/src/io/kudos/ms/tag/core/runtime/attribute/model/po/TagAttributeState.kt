package io.kudos.ms.tag.core.runtime.attribute.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface TagAttributeState : IDbEntity<String, TagAttributeState> {
    companion object : DbEntityFactory<TagAttributeState>()
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var attributeId: String
    var valueKey: String
    var valueType: String
    var stringValue: String?
    var integerValue: Long?
    var decimalValue: BigDecimal?
    var booleanValue: Boolean?
    var dateValue: LocalDate?
    var datetimeValue: LocalDateTime?
    var sourceEventId: String
    var sourceVersion: Long?
    var stateVersion: Long
    var effectiveTime: LocalDateTime
    var expireTime: LocalDateTime?
    var updateTime: LocalDateTime
}
