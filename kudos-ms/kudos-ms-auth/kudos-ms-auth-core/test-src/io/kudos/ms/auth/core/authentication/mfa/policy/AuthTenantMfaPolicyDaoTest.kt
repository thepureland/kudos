package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.core.authentication.mfa.policy.dao.AuthTenantMfaPolicyDao
import io.kudos.ms.auth.core.authentication.mfa.policy.model.po.AuthTenantMfaPolicy
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class AuthTenantMfaPolicyDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var dao: AuthTenantMfaPolicyDao

    @Test
    fun persistsAndUpdatesOnePolicyPerTenant() {
        val tenantId = UUID.randomUUID().toString()
        val now = LocalDateTime.of(2026, 8, 25, 10, 0)
        val policy = policy(tenantId, now)

        dao.insert(policy)
        val loaded = dao.get(tenantId)!!
        assertEquals("CONDITIONAL", loaded.mode)
        assertEquals("STAFF", loaded.requiredAccountTypeCodes)
        assertEquals("SECURITY_ADMIN", loaded.requiredRoleCodes)

        loaded.mode = "REQUIRED"
        loaded.updateReason = "raise assurance"
        loaded.updateTime = now.plusMinutes(1)
        assertTrue(dao.update(loaded))
        assertEquals("REQUIRED", dao.get(tenantId)?.mode)
        assertEquals("raise assurance", dao.get(tenantId)?.updateReason)
    }

    private fun policy(tenantId: String, now: LocalDateTime) = AuthTenantMfaPolicy {
        id = tenantId
        this.tenantId = tenantId
        mode = "CONDITIONAL"
        gracePeriodDays = 7
        allowedMethods = "TOTP"
        recoveryCodesEnabled = true
        requiredAccountTypeCodes = "STAFF"
        requiredRoleCodes = "SECURITY_ADMIN"
        createUserId = "admin-1"
        createReason = "create"
        createTime = now
        updateUserId = "admin-1"
        updateReason = "create"
        updateTime = now
    }
}
