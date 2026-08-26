package io.kudos.ms.auth.core.authentication.mfa.webauthn

import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class AuthWebAuthnCredentialDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthWebAuthnCredentialDao

    @Test
    fun assertionCounterCasAndRevocationAreTenantAndSubjectScoped() {
        val suffix = UUID.randomUUID().toString()
        val tenantId = "t-${suffix.take(30)}"
        val userId = "u-${suffix.take(30)}"
        val credentialId = encoded("credential-$suffix")
        val now = LocalDateTime.of(2026, 8, 25, 10, 0)
        dao.insert(credential(tenantId, userId, credentialId, now))

        assertTrue(dao.hasActive(tenantId, userId))
        assertFalse(dao.hasActive("other-tenant", userId))
        assertEquals(userId, dao.findActiveByUserHandle(tenantId, encoded("handle-$userId")).single().userId)
        assertTrue(dao.findActiveByUserHandle("other-tenant", encoded("handle-$userId")).isEmpty())
        assertFalse(dao.updateAssertionState(tenantId, "other-user", credentialId, 1, 2, false, now.plusSeconds(1)))
        assertTrue(dao.updateAssertionState(tenantId, userId, credentialId, 1, 2, true, now.plusSeconds(1)))
        assertFalse(dao.updateAssertionState(tenantId, userId, credentialId, 1, 3, true, now.plusSeconds(2)))
        val updated = dao.findActiveByCredentialId(tenantId, credentialId)!!
        assertEquals(2L, updated.signatureCount)
        assertEquals(1L, updated.version)
        assertTrue(updated.backedUp)

        assertFalse(dao.rename(tenantId, "other-user", credentialId, "Other laptop"))
        assertTrue(dao.rename(tenantId, userId, credentialId, "Work laptop"))
        val renamed = dao.findActiveByCredentialId(tenantId, credentialId)!!
        assertEquals("Work laptop", renamed.displayName)
        assertEquals(2L, renamed.version)

        assertFalse(dao.revoke("other-tenant", userId, credentialId, now.plusMinutes(1)))
        assertTrue(dao.revoke(tenantId, userId, credentialId, now.plusMinutes(1)))
        assertNull(dao.findActiveByCredentialId(tenantId, credentialId))
        assertEquals(3L, dao.findByCredentialId(tenantId, credentialId)?.version)
        assertEquals(1, dao.findByUser(tenantId, userId).size)
        assertTrue(dao.findByUser("other-tenant", userId).isEmpty())
        assertTrue(dao.findByUser(tenantId, "other-user").isEmpty())
    }

    private fun credential(
        tenantId: String,
        userId: String,
        credentialId: String,
        now: LocalDateTime,
    ) = AuthWebAuthnCredential {
        id = UUID.randomUUID().toString()
        this.tenantId = tenantId
        this.userId = userId
        this.credentialId = credentialId
        userHandle = encoded("handle-$userId")
        publicKeyCose = encoded("public-key")
        signatureCount = 1
        transports = "internal,usb"
        aaguid = null
        attestationFormat = "none"
        backupEligible = true
        backedUp = false
        discoverable = true
        displayName = "Passkey"
        createdAt = now
        lastUsedAt = null
        revokedAt = null
        version = 0
    }

    private fun encoded(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}
