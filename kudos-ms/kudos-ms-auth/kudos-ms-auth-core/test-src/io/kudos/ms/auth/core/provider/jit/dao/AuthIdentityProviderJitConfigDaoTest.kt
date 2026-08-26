package io.kudos.ms.auth.core.provider.jit.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Database mapping proof for the Provider-scoped JIT configuration introduced by auth V38. */
@EnabledIfDockerInstalled
class AuthIdentityProviderJitConfigDaoTest : RdbTestBase() {

    @Resource
    private lateinit var dao: AuthIdentityProviderJitConfigDao

    @Test
    fun readsProviderScopedJitConfiguration() {
        val config = assertNotNull(dao.get(PROVIDER_ID))

        assertEquals(TENANT_ID, config.tenantId)
        assertEquals("EMAIL_LOCAL_PART_HASHED", config.usernameStrategy)
        assertTrue(config.requireVerifiedEmail)
        assertEquals("example.com,*.partner.example", config.allowedEmailDomains)
        assertEquals("America/Argentina/Buenos_Aires", config.defaultTimezone)
        assertEquals("JPY", config.defaultCurrency)
        assertEquals("admin-1", config.updateUserId)
        assertEquals("Approved JIT defaults", config.updateReason)
    }

    private companion object {
        const val TENANT_ID = "jit-config-tenant-1"
        const val PROVIDER_ID = "e3000000-0000-0000-0000-000000000001"
    }
}
