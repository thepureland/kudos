package io.kudos.ability.log.audit.mongo.repository

import io.kudos.ability.log.audit.mongo.entity.SysAuditLogDocument
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Spring Data Mongo repository for [SysAuditLogDocument].
 *
 * `MongoRepository` brings in the standard CRUD surface (save, findById, count, insert,
 * deleteById, etc.). The audit module doesn't need finder methods beyond CRUD — admin-side
 * search endpoints in the kudos-console don't currently query against Mongo audit records.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface SysAuditLogRepository : MongoRepository<SysAuditLogDocument, String>
