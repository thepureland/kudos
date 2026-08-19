package io.kudos.ms.user.client.init

import io.kudos.ms.user.client.org.proxy.IUserOrgProxy
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.api.IUserOrgApi
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trip test for the user interface clients against a controller implementing the same
 * `IUserOrgApi` contract from `kudos-ms-user-common`.
 *
 * Same purpose as `SysClientContractTest`: the risky part of this migration is moving the mapping
 * annotations in `-common` from Spring MVC to the `@HttpExchange` family, and only a real HTTP
 * round-trip proves the shared declaration still works as a server-side route *and* as a client
 * contract. The mock controller declares no mapping annotations of its own.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=${UserClientContractTest.PORT}",
        "spring.http.serviceclient.${UserClientAutoConfiguration.GROUP}.base-url=http://localhost:${UserClientContractTest.PORT}",
    ]
)
@Import(UserClientContractTest.MockOrgController::class)
open class UserClientContractTest {

    @Autowired
    private lateinit var orgProxy: IUserOrgProxy

    @Test
    fun `GET with one unnamed RequestParam round-trips`() {
        val org = orgProxy.getOrgById("org-1")
        assertEquals("org-1", org?.id)
        assertEquals("Kudos HQ", org?.name)
    }

    @Test
    fun `GET with two unnamed RequestParams round-trips`() {
        assertTrue(orgProxy.isUserInOrg("u-1", "org-1"))
        assertTrue(!orgProxy.isUserInOrg("u-2", "org-1"))
    }

    @Test
    fun `POST with a RequestBody collection and a Map return round-trips`() {
        val orgs = orgProxy.getOrgsByIds(listOf("org-1", "org-2"))
        assertEquals(setOf("org-1", "org-2"), orgs.keys)
        assertEquals("Kudos HQ", orgs["org-1"]?.name)
    }

    @Test
    fun `GET returning a list round-trips`() {
        assertEquals(listOf("org-1", "org-2"), orgProxy.getOrgIds("t-1"))
    }

    @Test
    fun `GET returning null round-trips as null`() {
        assertNull(orgProxy.getParentOrg("org-1"))
    }

    /** Mock provider; routes come solely from `@GetExchange` / `@PostExchange` on [IUserOrgApi]. */
    @RestController
    open class MockOrgController : IUserOrgApi {

        override fun getOrgById(id: String): UserOrgCacheEntry? = org(id)

        override fun getOrgsByIds(ids: Collection<String>): Map<String, UserOrgCacheEntry> =
            ids.associateWith { org(it) }

        override fun getOrgIds(tenantId: String): List<String> =
            if (tenantId == "t-1") listOf("org-1", "org-2") else emptyList()

        override fun getOrgAdmins(orgId: String): List<UserAccountCacheEntry> = emptyList()

        override fun getOrgUsers(orgId: String): List<UserAccountCacheEntry> = emptyList()

        override fun isUserInOrg(userId: String, orgId: String): Boolean =
            userId == "u-1" && orgId == "org-1"

        override fun getChildOrgs(orgId: String): List<UserOrgCacheEntry> = emptyList()

        override fun getParentOrg(orgId: String): UserOrgCacheEntry? = null

        private fun org(id: String) = UserOrgCacheEntry(
            id = id,
            name = if (id == "org-1") "Kudos HQ" else "Branch $id",
            shortName = null,
            tenantId = "t-1",
            parentId = null,
            orgTypeDictCode = null,
            sortNum = null,
            remark = null,
            active = true,
            builtIn = false,
            createUserId = null,
            createUserName = null,
            createTime = null,
            updateUserId = null,
            updateUserName = null,
            updateTime = null,
        )
    }

    companion object {
        /** Fixed port: the group's base-url is resolved before a RANDOM_PORT server knows its port. */
        const val PORT: String = "18098"
    }

}
