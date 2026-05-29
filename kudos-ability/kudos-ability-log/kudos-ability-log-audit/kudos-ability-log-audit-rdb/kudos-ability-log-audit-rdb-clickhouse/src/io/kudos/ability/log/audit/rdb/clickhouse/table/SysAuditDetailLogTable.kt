package io.kudos.ability.log.audit.rdb.clickhouse.table

import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditDetailLogColumn
import org.ktorm.schema.Table
import org.ktorm.schema.text
import org.ktorm.schema.varchar

/**
 * Ktorm `Table` metadata for the ClickHouse audit-log detail table.
 *
 * Same duplication rationale as [SysAuditLogTable] — duplicate the column wiring to keep this
 * module pulling in only `audit-rdb-common`, not the `audit-rdb-ktorm` jar.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object SysAuditDetailLogTable : Table<Nothing>(AuditLogSchema.TABLE_AUDIT_DETAIL_LOG) {
    val id = varchar(AuditDetailLogColumn.ID)
    val auditId = varchar(AuditDetailLogColumn.AUDIT_ID)
    val operateUrl = varchar(AuditDetailLogColumn.OPERATE_URL)
    val stringParams = text(AuditDetailLogColumn.STRING_PARAMS)
    val objectParams = text(AuditDetailLogColumn.OBJECT_PARAMS)
    val requestReferer = varchar(AuditDetailLogColumn.REQUEST_REFERER)
    val requestFormData = text(AuditDetailLogColumn.REQUEST_FORM_DATA)
    val description = varchar(AuditDetailLogColumn.DESCRIPTION)
}
