package io.kudos.ability.log.audit.mongo.entity

import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo

/**
 * Mongo-side sub-document carrying the per-entity detail of an audit record.
 *
 * Embedded inside [SysAuditLogDocument.detail] rather than persisted to its own collection. Soul's
 * port declared a `@Document(collection = "SysAuditDetailLog")` on this class but never actually
 * wrote to that collection — the detail was always nested inside the main doc. We drop the dead
 * annotation here and lean fully into the nested form, which is the natural Mongo modeling
 * choice for a 1:1 audit-to-detail relationship.
 *
 * Mirrors [SysAuditDetailLogVo] field-for-field so a [SysAuditDetailLogVo] can be copied into a
 * [SysAuditDetailLogDocument] without any field renames or conversions.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class SysAuditDetailLogDocument {

    /** Detail primary key (mirrors [SysAuditDetailLogVo.id]). */
    var id: String? = null

    /** Audit-log primary key this detail belongs to. */
    var auditId: String? = null

    /** Full operation URL. */
    var operateUrl: String? = null

    /** String-form parameters that map into `{0}` placeholders of the description template. */
    var stringParams: String? = null

    /** JSON-form parameters that map into `${...}` placeholders. */
    var objectParams: String? = null

    /** `Referer` header value at the time of the operation. */
    var requestReferer: String? = null

    /** Raw POST/form payload. */
    var requestFormData: String? = null

    /** Human-readable description (after placeholder substitution). */
    var description: String? = null

    constructor()

    constructor(vo: SysAuditDetailLogVo) {
        id = vo.id
        auditId = vo.auditId
        operateUrl = vo.operateUrl
        stringParams = vo.stringParams
        objectParams = vo.objectParams
        requestReferer = vo.requestReferer
        requestFormData = vo.requestFormData
        description = vo.description
    }
}
