package io.kudos.ability.log.audit.rdb.clickhouse.table

import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditLogColumn
import org.ktorm.schema.Table
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * Ktorm `Table` metadata for the ClickHouse audit-log main table.
 *
 * Intentionally duplicated from `kudos-ability-log-audit-rdb-ktorm.SysAuditLogTable` rather than
 * shared via a deeper common module. Reason:
 *  - Both modules persist to the same logical schema (column names live in [AuditLogSchema]).
 *  - The ktorm module's `RdbKtormAuditService` is a sibling implementation of `IAuditService`;
 *    pulling its module here would land two `IAuditService` beans in the same app context, which
 *    forces every consumer to add a `@Qualifier`. The 20-line duplicate avoids that ergonomic
 *    landmine for the common case (ClickHouse-only deployments).
 *
 * No `PRIMARY KEY` is declared (`primaryKey()` removed vs the ktorm-module copy) — ClickHouse
 * does not enforce PK; `id` simply participates in `ORDER BY` per the engine DDL.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object SysAuditLogTable : Table<Nothing>(AuditLogSchema.TABLE_AUDIT_LOG) {
    val id = varchar(AuditLogColumn.ID)
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
