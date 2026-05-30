package io.kudos.ms.auth.core.role.exclusion.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * SoD mutual-exclusion pair database entity.
 *
 * Enforces the invariant that no user may simultaneously hold both [roleAId] and [roleBId]
 * (including indirect grants via group membership or parent-chain inheritance).
 *
 * The pair is stored in canonical order ([roleAId] < [roleBId]) so a (A,B) and (B,A) duplicate
 * can never be inserted; the service layer canonicalises before any write.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRoleExclusion : IDbEntity<String, AuthRoleExclusion> {

    companion object : DbEntityFactory<AuthRoleExclusion>()

    /** Role A id (canonical: < roleB id). */
    var roleAId: String

    /** Role B id (canonical: > roleA id). */
    var roleBId: String

    /** Tenant id. Both roles must belong to this tenant. */
    var tenantId: String

    /** Human-readable description of why these roles are mutually exclusive. */
    var description: String?

    var createUserId: String?
    var createUserName: String?
    var createTime: LocalDateTime?
    var updateUserId: String?
    var updateUserName: String?
    var updateTime: LocalDateTime?
}
