package io.kudos.ms.auth.core.authentication.mfa.policy.exemption.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.sort.Order
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.po.AuthMfaEnrollmentExemption
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.table.AuthMfaEnrollmentExemptions
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthMfaEnrollmentExemptionDao :
    BaseCrudDao<String, AuthMfaEnrollmentExemption, AuthMfaEnrollmentExemptions>() {

    /**
     * Active, not-yet-expired grants for one user.
     *
     * Returns a list rather than a single row on purpose: two administrators granting at the same moment is a
     * benign race, and tolerating it here is cheaper — and less surprising — than a filtered unique index the
     * database cannot express portably. The effective expiry is simply the latest one.
     */
    open fun findActive(
        tenantId: String,
        userId: String,
        now: LocalDateTime,
    ): List<AuthMfaEnrollmentExemption> = database().useConnection { connection ->
        connection.prepareStatement(
            """select "id", "tenant_id", "user_id", "status", "reason", "granted_by", "granted_at",
                      "expires_at", "revoked_by", "revoke_reason", "revoked_at"
               from "auth_mfa_enrollment_exemption"
               where "tenant_id" = ? and "user_id" = ? and "status" = 'ACTIVE' and "expires_at" > ?
               order by "expires_at" desc""".trimIndent()
        ).use { statement ->
            statement.setString(1, tenantId)
            statement.setString(2, userId)
            statement.setTimestamp(3, Timestamp.valueOf(now))
            statement.executeQuery().use { results ->
                buildList {
                    while (results.next()) add(results.toExemption())
                }
            }
        }
    }

    open fun findRecent(
        tenantId: String,
        userId: String?,
        limit: Int,
    ): List<AuthMfaEnrollmentExemption> {
        val criteria = Criteria(AuthMfaEnrollmentExemption::tenantId eq tenantId)
        userId?.let { criteria.addAnd(AuthMfaEnrollmentExemption::userId eq it) }
        return pagingSearch(criteria, 1, limit, Order.desc(AuthMfaEnrollmentExemption::grantedAt.name))
    }

    /**
     * Revokes every grant of one user that is actually in force, in a single statement.
     *
     * Revoking all of them is what an operator means by "take this away"; leaving a concurrently granted second
     * row active would silently keep the exemption alive. Grants that already ran out are deliberately left
     * alone: marking one `REVOKED` would record that an administrator withdrew it when in truth it simply
     * expired, and the row is the only account of what happened.
     *
     * @return how many grants this call revoked
     */
    open fun revokeActive(
        tenantId: String,
        userId: String,
        actorUserId: String,
        reason: String,
        revokedAt: LocalDateTime,
    ): Int = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_mfa_enrollment_exemption"
               set "status" = 'REVOKED', "revoked_by" = ?, "revoke_reason" = ?, "revoked_at" = ?
               where "tenant_id" = ? and "user_id" = ? and "status" = 'ACTIVE' and "expires_at" > ?""".trimIndent()
        ).use { statement ->
            statement.setString(1, actorUserId)
            statement.setString(2, reason)
            statement.setTimestamp(3, Timestamp.valueOf(revokedAt))
            statement.setString(4, tenantId)
            statement.setString(5, userId)
            statement.setTimestamp(6, Timestamp.valueOf(revokedAt))
            statement.executeUpdate()
        }
    }

    private fun java.sql.ResultSet.toExemption() = AuthMfaEnrollmentExemption {
        id = getString(1)
        tenantId = getString(2)
        userId = getString(3)
        status = getString(4)
        reason = getString(5)
        grantedBy = getString(6)
        grantedAt = getTimestamp(7).toLocalDateTime()
        expiresAt = getTimestamp(8).toLocalDateTime()
        revokedBy = getString(9)
        revokeReason = getString(10)
        revokedAt = getTimestamp(11)?.toLocalDateTime()
    }
}
