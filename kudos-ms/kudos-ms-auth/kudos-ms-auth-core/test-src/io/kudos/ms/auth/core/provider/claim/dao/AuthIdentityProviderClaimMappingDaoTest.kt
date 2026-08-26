package io.kudos.ms.auth.core.provider.claim.dao

import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Database mapping proof for Provider management audit fields and typed claim paths introduced by auth V39. */
@EnabledIfDockerInstalled
class AuthIdentityProviderClaimMappingDaoTest : RdbTestBase() {

    @Resource
    private lateinit var dao: AuthIdentityProviderClaimMappingDao

    @Resource
    private lateinit var providerDao: AuthIdentityProviderDao

    @Test
    fun readsProviderAuditAndTypedClaimMapping() {
        val provider = assertNotNull(providerDao.get(PROVIDER_ID))
        assertEquals(TENANT_ID, provider.tenantId)
        assertEquals("admin-1", provider.createUserId)
        assertEquals("Register enterprise IdP", provider.updateReason)

        val mapping = assertNotNull(dao.get(PROVIDER_ID))
        assertEquals(TENANT_ID, mapping.tenantId)
        assertEquals("identity.stable_id", mapping.subjectClaims)
        assertEquals("profile.username,login", mapping.usernameClaims)
        assertEquals("profiles.0.email,email", mapping.emailClaims)
        assertEquals("profile.avatar.url", mapping.avatarClaims)
        assertEquals("admin-2", mapping.updateUserId)
        assertEquals("Approve claim contract", mapping.updateReason)
    }

    private companion object {
        const val TENANT_ID = "claim-map-tenant-1"
        const val PROVIDER_ID = "e4000000-0000-0000-0000-000000000001"
    }
}
