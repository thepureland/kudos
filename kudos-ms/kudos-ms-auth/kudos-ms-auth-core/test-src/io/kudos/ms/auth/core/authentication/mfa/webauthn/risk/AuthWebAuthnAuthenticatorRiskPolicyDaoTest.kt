package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk

import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.dao.AuthWebAuthnAuthenticatorRiskPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.po.AuthWebAuthnAuthenticatorRiskPolicy
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class AuthWebAuthnAuthenticatorRiskPolicyDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthWebAuthnAuthenticatorRiskPolicyDao

    @Test
    fun policyPersistsAsOneTenantScopedAuditedRecord() {
        val tenantId = UUID.randomUUID().toString()
        val now = LocalDateTime.of(2026, 8, 25, 10, 0)
        val policy = AuthWebAuthnAuthenticatorRiskPolicy {
            id = tenantId
            this.tenantId = tenantId
            blockedRiskLevels = "CRITICAL"
            createUserId = "admin-1"
            createReason = "baseline"
            createTime = now
            updateUserId = "admin-1"
            updateReason = "baseline"
            updateTime = now
        }

        dao.insert(policy)
        val stored = dao.get(tenantId)!!

        assertEquals("CRITICAL", stored.blockedRiskLevels)
        stored.blockedRiskLevels = "CRITICAL,WARNING"
        stored.updateReason = "block known vulnerable models"
        stored.updateTime = now.plusMinutes(1)
        assertTrue(dao.update(stored))
        val updated = dao.get(tenantId)!!
        assertEquals("CRITICAL,WARNING", updated.blockedRiskLevels)
        assertEquals("block known vulnerable models", updated.updateReason)
    }
}
