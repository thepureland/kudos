package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.dao.AuthWebAuthnAttestationPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.po.AuthWebAuthnAttestationPolicy
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class AuthWebAuthnAttestationPolicyDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthWebAuthnAttestationPolicyDao

    @Test
    fun policyPersistsAsOneTenantScopedAuditedRecord() {
        val tenantId = UUID.randomUUID().toString()
        val now = LocalDateTime.of(2026, 8, 25, 10, 0)
        val policy = AuthWebAuthnAttestationPolicy {
            id = tenantId
            this.tenantId = tenantId
            aaguidMode = "ALLOW_LIST"
            aaguids = "00112233-4455-6677-8899-aabbccddeeff"
            allowedAttestationFormats = "none,packed"
            requireTrustedAttestation = false
            createUserId = "admin-1"
            createReason = "baseline"
            createTime = now
            updateUserId = "admin-1"
            updateReason = "baseline"
            updateTime = now
        }

        dao.insert(policy)
        val stored = dao.get(tenantId)!!

        assertEquals("ALLOW_LIST", stored.aaguidMode)
        assertEquals("none,packed", stored.allowedAttestationFormats)
        assertFalse(stored.requireTrustedAttestation)
        stored.aaguidMode = "DENY_LIST"
        stored.requireTrustedAttestation = true
        stored.updateReason = "enterprise roots installed"
        stored.updateTime = now.plusMinutes(1)
        assertTrue(dao.update(stored))
        val updated = dao.get(tenantId)!!
        assertEquals("DENY_LIST", updated.aaguidMode)
        assertTrue(updated.requireTrustedAttestation)
        assertEquals("enterprise roots installed", updated.updateReason)
    }
}
