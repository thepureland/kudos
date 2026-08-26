package io.kudos.ms.auth.core.provider.invitation.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.provider.invitation.model.po.AuthExternalIdentityInvitation
import io.kudos.ms.auth.core.provider.invitation.model.table.AuthExternalIdentityInvitations
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthExternalIdentityInvitationDao :
    BaseCrudDao<String, AuthExternalIdentityInvitation, AuthExternalIdentityInvitations>() {

    open fun findByTokenHash(tokenHash: String): AuthExternalIdentityInvitation? =
        search(Criteria(AuthExternalIdentityInvitation::tokenHash eq tokenHash)).firstOrNull()

    /** Atomically claims one remaining use without allowing a revoke or replay to win too. */
    open fun consume(
        id: String,
        tenantId: String,
        identityProviderId: String,
        now: LocalDateTime,
        consumedSubjectHash: String,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_external_identity_invitation"
               set "used_count" = "used_count" + 1,
                   "active" = case when "used_count" + 1 >= "max_uses" then false else "active" end,
                   "last_used_time" = ?, "consumed_subject_hash" = ?, "update_time" = ?
               where "id" = ? and "tenant_id" = ? and "identity_provider_id" = ?
                 and "active" = true and "expires_at" > ? and "used_count" < "max_uses"
            """.trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setString(2, consumedSubjectHash)
            statement.setTimestamp(3, Timestamp.valueOf(now))
            statement.setString(4, id)
            statement.setString(5, tenantId)
            statement.setString(6, identityProviderId)
            statement.setTimestamp(7, Timestamp.valueOf(now))
            statement.executeUpdate() == 1
        }
    }

    /** Revocation competes with consumption through the same active-row predicate. */
    open fun revoke(
        id: String,
        tenantId: String,
        actorUserId: String,
        reason: String,
        now: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_external_identity_invitation"
               set "active" = false, "revoke_user_id" = ?, "revoke_reason" = ?,
                   "revoke_time" = ?, "update_time" = ?
               where "id" = ? and "tenant_id" = ? and "active" = true""".trimIndent()
        ).use { statement ->
            statement.setString(1, actorUserId)
            statement.setString(2, reason)
            statement.setTimestamp(3, Timestamp.valueOf(now))
            statement.setTimestamp(4, Timestamp.valueOf(now))
            statement.setString(5, id)
            statement.setString(6, tenantId)
            statement.executeUpdate() == 1
        }
    }
}
