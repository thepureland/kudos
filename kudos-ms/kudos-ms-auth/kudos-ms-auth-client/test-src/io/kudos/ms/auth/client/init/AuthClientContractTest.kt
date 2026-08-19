package io.kudos.ms.auth.client.init

import io.kudos.ms.auth.client.authz.proxy.IAuthzDecisionProxy
import io.kudos.ms.auth.client.role.proxy.IAuthRoleProxy
import io.kudos.ms.auth.common.role.api.IAuthRoleApi
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * Round-trip test for the auth interface clients against a controller implementing the same
 * `IAuthRoleApi` contract from `kudos-ms-auth-common`.
 *
 * Auth is also where the old Feign model was most strained: both proxies declared
 * `name = "auth-role"`, and `IAuthzDecisionProxy` carried `contextId = "authzDecision"` purely to
 * stop Feign rejecting the duplicate. Registering both in one group makes that workaround
 * unnecessary — the first test below pins that they are now distinct beans.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=${AuthClientContractTest.PORT}",
        "spring.http.serviceclient.${AuthClientAutoConfiguration.GROUP}.base-url=http://localhost:${AuthClientContractTest.PORT}",
    ]
)
@Import(AuthClientContractTest.MockRoleController::class)
open class AuthClientContractTest {

    @Autowired
    private lateinit var roleProxy: IAuthRoleProxy

    @Autowired
    private lateinit var authzDecisionProxy: IAuthzDecisionProxy

    @Test
    fun `both proxies resolve independently without a contextId workaround`() {
        // Under Feign these two shared name = "auth-role", so IAuthzDecisionProxy needed
        // contextId = "authzDecision" just to avoid a duplicate-bean clash. Registering both in one
        // group keys them by type instead, so no disambiguator is needed and they stay distinct beans.
        assertNotSame<Any>(roleProxy, authzDecisionProxy)
    }

    @Test
    fun `GET with one unnamed RequestParam round-trips`() {
        val role = roleProxy.getRoleById("r-1")
        assertEquals("r-1", role?.id)
        assertEquals("ADMIN", role?.code)
    }

    @Test
    fun `GET with two unnamed RequestParams round-trips`() {
        assertEquals("r-1", roleProxy.getRoleId("t-1", "ADMIN"))
        assertNull(roleProxy.getRoleId("t-1", "NOPE"))
    }

    @Test
    fun `POST with a RequestBody collection and a Map return round-trips`() {
        val roles = roleProxy.getRolesByIds(listOf("r-1", "r-2"))
        assertEquals(setOf("r-1", "r-2"), roles.keys)
    }

    /**
     * Mock provider; routes come solely from the `@HttpExchange`-family annotations on [IAuthRoleApi].
     *
     * Every contract method is implemented because the interface demands it; only the three the tests
     * assert on return meaningful data.
     */
    @RestController
    open class MockRoleController : IAuthRoleApi {

        override fun getRoleById(id: String): AuthRoleCacheEntry? = role(id)

        override fun getRolesByIds(ids: Collection<String>): Map<String, AuthRoleCacheEntry> =
            ids.associateWith { role(it) }

        override fun getRoleId(tenantId: String, code: String): String? =
            if (tenantId == "t-1" && code == "ADMIN") "r-1" else null

        override fun getRoleUsers(roleId: String): List<UserAccountCacheEntry> = emptyList()

        override fun getUserIdsByRoleCode(tenantId: String, roleCode: String): List<String> = emptyList()

        override fun getRoleResources(roleId: String): List<SysResourceCacheEntry> = emptyList()

        override fun hasResource(roleId: String, resourceId: String): Boolean = false

        override fun getRoleIds(tenantId: String): List<String> = emptyList()

        override fun getResources(userId: String): List<SysResourceCacheEntry> = emptyList()

        override fun getUserRoles(userId: String): List<AuthRoleCacheEntry> = emptyList()

        override fun hasRole(userId: String, roleId: String): Boolean = false

        override fun hasRoleByCode(userId: String, tenantId: String, roleCode: String): Boolean = false

        override fun isUserHasResource(userId: String, resourceId: String): Boolean = false

        private fun role(id: String) = AuthRoleCacheEntry(
            id = id,
            code = if (id == "r-1") "ADMIN" else "USER",
            name = "Role $id",
            tenantId = "t-1",
            subsysCode = "kudos",
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
        const val PORT: String = "18097"
    }

}
