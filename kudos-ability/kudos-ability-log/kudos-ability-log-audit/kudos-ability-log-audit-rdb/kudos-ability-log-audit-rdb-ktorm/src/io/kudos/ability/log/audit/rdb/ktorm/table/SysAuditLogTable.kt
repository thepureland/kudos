package io.kudos.ability.log.audit.rdb.ktorm.table

import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditLogColumn
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * Ktorm `Table` metadata for the audit-log main table.
 *
 * No `IDbEntity` / `IntIdTable` abstraction is used here — `SysAuditLogVo` is a POJO from the upstream
 * `log-audit-common` module (not a Ktorm Entity), so this table simply holds column references and writes via
 * `database.insert(SysAuditLogTable) { set(column, value) }`.
 *
 * The primary key is supplied by the caller ([io.kudos.ability.log.audit.common.entity.SysAuditLogVo.id]); the
 * upstream aspect fills it in using the business-side ID generator.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object SysAuditLogTable : Table<Nothing>(AuditLogSchema.TABLE_AUDIT_LOG) {
    val id = varchar(AuditLogColumn.ID).primaryKey()
    val entityId = varchar(AuditLogColumn.ENTITY_ID)
    val operateTypeId = int(AuditLogColumn.OPERATE_TYPE_ID)
    val operateType = varchar(AuditLogColumn.OPERATE_TYPE)
    val moduleId = int(AuditLogColumn.MODULE_ID)
    val moduleName = varchar(AuditLogColumn.MODULE_NAME)
    val moduleCode = varchar(AuditLogColumn.MODULE_CODE)
    val description = varchar(AuditLogColumn.DESCRIPTION)
    val operator = varchar(AuditLogColumn.OPERATOR)
    val operatorId = varchar(AuditLogColumn.OPERATOR_ID)
    val operatorUserType = varchar(AuditLogColumn.OPERATOR_USER_TYPE)
    val tenantId = varchar(AuditLogColumn.TENANT_ID)
    val sourceTenantId = varchar(AuditLogColumn.SOURCE_TENANT_ID)
    val subSysCode = varchar(AuditLogColumn.SUB_SYS_CODE)
    val operateTime = datetime(AuditLogColumn.OPERATE_TIME)
    val operateIp = long(AuditLogColumn.OPERATE_IP)
    val operateIpDictCode = varchar(AuditLogColumn.OPERATE_IP_DICT_CODE)
    val clientOs = varchar(AuditLogColumn.CLIENT_OS)
    val clientBrowser = varchar(AuditLogColumn.CLIENT_BROWSER)
    val requestType = varchar(AuditLogColumn.REQUEST_TYPE)
}
