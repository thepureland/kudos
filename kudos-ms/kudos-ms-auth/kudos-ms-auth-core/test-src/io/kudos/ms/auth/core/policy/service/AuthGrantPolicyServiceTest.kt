package io.kudos.ms.auth.core.policy.service

import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


/**
 * junit test for the shared grant-admission gate and for the five write paths that go through it.
 *
 * Test data source: `AuthGrantPolicyServiceTest.sql` — a maker/checker separation-of-duties pair, a
 * group carrying the checker role, and a second tenant.
 *
 * The point of most of these cases is *equivalence*: the same assignment must be refused whichever
 * door it is attempted through. Before the policy layer existed, only the direct role bind enforced
 * anything, which made group membership the supported way to break a duty-separation rule.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGrantPolicyServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var policyService: IAuthGrantPolicyService

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

    @Resource
    private lateinit var authGroupUserService: IAuthGroupUserService

    @Resource
    private lateinit var authGroupRoleService: IAuthGroupRoleService

    @Resource
    private lateinit var authRoleResourceService: IAuthRoleResourceService

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    private val makerUser = "pol10000-0000-0000-0000-000000000001"
    private val plainUser = "pol10000-0000-0000-0000-000000000002"
    private val otherTenantUser = "pol10000-0000-0000-0000-000000000003"
    private val makerRole = "pol20000-0000-0000-0000-000000000001"
    private val checkerRole = "pol20000-0000-0000-0000-000000000002"
    private val otherTenantRole = "pol20000-0000-0000-0000-000000000003"
    private val checkersGroup = "pol50000-0000-0000-0000-000000000001"
    private val emptyGroup = "pol50000-0000-0000-0000-000000000002"
    private val amsResource = "pol70000-0000-0000-0000-000000000001"
    private val otherSubsysResource = "pol70000-0000-0000-0000-000000000002"

    // -------------------- screenGrants --------------------

    @Test
    fun screenGrants_admissibleCandidatePasses() {
        val rejections = policyService.screenGrants(listOf(GrantCandidate(checkerRole, plainUser)))
        assertTrue(rejections.isEmpty(), "a plain user may receive the checker role; actual: ${rejections}")
    }

    @Test
    fun screenGrants_separationOfDutiesRejects() {
        val rejections = policyService.screenGrants(listOf(GrantCandidate(checkerRole, makerUser)))
        assertEquals(1, rejections.size)
        assertTrue(rejections.first().reason.contains("separation-of-duties"), rejections.first().reason)
    }

    @Test
    fun screenGrants_crossTenantRejects() {
        val rejections = policyService.screenGrants(listOf(GrantCandidate(otherTenantRole, plainUser)))
        assertEquals(1, rejections.size)
        assertTrue(rejections.first().reason.contains("cross-tenant"), rejections.first().reason)
    }

    @Test
    fun screenGrants_unknownRoleOrPrincipalRejects() {
        val rejections = policyService.screenGrants(
            listOf(
                GrantCandidate("no-such-role", plainUser),
                GrantCandidate(checkerRole, "no-such-principal"),
            ),
        )
        assertEquals(2, rejections.size)
        assertTrue(rejections.any { it.reason.contains("role does not exist") })
        assertTrue(rejections.any { it.reason.contains("principal does not exist") })
    }

    @Test
    fun screenGrants_reportsEveryRejectionNotJustTheFirst() {
        // The admin has to see all the blocked rows at once, not fix them one refresh at a time.
        val rejections = policyService.screenGrants(
            listOf(
                GrantCandidate(checkerRole, makerUser),
                GrantCandidate(otherTenantRole, plainUser),
                GrantCandidate(checkerRole, plainUser),
            ),
        )
        assertEquals(2, rejections.size, "two of the three candidates are inadmissible; actual: ${rejections}")
    }

    // -------------------- the five write paths --------------------

    @Test
    fun directRoleBind_isRefusedOnSodBreach() {
        assertFailsWith<IllegalArgumentException> {
            authRoleUserService.batchBind(checkerRole, listOf(makerUser))
        }
    }

    /**
     * The bypass this whole layer exists to close: the assignment refused above must be refused just
     * as firmly when it arrives as group membership instead of as a direct grant.
     */
    @Test
    fun groupMembership_isRefusedOnTheSameSodBreach() {
        val ex = assertFailsWith<IllegalArgumentException> {
            authGroupUserService.batchBind(checkersGroup, listOf(makerUser))
        }
        assertTrue(ex.message!!.contains("separation-of-duties"), ex.message!!)
        assertTrue(ex.message!!.contains("GROUP_MEMBERSHIP"), "the message should say which door was tried")
    }

    @Test
    fun groupMembership_admissibleUserStillGetsIn() {
        val bound = authGroupUserService.batchBind(checkersGroup, listOf(plainUser))
        assertEquals(1, bound, "screening must not block admissible members")
    }

    /**
     * The broadcast write: binding a role to a group hands it to every current member at once, so it
     * is screened against each of them.
     */
    @Test
    fun groupRoleBroadcast_isRefusedWhenAnyMemberWouldBreachSod() {
        // Put the maker into a group that carries nothing yet — admissible on its own.
        authGroupUserService.batchBind(emptyGroup, listOf(makerUser))

        val ex = assertFailsWith<IllegalArgumentException> {
            authGroupRoleService.batchBind(emptyGroup, listOf(checkerRole))
        }
        assertTrue(ex.message!!.contains("separation-of-duties"), ex.message!!)
        assertTrue(ex.message!!.contains(makerUser), "the message must name the member that blocked it")
    }

    @Test
    fun groupRoleBroadcast_passesWhenNoMemberIsAffected() {
        authGroupUserService.batchBind(emptyGroup, listOf(plainUser))
        assertEquals(1, authGroupRoleService.batchBind(emptyGroup, listOf(checkerRole)))
    }

    @Test
    fun permissionBinding_rejectsUnknownAndForeignSubsystemResources() {
        assertFailsWith<IllegalArgumentException> {
            authRoleResourceService.batchBind(makerRole, listOf("no-such-resource"))
        }
        assertFailsWith<IllegalArgumentException> {
            authRoleResourceService.batchBind(makerRole, listOf(otherSubsysResource))
        }
        // The one that is registered in the role's own subsystem goes through.
        assertEquals(1, authRoleResourceService.batchBind(makerRole, listOf(amsResource)))
    }

    @Test
    fun screenPermissionBindings_requiresAnAddressedPermission() {
        val reasons = policyService.screenPermissionBindings(makerRole)
        assertEquals(1, reasons.size)
        assertTrue(reasons.first().contains("resource id or by permission code"), reasons.first())
    }

    /**
     * Re-parenting writes no grant, so no bind-time check would ever look at it — yet it hands every
     * holder of the moved role its new ancestors.
     */
    @Test
    fun reparent_isRefusedWhenItWouldBreachSodForAHolder() {
        val childRole = authRoleDao.insert(AuthRole.Companion().apply {
            this.code = "svc-policy-child-Ph7xR2s"
            this.name = "child"
            this.tenantId = "tenant-policy-1-Ph7xR2s"
            this.subsysCode = "ams"
            this.active = true
            this.builtIn = false
        })
        val grantId = authRoleUserDao.insert(AuthRoleUser.Companion().apply {
            this.roleId = childRole
            this.userId = makerUser
        })
        try {
            // The holder already has maker; putting the child under checker would hand them checker too.
            val rejections = policyService.screenReparent(childRole, checkerRole)
            assertTrue(rejections.isNotEmpty(), "the move would breach the maker/checker rule")
            assertTrue(rejections.first().reason.contains("re-parenting"), rejections.first().reason)
            assertEquals(makerUser, rejections.first().candidate.principalId)

            // Detaching to a root only ever removes inherited permissions.
            assertTrue(policyService.screenReparent(childRole, null).isEmpty())
        } finally {
            authRoleUserDao.deleteById(grantId)
            authRoleDao.deleteById(childRole)
        }
    }
}
