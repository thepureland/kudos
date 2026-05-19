package io.kudos.ability.log.audit.rdb.ktorm.table

import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditDetailLogColumn
import org.ktorm.schema.Table
import org.ktorm.schema.text
import org.ktorm.schema.varchar

/**
 * 审计日志详情表的 Ktorm `Table` 元数据。
 *
 * 一条主表记录（[SysAuditLogTable]）对应 0..1 条详情——切面在 `after` 阶段会把
 * URL / 描述 / 参数 JSON 等大字段塞到这里，避免主表行变得过宽影响热路径查询。
 *
 * `audit_id` 外键指向 [SysAuditLogTable.id]——目前 DDL 不强制外键约束（业务量大时
 * FK 维护成本高），由应用层保证一致性。
 *
 * @author K
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
