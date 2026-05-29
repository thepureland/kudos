package io.kudos.ability.log.audit.mongo.service

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.mongo.entity.SysAuditDetailLogDocument
import io.kudos.ability.log.audit.mongo.entity.SysAuditLogDocument
import io.kudos.ability.log.audit.mongo.repository.SysAuditLogRepository
import io.kudos.base.logger.LogFactory

/**
 * Mongo-backed [IAuditService] implementation.
 *
 * Mirrors the contract of `RdbKtormAuditService` so apps can swap backends by replacing the
 * dependency without touching aspect code:
 *  - Empty model → log + return true (no-op success, not a failure).
 *  - Persistence failure → log + return false. The audit aspect's exception handling decides
 *    whether to retry, fall back to a local file, etc.
 *  - Never throws. Audit pipeline failures must not interrupt business flow.
 *
 * Why no `@Transactional`:
 *  - Standalone Mongo (the common deployment shape) has no transaction support; the annotation
 *    would be silently inert and lull readers into a false sense of atomicity.
 *  - Replica-set Mongo would honour it, but writing N audit docs in a single insert call is
 *    already atomic per-document and the cross-doc consistency that a transaction would buy
 *    isn't load-bearing for the audit use case.
 *
 * Why a single collection (vs soul's two `@Document` classes):
 *  - The audit-to-detail relationship is strictly 1:1 in the model; embedding the detail inside
 *    the main document is the canonical Mongo modeling choice for a 1:1.
 *  - Soul's `SysAuditDetailLog @Document(collection = "...")` annotation was effectively dead
 *    code — the AuditLogMongoService only ever inserted into the main collection.
 *
 * Detail matching: the upstream `SysAuditLogModel` ships `entities` and `sysAuditDetailLogs` as
 * parallel lists keyed by `SysAuditDetailLogVo.auditId == SysAuditLogVo.id`. We build a
 * `auditId → detail` map once, then attach during the entity loop.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class MongoAuditService(
    private val repository: SysAuditLogRepository,
) : IAuditService {

    private val log = LogFactory.getLog(this::class)

    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        val entities = sysAuditLogVo.entities.orEmpty().filterNotNull()
        if (entities.isEmpty()) {
            log.debug("Audit log model carries no entities; skipping Mongo write")
            return true
        }
        return try {
            val detailMap: Map<String, SysAuditDetailLogDocument> = sysAuditLogVo.sysAuditDetailLogs
                .orEmpty()
                .filterNotNull()
                .mapNotNull { vo ->
                    val auditId = vo.auditId ?: return@mapNotNull null
                    auditId to SysAuditDetailLogDocument(vo)
                }
                .toMap()
            val docs = entities.map { entity ->
                SysAuditLogDocument(entity, detailMap[entity.id], sysAuditLogVo)
            }
            repository.insert(docs)
            true
        } catch (t: Throwable) {
            log.error(
                t,
                "Failed to persist Mongo audit log tenant={0} subSys={1} entities={2}",
                sysAuditLogVo.tenantId, sysAuditLogVo.subSysCode, entities.size,
            )
            false
        }
    }
}
