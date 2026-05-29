package io.kudos.ms.sys.api.admin.controller.auditLog

import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import java.io.Serializable
import java.util.Date

/**
 * Flattened audit-log detail payload returned by [SysAuditLogAdminController.getDetail].
 *
 * Merges [SysAuditLogVo] (main row) and the optional [SysAuditDetailLogVo] (1:1 join on
 * `audit_id`) into a single object so the console UI's `SectionedDetailDialog` can render every
 * field through one `formatFieldValue` pass instead of switching between two nested objects.
 *
 * The detail row's `description` is intentionally NOT merged — both tables have a `description`
 * column with different semantics (main: operation summary; detail: formatted-from-POST). Keeping
 * the main row's `description` avoids surprises.
 *
 * Nulls are preserved as-is; the UI renders them as the empty placeholder.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuditLogDetailDto(
    // ----- from SysAuditLogVo (main) -----
    val id: String?,
    val entityId: String?,
    val operateTypeId: Int?,
    val operateType: String?,
    val moduleId: Int?,
    val moduleName: String?,
    val moduleCode: String?,
    val description: String?,
    val operator: String?,
    val operatorId: String?,
    val operatorUserType: String?,
    val tenantId: String?,
    val sourceTenantId: String?,
    val subSysCode: String?,
    val operateTime: Date?,
    val operateIp: Long?,
    val operateIpDictCode: String?,
    val clientOs: String?,
    val clientBrowser: String?,
    val requestType: String?,
    // ----- from SysAuditDetailLogVo (detail; null when no detail row exists) -----
    val operateUrl: String?,
    val stringParams: String?,
    val objectParams: String?,
    val requestReferer: String?,
    val requestFormData: String?,
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        fun from(main: SysAuditLogVo, detail: SysAuditDetailLogVo?): AuditLogDetailDto = AuditLogDetailDto(
            id = main.id,
            entityId = main.entityId,
            operateTypeId = main.operateTypeId,
            operateType = main.operateType,
            moduleId = main.moduleId,
            moduleName = main.moduleName,
            moduleCode = main.moduleCode,
            description = main.description,
            operator = main.operator,
            operatorId = main.operatorId,
            operatorUserType = main.operatorUserType,
            tenantId = main.tenantId,
            sourceTenantId = main.sourceTenantId,
            subSysCode = main.subSysCode,
            operateTime = main.operateTime,
            operateIp = main.operateIp,
            operateIpDictCode = main.operateIpDictCode,
            clientOs = main.clientOs,
            clientBrowser = main.clientBrowser,
            requestType = main.requestType,
            operateUrl = detail?.operateUrl,
            stringParams = detail?.stringParams,
            objectParams = detail?.objectParams,
            requestReferer = detail?.requestReferer,
            requestFormData = detail?.requestFormData,
        )
    }
}
