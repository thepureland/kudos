package io.kudos.ms.tag.core.runtime.attribute.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface TagAttributeEvent : IDbEntity<String, TagAttributeEvent> {
    companion object : DbEntityFactory<TagAttributeEvent>()
    var eventId: String
    var requestId: String?
    var payloadChecksum: String
    var tenantId: String
    var subjectType: String
    var subjectId: String
    var attributeId: String
    var operation: String
    var sourceCode: String
    var sourceVersion: Long?
    var occurredTime: LocalDateTime
    var receivedTime: LocalDateTime
    var processStatus: String
    var errorCode: String?
    var errorMessage: String?
    var valueType: String?
    var stringValue: String?
    var integerValue: Long?
    var decimalValue: BigDecimal?
    var booleanValue: Boolean?
    var dateValue: LocalDate?
    var datetimeValue: LocalDateTime?

    override var id: String
        get() = eventId
        set(value) { eventId = value }
}
