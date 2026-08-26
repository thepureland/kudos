package io.kudos.ms.auth.core.authentication.mfa.recovery.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.authentication.mfa.recovery.model.po.AuthRecoveryCode
import io.kudos.ms.auth.core.authentication.mfa.recovery.model.table.AuthRecoveryCodes
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthRecoveryCodeDao : BaseCrudDao<String, AuthRecoveryCode, AuthRecoveryCodes>() {

    open fun findActiveSetId(tenantId: String, userId: String): String? =
        database().useConnection { connection ->
            connection.prepareStatement(
                """select "set_id" from "auth_recovery_code"
                   where "tenant_id" = ? and "user_id" = ? and "revoked_at" is null
                   order by "created_at" desc, "id" desc limit 1""".trimIndent()
            ).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, userId)
                statement.executeQuery().use { result -> if (result.next()) result.getString(1) else null }
            }
        }

    open fun countUnused(tenantId: String, userId: String, setId: String): Int =
        database().useConnection { connection ->
            connection.prepareStatement(
                """select count(*) from "auth_recovery_code"
                   where "tenant_id" = ? and "user_id" = ? and "set_id" = ?
                     and "consumed_at" is null and "revoked_at" is null""".trimIndent()
            ).use { statement ->
                statement.setString(1, tenantId)
                statement.setString(2, userId)
                statement.setString(3, setId)
                statement.executeQuery().use { result -> result.next(); result.getInt(1) }
            }
        }

    /** Exactly one concurrent request can consume a matching live code. */
    open fun consume(
        tenantId: String,
        userId: String,
        setId: String,
        codeHash: String,
        now: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_recovery_code" set "consumed_at" = ?
               where "tenant_id" = ? and "user_id" = ? and "set_id" = ? and "code_hash" = ?
                 and "consumed_at" is null and "revoked_at" is null""".trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(now))
            statement.setString(2, tenantId)
            statement.setString(3, userId)
            statement.setString(4, setId)
            statement.setString(5, codeHash)
            statement.executeUpdate() == 1
        }
    }

    open fun revokeActive(tenantId: String, userId: String, now: LocalDateTime): Int =
        database().useConnection { connection ->
            connection.prepareStatement(
                """update "auth_recovery_code" set "revoked_at" = ?
                   where "tenant_id" = ? and "user_id" = ? and "revoked_at" is null""".trimIndent()
            ).use { statement ->
                statement.setTimestamp(1, Timestamp.valueOf(now))
                statement.setString(2, tenantId)
                statement.setString(3, userId)
                statement.executeUpdate()
            }
        }

    open fun deleteByUserId(userId: String): Int =
        batchDeleteCriteria(Criteria(AuthRecoveryCode::userId eq userId))
}
