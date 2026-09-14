package io.kudos.ms.tag.core.runtime.attribute.model.table

import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeEvent
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.date
import org.ktorm.schema.datetime
import org.ktorm.schema.decimal
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagAttributeEvents : Table<TagAttributeEvent>("tag_attribute_event") {
    val eventId = varchar("event_id").bindTo { it.eventId }
    val requestId = varchar("request_id").bindTo { it.requestId }
    val payloadChecksum = varchar("payload_checksum").bindTo { it.payloadChecksum }
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val attributeId = varchar("attribute_id").bindTo { it.attributeId }
    val operation = varchar("operation").bindTo { it.operation }
    val sourceCode = varchar("source_code").bindTo { it.sourceCode }
    val sourceVersion = long("source_version").bindTo { it.sourceVersion }
    val occurredTime = datetime("occurred_time").bindTo { it.occurredTime }
    val receivedTime = datetime("received_time").bindTo { it.receivedTime }
    val processStatus = varchar("process_status").bindTo { it.processStatus }
    val errorCode = varchar("error_code").bindTo { it.errorCode }
    val errorMessage = varchar("error_message").bindTo { it.errorMessage }
    val valueType = varchar("value_type").bindTo { it.valueType }
    val stringValue = varchar("string_value").bindTo { it.stringValue }
    val integerValue = long("integer_value").bindTo { it.integerValue }
    val decimalValue = decimal("decimal_value").bindTo { it.decimalValue }
    val booleanValue = boolean("boolean_value").bindTo { it.booleanValue }
    val dateValue = date("date_value").bindTo { it.dateValue }
    val datetimeValue = datetime("datetime_value").bindTo { it.datetimeValue }
    val id = eventId.primaryKey()
}
