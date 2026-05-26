package io.kudos.ability.log.audit.rdb.ktorm.table

import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditDetailLogColumn
import org.ktorm.schema.Table
import org.ktorm.schema.text
import org.ktorm.schema.varchar

/**
 * Ktorm `Table` metadata for the audit-log detail table.
 *
 * Each main-table row ([SysAuditLogTable]) corresponds to 0..1 detail rows — during the `after` phase the aspect
 * stuffs large fields like URL / description / params JSON in here, avoiding making the main-table row too wide and
 * hurting hot-path queries.
 *
 * The `audit_id` foreign key points to [SysAuditLogTable.id] — the DDL currently does not enforce the FK constraint
 * (FK maintenance is costly at high volume); consistency is guaranteed at the application layer.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object SysAuditDetailLogTable : Table<Nothing>(AuditLogSchema.TABLE_AUDIT_DETAIL_LOG) {
    val id = varchar(AuditDetailLogColumn.ID).primaryKey()
    val auditId = varchar(AuditDetailLogColumn.AUDIT_ID)
    val operateUrl = varchar(AuditDetailLogColumn.OPERATE_URL)
    val stringParams = text(AuditDetailLogColumn.STRING_PARAMS)
    val objectParams = text(AuditDetailLogColumn.OBJECT_PARAMS)
    val requestReferer = varchar(AuditDetailLogColumn.REQUEST_REFERER)
    val requestFormData = text(AuditDetailLogColumn.REQUEST_FORM_DATA)
    val description = varchar(AuditDetailLogColumn.DESCRIPTION)
}
