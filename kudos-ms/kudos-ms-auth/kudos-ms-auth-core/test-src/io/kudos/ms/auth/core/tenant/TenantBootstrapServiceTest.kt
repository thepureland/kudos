package io.kudos.ms.auth.core.tenant

import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.platform.authz.cache.PermissionGrantsByUserIdCache
import io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache
import io.kudos.ms.auth.core.tenant.service.iservice.ITenantBootstrapService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for [io.kudos.ms.auth.core.tenant.service.impl.TenantBootstrapService].
 *
 * Test data source: `TenantBootstrapServiceTest.sql`
 *
 * The bootstrap problem is circular by nature — roles are administered by role-holders — so the
 * properties that matter are about what the framework is allowed to do *on its own*: create roles
 * freely (they grant nobody anything), and confer access only when asked, once.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class TenantBootstrapServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: ITenantBootstrapService

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var decisionApi: IAuthzDecisionApi

    @Resource
    private lateinit var roleIdsCache: RoleIdsByUserIdCache

    @Resource
    private lateinit var grantsCache: PermissionGrantsByUserIdCache

    private val freshTenant = "tb-tenant-fresh-4c7d81ba"
    private val admin = "4c7d81ba-0000-0000-0000-0000000000e1"
    private val second = "4c7d81ba-0000-0000-0000-0000000000e2"

    @Test
    fun seedingCreatesTheBuiltInRoleWithItsBindings() {
        val result = service.seedRoles(freshTenant)

        assertTrue(result.createdRoleCodes.contains("tenant-admin"))
        assertTrue(result.createdBindings > 0)
        val role = assertNotNull(authRoleDao.searchRoleByTenantIdAndRoleCode(freshTenant, "tenant-admin"))
        assertEquals(true, role.builtIn, "the only role that can administer the tenant must not be casually deleted")
    }

    /**
     * Seeding runs on tenant creation and can be re-run by hand after a failure, so a repeat must
     * leave everything alone — including whatever the tenant's administrators have since edited. A
     * redeploy that reset every tenant's customisation would be worse than no bootstrap at all.
     */
    @Test
    fun seedingIsIdempotentAndNeverOverwrites() {
        service.seedRoles(freshTenant)
        val again = service.seedRoles(freshTenant)

        assertTrue(again.createdRoleCodes.isEmpty())
        assertTrue(again.existingRoleCodes.contains("tenant-admin"))
        assertEquals(0, again.createdBindings)
    }

    /**
     * The point of splitting the two steps: a seeded tenant has roles and *nobody holding them*.
     * "A tenant was created" must never silently mean "somebody has administrative access to it".
     */
    @Test
    fun seedingAloneGrantsNobodyAnything() {
        service.seedRoles(freshTenant)
        assertTrue(
            !decisionApi.decide(AuthzRequest(SubjectRef.ofUser(admin), "auth:role:save")).permitted,
        )
    }

    /** …and the explicit second step is what actually confers access. */
    @Test
    fun appointingTheFirstAdministratorConfersAccess() {
        service.seedRoles(freshTenant)
        val result = service.bindFirstAdministrator(freshTenant, admin)
        assertEquals("tenant-admin", result.boundRoleCode)

        val roleId = authRoleDao.searchRoleByTenantIdAndRoleCode(freshTenant, "tenant-admin")!!.id
        roleIdsCache.on(AuthRoleUserRelationsChanged(roleId, listOf(admin)))
        grantsCache.on(AuthRoleUserRelationsChanged(roleId, listOf(admin)))

        assertTrue(
            decisionApi.decide(AuthzRequest(SubjectRef.ofUser(admin), "auth:role:save")).permitted,
            "the wildcard binding auth:role:* covers it",
        )
    }

    /**
     * "First administrator" is a one-time act. An endpoint that quietly appoints a second one is an
     * escalation path — anyone who can reach it could add themselves.
     */
    @Test
    fun appointingASecondAdministratorRequiresSayingSo() {
        service.seedRoles(freshTenant)
        service.bindFirstAdministrator(freshTenant, admin)

        val failure = assertFailsWith<IllegalArgumentException> {
            service.bindFirstAdministrator(freshTenant, second)
        }
        assertTrue(failure.message!!.contains("force"))

        // With the intent stated, it goes through.
        val forced = service.bindFirstAdministrator(freshTenant, second, force = true)
        assertEquals(second, forced.boundUserId)
    }

    /** Re-appointing the same person is a no-op rather than an error; retries must be safe. */
    @Test
    fun reAppointingTheSamePersonIsANoOp() {
        service.seedRoles(freshTenant)
        service.bindFirstAdministrator(freshTenant, admin)
        val again = service.bindFirstAdministrator(freshTenant, admin, force = true)
        assertEquals(admin, again.boundUserId)
    }

    /** Appointing into a tenant that was never seeded says so, rather than failing obscurely. */
    @Test
    fun appointingIntoAnUnseededTenantIsRefusedClearly() {
        val failure = assertFailsWith<IllegalArgumentException> {
            service.bindFirstAdministrator("tb-tenant-never-seeded", admin)
        }
        assertTrue(failure.message!!.contains("seed the tenant first"))
    }

    /**
     * The bootstrap call skips *delegation* screening — there is nobody inside a new tenant with
     * authority to delegate — but not the tenant boundary. Appointing somebody from another tenant
     * would make this endpoint a cross-tenant escalation.
     */
    @Test
    fun theTenantBoundaryStillHolds() {
        service.seedRoles(freshTenant)
        val outsider = "4c7d81ba-0000-0000-0000-0000000000e9"
        val failure = assertFailsWith<IllegalArgumentException> {
            service.bindFirstAdministrator(freshTenant, outsider)
        }
        assertTrue(failure.message!!.contains("cross-tenant"))
    }
}
