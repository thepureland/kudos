package io.kudos.ms.auth.core.token.refresh.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.token.refresh.model.po.AuthRefreshToken
import io.kudos.ms.auth.core.token.refresh.model.table.AuthRefreshTokens
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthRefreshTokenDao : BaseCrudDao<String, AuthRefreshToken, AuthRefreshTokens>() {

    open fun findByTokenHash(tokenHash: String): AuthRefreshToken? =
        search(Criteria(AuthRefreshToken::tokenHash eq tokenHash)).firstOrNull()

    /** Atomically consumes one live token. Exactly one concurrent rotation can succeed. */
    open fun consume(id: String, replacedById: String, now: LocalDateTime): Boolean =
        database().useConnection { connection ->
            connection.prepareStatement(
                """update "auth_refresh_token"
                   set "consumed_at" = ?, "replaced_by_id" = ?
                   where "id" = ? and "consumed_at" is null and "revoked_at" is null
                     and "expires_at" > ?""".trimIndent()
            ).use { statement ->
                statement.setTimestamp(1, Timestamp.valueOf(now))
                statement.setString(2, replacedById)
                statement.setString(3, id)
                statement.setTimestamp(4, Timestamp.valueOf(now))
                statement.executeUpdate() == 1
            }
        }

    open fun revokeFamily(
        familyId: String,
        reason: String,
        now: LocalDateTime,
    ): Int = revokeWhere("family_id", familyId, reason, now)

    open fun revokeBySession(
        sessionId: String,
        reason: String,
        now: LocalDateTime,
    ): Int = revokeWhere("session_id", sessionId, reason, now)

    private fun revokeWhere(
        column: String,
        value: String,
        reason: String,
        now: LocalDateTime,
    ): Int = database().useConnection { connection ->
        check(column == "family_id" || column == "session_id")
        connection.prepareStatement(
            """update "auth_refresh_token"
               set "revoked_at" = ?, "revoke_reason" = ?
               where "$column" = ? and "revoked_at" is null""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setString(2, reason)
            statement.setString(3, value)
            statement.executeUpdate()
        }
    }
}
