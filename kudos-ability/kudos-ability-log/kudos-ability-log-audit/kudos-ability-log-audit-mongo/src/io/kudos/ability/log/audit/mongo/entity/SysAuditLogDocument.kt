package io.kudos.ability.log.audit.mongo.entity

import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date

/**
 * Mongo persistence document for an audit-log entry.
 *
 * Field shape mirrors [SysAuditLogVo] one-to-one with one Mongo-specific addition: the matching
 * [SysAuditDetailLogDocument] is embedded under [detail] rather than written to a separate
 * `SysAuditDetailLog` collection. This is the natural Mongo modeling for a strict 1:1 relationship
 * and avoids the cross-collection-without-transaction concern that soul's port silently inherited
 * (Mongo standalone has no multi-collection ACID).
 *
 * Collection name `sys_audit_log` follows the snake_case convention used by kudos-ability-log-audit's
 * RDB ktorm table — keeps cross-backend log inspection ergonomic.
 *
 * `operateIp` stays at `Long?` to match [SysAuditLogVo.operateIp]; soul used `BigInteger` to leave
 * room for IPv6, but kudos's IP column is committed to IPv4-range Long. If/when IPv6 is needed,
 * the kudos-base [io.kudos.ability.data.docdb.mongo.convert.BigIntegerConverters] is already wired
 * to round-trip BigInteger through BSON as a String — flip this field's type at that time.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Document(collection = "sys_audit_log")
class SysAuditLogDocument {

    @Id
    var id: String? = null

    /** Business entity id (the affected object's primary key). */
    var entityId: String? = null

    /** Operation type id (matches dictionary `op_type_id`). */
    var operateTypeId: Int? = null

    /** Operation type label (matches dictionary `op_type`). */
    var operateType: String? = null

    /** Multi-level module name (e.g. `User/Login`). */
    var moduleName: String? = null

    /** Module type code (matches dictionary `module_code`). */
    var moduleCode: String? = null

    /** Module numeric id (for fast tenant-wide module filtering). */
    var moduleId: Int? = null

    /** Operation description after placeholder substitution. */
    var description: String? = null

    /** Display name of the operator at audit time. */
    var operator: String? = null

    /** Tenant id of the audit record itself (NOT the operator's tenant). */
    var tenantId: String? = null

    /** Originating tenant when the audit was forwarded cross-tenant. */
    var sourceTenantId: String? = null

    /** Subsystem code; used for routing in cross-process audit forwarding. */
    var subSysCode: String? = null

    /** Operation timestamp. */
    var operateTime: Date? = null

    /**
     * Client IP at operation time, as Long. Matches [SysAuditLogVo.operateIp]; if the deployment
     * later moves to IPv6 (BigInteger), flip this to BigInteger and the BigIntegerConverters
     * registered by kudos-ability-data-docdb-mongo will keep precision via String storage.
     */
    var operateIp: Long? = null

    /** IP geo-region dictionary code (e.g. country / city code). */
    var operateIpDictCode: String? = null

    /** Operator id (FK to sys_user-style table; backend-defined). */
    var operatorId: String? = null

    /** User type (e.g. `sys_user`, `customer`, `partner`). */
    var operatorUserType: String? = null

    /** Client OS string. */
    var clientOs: String? = null

    /** Client browser string. */
    var clientBrowser: String? = null

    /** HTTP request method (`GET` / `POST`). */
    var requestType: String? = null

    /** Embedded detail; null when the upstream model carries no matching detail for this entity. */
    var detail: SysAuditDetailLogDocument? = null

    constructor()

    /**
     * Build a document from one entity row plus its matching detail. `tenantId` / `subSysCode`
     * follow the kudos audit convention: entity's own value first, fall back to the model-level
     * value (which is what cross-process forwarding usually populates).
     */
    constructor(entity: SysAuditLogVo, detail: SysAuditDetailLogDocument?, model: SysAuditLogModel) {
        id = entity.id
        entityId = entity.entityId
        operateTypeId = entity.operateTypeId
        operateType = entity.operateType
        moduleName = entity.moduleName
        moduleCode = entity.moduleCode
        moduleId = entity.moduleId
        description = entity.description
        operator = entity.operator
        tenantId = entity.tenantId ?: model.tenantId
        sourceTenantId = entity.sourceTenantId
        subSysCode = entity.subSysCode ?: model.subSysCode
        operateTime = entity.operateTime
        operateIp = entity.operateIp
        operateIpDictCode = entity.operateIpDictCode
        operatorId = entity.operatorId
        operatorUserType = entity.operatorUserType
        clientOs = entity.clientOs
        clientBrowser = entity.clientBrowser
        requestType = entity.requestType
        this.detail = detail
    }
}
