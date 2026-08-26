package io.kudos.ms.auth.core.authentication.credential.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.authentication.credential.model.po.AuthCredential
import io.kudos.ms.auth.core.authentication.credential.model.table.AuthCredentials
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthCredentialDao : BaseCrudDao<String, AuthCredential, AuthCredentials>() {

    open fun findActive(
        tenantId: String,
        userId: String,
        type: String,
    ): AuthCredential? = database().useConnection { connection ->
        connection.prepareStatement(
            """select "id", "tenant_id", "user_id", "type", "secret_hash_or_ref", "status", "version",
                      "enrolled_at", "expires_at", "last_used_at", "metadata", "revoked_at", "revoke_reason",
                      "create_time", "update_time"
               from "auth_credential"
               where "tenant_id" = ? and "user_id" = ? and "type" = ? and "status" = 'ACTIVE'""".trimIndent()
        ).use { statement ->
            statement.setString(1, tenantId)
            statement.setString(2, userId)
            statement.setString(3, type)
            statement.executeQuery().use { results ->
                if (results.next()) results.toCredential() else null
            }
        }
    }

    open fun findByUser(tenantId: String, userId: String): List<AuthCredential> =
        database().useConnection { connection ->
            connection.prepareStatement(
                """select "id", "tenant_id", "user_id", "type", "secret_hash_or_ref", "status", "version",
                          "enrolled_at", "expires_at", "last_used_at", "metadata", "revoked_at", "revoke_reason",
                          "create_time", "update_time"
                   from "auth_credential"
                   where "tenant_id" = ? and "user_id" = ?
                   order by "type", "status"""".trimIndent()
            ).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, userId)
                statement.executeQuery().use { results ->
                    buildList { while (results.next()) add(results.toCredential()) }
                }
            }
        }

    /**
     * Replaces the secret of an active credential under its version.
     *
     * The version predicate is what makes two concurrent password changes resolve to one winner instead of
     * one silently overwriting the other — the same reason the login-time hash upgrade is a conditional update.
     */
    open fun rotate(
        id: String,
        expectedVersion: Long,
        secretHashOrRef: String,
        expiresAt: LocalDateTime?,
        metadata: String?,
        updatedAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_credential"
               set "secret_hash_or_ref" = ?, "expires_at" = ?, "metadata" = ?,
                   "version" = "version" + 1, "update_time" = ?
               where "id" = ? and "version" = ? and "status" = 'ACTIVE'""".trimIndent()
        ).use { statement ->
            statement.setString(1, secretHashOrRef)
            statement.setTimestamp(2, expiresAt?.let(Timestamp::valueOf))
            statement.setString(3, metadata)
            statement.setTimestamp(4, Timestamp.valueOf(updatedAt))
            statement.setString(5, id)
            statement.setLong(6, expectedVersion)
            statement.executeUpdate() == 1
        }
    }

    open fun revoke(
        tenantId: String,
        userId: String,
        type: String,
        reason: String,
        revokedAt: LocalDateTime,
    ): Int = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_credential"
               set "status" = 'REVOKED', "revoked_at" = ?, "revoke_reason" = ?, "update_time" = ?
               where "tenant_id" = ? and "user_id" = ? and "type" = ? and "status" = 'ACTIVE'""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(revokedAt))
            statement.setString(2, reason)
            statement.setTimestamp(3, Timestamp.valueOf(revokedAt))
            statement.setString(4, tenantId)
            statement.setString(5, userId)
            statement.setString(6, type)
            statement.executeUpdate()
        }
    }

    /**
     * Records use without touching the optimistic version.
     *
     * "Last used" is an observation, not a change to the credential; letting it bump the version would make
     * every sign-in able to lose a race with a concurrent password change.
     */
    open fun recordUsage(id: String, lastUsedAt: LocalDateTime): Boolean =
        database().useConnection { connection ->
            connection.prepareStatement(
                """update "auth_credential"
                   set "last_used_at" = ?, "update_time" = ?
                   where "id" = ? and "status" = 'ACTIVE'""".trimIndent()
            ).use { statement ->
                statement.setTimestamp(1, Timestamp.valueOf(lastUsedAt))
                statement.setTimestamp(2, Timestamp.valueOf(lastUsedAt))
                statement.setString(3, id)
                statement.executeUpdate() == 1
            }
        }

    open fun deleteByUser(tenantId: String, userId: String): Int =
        database().useConnection { connection ->
            connection.prepareStatement(
                """delete from "auth_credential" where "tenant_id" = ? and "user_id" = ?"""
            ).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, userId)
                statement.executeUpdate()
            }
        }

    private fun java.sql.ResultSet.toCredential() = AuthCredential {
        id = getString(1)
        tenantId = getString(2)
        userId = getString(3)
        type = getString(4)
        secretHashOrRef = getString(5)
        status = getString(6)
        version = getLong(7)
        enrolledAt = getTimestamp(8).toLocalDateTime()
        expiresAt = getTimestamp(9)?.toLocalDateTime()
        lastUsedAt = getTimestamp(10)?.toLocalDateTime()
        metadata = getString(11)
        revokedAt = getTimestamp(12)?.toLocalDateTime()
        revokeReason = getString(13)
        createTime = getTimestamp(14).toLocalDateTime()
        updateTime = getTimestamp(15).toLocalDateTime()
    }
}
