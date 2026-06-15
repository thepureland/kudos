package io.kudos.ms.user.core.account.dao

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for UserAccountThirdDao — covers `searchByUserId` and `fetchByProviderSubject`, exercising
 * both the issuer-present and the issuer-IS-NULL branches plus the not-found path.
 *
 * Test data source: `UserAccountThirdDaoTest.sql`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountThirdDaoTest : RdbTestBase() {

    @Resource
    private lateinit var userAccountThirdDao: UserAccountThirdDao

    private val userId = "11111111-0000-0000-0000-000000000001"
    private val tenantId = "tenant-third-test-1"

    @Test
    fun searchByUserId() {
        val list = userAccountThirdDao.searchByUserId(userId)
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.userId == userId })

        assertTrue(userAccountThirdDao.searchByUserId("no-such-user").isEmpty())
    }

    @Test
    fun fetchByProviderSubject_withIssuer() {
        val binding = userAccountThirdDao.fetchByProviderSubject(
            tenantId = tenantId,
            accountProviderDictCode = "github",
            accountProviderIssuer = "https://github.com",
            subject = "github-user-001"
        )
        assertNotNull(binding)
        assertEquals(userId, binding.userId)

        // Wrong subject -> not found.
        assertNull(
            userAccountThirdDao.fetchByProviderSubject(tenantId, "github", "https://github.com", "no-such-subject")
        )
    }

    @Test
    fun fetchByProviderSubject_nullIssuerBranchDoesNotMatchNonNullIssuerRow() {
        // The fixture row has a non-null issuer; querying with issuer=null takes the IS NULL branch and
        // must therefore NOT match it.
        val binding = userAccountThirdDao.fetchByProviderSubject(
            tenantId = tenantId,
            accountProviderDictCode = "github",
            accountProviderIssuer = null,
            subject = "github-user-001"
        )
        assertNull(binding)
    }
}
