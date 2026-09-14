package io.kudos.ms.tag.core.runtime.attribute.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.runtime.attribute.model.po.TagAttributeState
import org.ktorm.schema.boolean
import org.ktorm.schema.date
import org.ktorm.schema.datetime
import org.ktorm.schema.decimal
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagAttributeStates : StringIdTable<TagAttributeState>("tag_attribute_state") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val subjectType = varchar("subject_type").bindTo { it.subjectType }
    val subjectId = varchar("subject_id").bindTo { it.subjectId }
    val attributeId = varchar("attribute_id").bindTo { it.attributeId }
    val valueKey = varchar("value_key").bindTo { it.valueKey }
    val valueType = varchar("value_type").bindTo { it.valueType }
    val stringValue = varchar("string_value").bindTo { it.stringValue }
    val integerValue = long("integer_value").bindTo { it.integerValue }
    val decimalValue = decimal("decimal_value").bindTo { it.decimalValue }
    val booleanValue = boolean("boolean_value").bindTo { it.booleanValue }
    val dateValue = date("date_value").bindTo { it.dateValue }
    val datetimeValue = datetime("datetime_value").bindTo { it.datetimeValue }
    val sourceEventId = varchar("source_event_id").bindTo { it.sourceEventId }
    val sourceVersion = long("source_version").bindTo { it.sourceVersion }
    val stateVersion = long("state_version").bindTo { it.stateVersion }
    val effectiveTime = datetime("effective_time").bindTo { it.effectiveTime }
    val expireTime = datetime("expire_time").bindTo { it.expireTime }
    val updateTime = datetime("update_time").bindTo { it.updateTime }
}
