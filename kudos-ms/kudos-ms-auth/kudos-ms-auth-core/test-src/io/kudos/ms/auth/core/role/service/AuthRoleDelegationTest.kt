package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.common.delegation.vo.RoleGrantRequest
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


/**
 * junit test for controlled delegation.
 *
 * Test data source: `AuthRoleDelegationTest.sql`.
 *
 * The rule being tested is one sentence — *what is handed on can only ever be narrower than what the
 * grantor holds* — across the four dimensions a grant has (reach, power, population, lifetime), plus
 * what happens when the authority underneath a chain disappears.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleDelegationTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    private val boss = "dlg10000-0000-0000-0000-000000000001"
    private val lead = "dlg10000-0000-0000-0000-000000000002"
    private val staff = "dlg10000-0000-0000-0000-000000000003"
    private val weak = "dlg10000-0000-0000-0000-000000000004"
    private val viaGroup = "dlg10000-0000-0000-0000-000000000005"
    private val outsider = "dlg10000-0000-0000-0000-000000000006"
    private val managerRole = "dlg20000-0000-0000-0000-000000000001"
    private val directorRole = "dlg20000-0000-0000-0000-000000000002"
    private val sealedRole = "dlg20000-0000-0000-0000-000000000003"
    private val bossManagerGrant = "dlg50000-0000-0000-0000-000000000001"

    // -------------------- the happy path and the chain it records --------------------

    @Test
    fun delegating_recordsTheChainAndNarrowsTheDepth() {
        val ids = authRoleUserService.grant(
            RoleGrantRequest(roleId = managerRole, userIds = listOf(lead), delegableDepth = 1),
            operatorId = boss,
        )
        assertEquals(1, ids.size)
        val grant = authRoleUserDao.get(ids.first())!!
        assertEquals(boss, grant.grantedBy, "the grant must name who exercised the authority")
        assertEquals(bossManagerGrant, grant.parentGrantId, "and which grant it came from")
        assertEquals(1, grant.delegableDepth)
        assertTrue(authRoleUserService.exists(managerRole, lead))
    }

    @Test
    fun delegatedAuthorityRunsOut() {
        // boss(2) -> lead(1) -> staff(0); staff may not pass it on again.
        authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(lead), delegableDepth = 1), operatorId = boss,
        )
        authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(staff), delegableDepth = 0), operatorId = lead,
        )
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(
                RoleGrantRequest(managerRole, listOf(outsider), delegableDepth = 0), operatorId = staff,
            )
        }
        assertTrue(ex.message!!.contains("terminal"), ex.message!!)
    }

    // -------------------- 1. reach --------------------

    @Test
    fun aTerminalGrantCannotBePassedOn() {
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(RoleGrantRequest(managerRole, listOf(lead)), operatorId = weak)
        }
        assertTrue(ex.message!!.contains("terminal"), ex.message!!)
    }

    @Test
    fun depthMustStrictlyNarrow() {
        val ex = assertFailsWith<IllegalArgumentException> {
            // boss holds depth 2, so the most they may hand on is 1.
            authRoleUserService.grant(
                RoleGrantRequest(managerRole, listOf(lead), delegableDepth = 2), operatorId = boss,
            )
        }
        assertTrue(ex.message!!.contains("does not narrow"), ex.message!!)
    }

    @Test
    fun theRolesOwnCeilingStillApplies() {
        // boss holds "sealed" with depth 2, but the role forbids delegation outright.
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(
                RoleGrantRequest(sealedRole, listOf(lead), delegableDepth = 1), operatorId = boss,
            )
        }
        assertTrue(ex.message!!.contains("ceiling"), ex.message!!)
    }

    @Test
    fun aRoleReachedThroughAGroupCarriesNoDelegationAuthority() {
        // Membership is batch and fluid; a chain running through it could not answer "who gave you
        // this" with a person's name, so the answer is simply no.
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(RoleGrantRequest(managerRole, listOf(lead)), operatorId = viaGroup)
        }
        assertTrue(ex.message!!.contains("direct grant"), ex.message!!)
    }

    // -------------------- 2. power --------------------

    @Test
    fun cannotHandOnMorePowerThanOneCanExercise() {
        // boss *holds* "director" directly and delegably, so the reach check passes. But boss also
        // holds a role carrying DENY ams:order:close, so boss cannot actually exercise everything
        // "director" grants — and handing it on would give the recipient strictly more than the
        // grantor has. Containment is judged on the resolved permission set for exactly this reason;
        // "do they hold the role" would have said yes.
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(
                RoleGrantRequest(directorRole, listOf(lead), delegableDepth = 0), operatorId = boss,
            )
        }
        assertTrue(ex.message!!.contains("does not hold every permission"), ex.message!!)
        assertTrue(ex.message!!.contains("ams:order:close"), ex.message!!)
    }

    @Test
    fun cannotHandOnARoleOneDoesNotHoldAtAll() {
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(RoleGrantRequest(managerRole, listOf(staff)), operatorId = outsider)
        }
        assertTrue(ex.message!!.contains("direct grant"), ex.message!!)
    }

    // -------------------- 3. population --------------------

    @Test
    fun cannotReachOutsideTheScopeFrozenAtGrantTime() {
        val ownGrant = authRoleUserDao.get(bossManagerGrant)!!
        ownGrant.scopeSnapshot = """{"org":["dlg00000-0000-0000-0000-00000000000a"]}"""
        authRoleUserDao.update(ownGrant)

        // Inside the frozen scope: fine.
        assertEquals(
            1,
            authRoleUserService.grant(RoleGrantRequest(managerRole, listOf(lead)), operatorId = boss).size,
        )
        // Outside it: refused, even though both are in the same tenant.
        val ex = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(RoleGrantRequest(managerRole, listOf(outsider)), operatorId = boss)
        }
        assertTrue(ex.message!!.contains("delegation scope"), ex.message!!)
    }

    // -------------------- 4. lifetime --------------------

    @Test
    fun aDelegatedGrantCannotOutliveItsSource() {
        val ownGrant = authRoleUserDao.get(bossManagerGrant)!!
        ownGrant.endTime = LocalDateTime.now().plusDays(7)
        authRoleUserDao.update(ownGrant)

        assertEquals(
            1,
            authRoleUserService.grant(
                RoleGrantRequest(managerRole, listOf(lead), endTime = LocalDateTime.now().plusDays(3)),
                operatorId = boss,
            ).size,
        )
        assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(
                RoleGrantRequest(managerRole, listOf(staff), endTime = LocalDateTime.now().plusDays(30)),
                operatorId = boss,
            )
        }
        // An open-ended delegation from a time-limited authority is the same mistake in disguise.
        assertFailsWith<IllegalArgumentException> {
            authRoleUserService.grant(RoleGrantRequest(managerRole, listOf(staff)), operatorId = boss)
        }
    }

    // -------------------- revocation --------------------

    @Test
    fun revokingCascadesDownTheChainAndIsSoft() {
        val leadGrant = authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(lead), delegableDepth = 1), operatorId = boss,
        ).first()
        val staffGrant = authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(staff), delegableDepth = 0), operatorId = lead,
        ).first()

        val revoked = authRoleUserService.revoke(leadGrant, "left the team")
        assertEquals(2, revoked, "the grant and everything delegated from it")

        assertFalse(authRoleUserService.exists(managerRole, lead))
        assertFalse(authRoleUserService.exists(managerRole, staff), "the downstream grant goes too")
        // Soft: the rows survive so the chain and the audit trail remain readable.
        assertNotNull(authRoleUserDao.get(staffGrant))
        assertEquals(true, authRoleUserDao.get(staffGrant)!!.revoked)
        assertEquals("left the team", authRoleUserDao.get(leadGrant)!!.revokeReason)
        // And a revoked grant no longer contributes to permission resolution.
        assertFalse(authRoleUserDao.searchRoleIdsByUserId(staff).contains(managerRole))
    }

    @Test
    fun revokeImpactIsAvailableBeforeRevoking() {
        val leadGrant = authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(lead), delegableDepth = 1), operatorId = boss,
        ).first()
        authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(staff), delegableDepth = 0), operatorId = lead,
        )

        val impact = authRoleUserService.getRevokeImpact(leadGrant)
        assertEquals(lead, impact.userId)
        assertEquals(1, impact.cascadedGrants.size, "one grant sits below this one")
        assertEquals(staff, impact.cascadedGrants.first().userId)
        assertEquals(1, impact.cascadedGrants.first().depth)
        assertEquals(2, impact.affectedUserCount)

        // Asking must not change anything.
        assertTrue(authRoleUserService.exists(managerRole, staff))
    }

    @Test
    fun removingTheUpstreamGrantTakesTheDownstreamWithIt() {
        // The zombie-permission case: if the source of authority is deleted and its descendants are
        // left alone, they keep working with nothing to trace them back to.
        authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(lead), delegableDepth = 1), operatorId = boss,
        )
        authRoleUserService.grant(
            RoleGrantRequest(managerRole, listOf(staff), delegableDepth = 0), operatorId = lead,
        )

        assertTrue(authRoleUserService.unbind(managerRole, lead))
        assertFalse(authRoleUserService.exists(managerRole, staff), "the delegated grant must not survive its source")
    }

    @Test
    fun revokingAnUnknownGrantIsANoOp() {
        assertEquals(0, authRoleUserService.revoke("no-such-grant"))
        assertEquals(0, authRoleUserService.getRevokeImpact("no-such-grant").affectedUserCount)
    }

    @Test
    fun aPlainBindCarriesNoDelegationAuthority() {
        // batchBind has no operator, so it writes no chain — and what it writes is terminal, which is
        // the safe default for an administrative act with no upstream.
        authRoleUserService.batchBind(managerRole, listOf(lead))
        val grant = authRoleUserDao.searchGrantsByRoleId(managerRole).first { it.userId == lead }
        assertEquals(0, grant.delegableDepth)
        assertEquals(null, grant.parentGrantId)
    }
}
