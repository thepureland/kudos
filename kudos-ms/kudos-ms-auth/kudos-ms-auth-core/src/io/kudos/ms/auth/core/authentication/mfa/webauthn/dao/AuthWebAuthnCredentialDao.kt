package io.kudos.ms.auth.core.authentication.mfa.webauthn.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.table.AuthWebAuthnCredentials
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
open class AuthWebAuthnCredentialDao :
    BaseCrudDao<String, AuthWebAuthnCredential, AuthWebAuthnCredentials>() {

    open fun findByCredentialId(tenantId: String, credentialId: String): AuthWebAuthnCredential? =
        search(
            Criteria(AuthWebAuthnCredential::tenantId eq tenantId)
                .addAnd(AuthWebAuthnCredential::credentialId eq credentialId)
        ).singleOrNull()

    open fun findActiveByCredentialId(tenantId: String, credentialId: String): AuthWebAuthnCredential? =
        search(activeCriteria(tenantId).addAnd(AuthWebAuthnCredential::credentialId eq credentialId)).singleOrNull()

    open fun findActiveByUser(tenantId: String, userId: String): List<AuthWebAuthnCredential> =
        search(activeCriteria(tenantId).addAnd(AuthWebAuthnCredential::userId eq userId))

    open fun findByUser(tenantId: String, userId: String): List<AuthWebAuthnCredential> =
        search(
            Criteria(AuthWebAuthnCredential::tenantId eq tenantId)
                .addAnd(AuthWebAuthnCredential::userId eq userId)
        )

    open fun findActiveByUserHandle(tenantId: String, userHandle: String): List<AuthWebAuthnCredential> =
        search(activeCriteria(tenantId).addAnd(AuthWebAuthnCredential::userHandle eq userHandle))

    open fun hasActive(tenantId: String, userId: String): Boolean =
        count(activeCriteria(tenantId).addAnd(AuthWebAuthnCredential::userId eq userId)) > 0

    /**
     * Persists state returned by a verified assertion. The old counter is part of the predicate so
     * two concurrent assertions cannot both advance the same credential snapshot.
     */
    open fun updateAssertionState(
        tenantId: String,
        userId: String,
        credentialId: String,
        expectedSignatureCount: Long,
        newSignatureCount: Long,
        backedUp: Boolean,
        usedAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_webauthn_credential"
               set "signature_count" = ?, "backed_up" = ?, "last_used_at" = ?, "version" = "version" + 1
               where "tenant_id" = ? and "user_id" = ? and "credential_id" = ?
                 and "signature_count" = ? and "revoked_at" is null""".trimIndent()
        ).use { statement ->
            statement.setLong(1, newSignatureCount)
            statement.setBoolean(2, backedUp)
            statement.setTimestamp(3, Timestamp.valueOf(usedAt))
            statement.setString(4, tenantId)
            statement.setString(5, userId)
            statement.setString(6, credentialId)
            statement.setLong(7, expectedSignatureCount)
            statement.executeUpdate() == 1
        }
    }

    open fun rename(
        tenantId: String,
        userId: String,
        credentialId: String,
        displayName: String,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_webauthn_credential" set "display_name" = ?, "version" = "version" + 1
               where "tenant_id" = ? and "user_id" = ? and "credential_id" = ? and "revoked_at" is null"""
                .trimIndent()
        ).use { statement ->
            statement.setString(1, displayName)
            statement.setString(2, tenantId)
            statement.setString(3, userId)
            statement.setString(4, credentialId)
            statement.executeUpdate() == 1
        }
    }

    open fun revoke(
        tenantId: String,
        userId: String,
        credentialId: String,
        revokedAt: LocalDateTime,
    ): Boolean = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_webauthn_credential" set "revoked_at" = ?, "version" = "version" + 1
               where "tenant_id" = ? and "user_id" = ? and "credential_id" = ? and "revoked_at" is null"""
                .trimIndent()
        ).use { statement ->
            statement.setTimestamp(1, Timestamp.valueOf(revokedAt))
            statement.setString(2, tenantId)
            statement.setString(3, userId)
            statement.setString(4, credentialId)
            statement.executeUpdate() == 1
        }
    }

    open fun deleteByUserId(userId: String): Int =
        batchDeleteCriteria(Criteria(AuthWebAuthnCredential::userId eq userId))

    private fun activeCriteria(tenantId: String) =
        Criteria(AuthWebAuthnCredential::tenantId eq tenantId)
            .addAnd(AuthWebAuthnCredential::revokedAt.name, OperatorEnum.IS_NULL, null)
}
