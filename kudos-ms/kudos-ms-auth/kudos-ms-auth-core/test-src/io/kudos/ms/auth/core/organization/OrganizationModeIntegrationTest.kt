package io.kudos.ms.auth.core.organization

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.organization.service.EffectiveRoleResolver
import io.kudos.ms.auth.core.organization.service.OrganizationAdministrationPolicy
import io.kudos.ms.auth.core.organization.service.OrganizationManagementException
import io.kudos.ms.auth.core.organization.service.OrganizationManagerService
import io.kudos.ms.auth.core.organization.service.OrganizationMemberRoleService
import io.kudos.ms.auth.core.organization.service.OrganizationRevisionConflictException
import io.kudos.ms.auth.core.organization.service.OrganizationSessionDeniedException
import io.kudos.ms.auth.core.organization.service.OrganizationSessionTargeting
import io.kudos.ms.auth.core.organization.service.OrganizationTenantService
import io.kudos.ms.auth.core.organization.service.TenantEntryDenial
import io.kudos.ms.auth.core.organization.service.TenantEntryService
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.exclusion.dao.AuthRoleExclusionDao
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.sys.core.organization.init.OrganizationModeProperties
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.dao.SysTenantSystemDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.Instant
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Organization mode end to end on the real auth/user/sys beans and H2 (acceptance K-1 … K-8 of the
 * design), with the synthetic organization of the soul examples: 甲公司 owns tenants A, B, C (open) and
 * D (closed); 小王 holds 使用者管理员 and 报表查看 by default and has 使用者管理员 removed in B.
 *
 * Organization mode is switched on for each test and off afterwards (the startup guard only runs at
 * boot); every test's writes roll back with its transaction.
 */
@EnabledIfDockerInstalled
class OrganizationModeIntegrationTest : RdbAndRedisCacheTestBase() {

    @Resource private lateinit var modeProperties: OrganizationModeProperties
    @Resource private lateinit var tenantDao: SysTenantDao
    @Resource private lateinit var tenantSystemDao: SysTenantSystemDao
    @Resource private lateinit var orgDao: UserOrgDao
    @Resource private lateinit var accountDao: UserAccountDao
    @Resource private lateinit var roleDao: AuthRoleDao
    @Resource private lateinit var bindingDao: AuthRoleResourceDao
    @Resource private lateinit var tenants: OrganizationTenantService
    @Resource private lateinit var members: OrganizationMemberRoleService
    @Resource private lateinit var managers: OrganizationManagerService
    @Resource private lateinit var entries: TenantEntryService
    @Resource private lateinit var effective: EffectiveRoleResolver
    @Resource private lateinit var policy: OrganizationAdministrationPolicy
    @Resource private lateinit var targeting: OrganizationSessionTargeting
    @Resource private lateinit var decisions: IAuthzDecisionApi
    @Resource private lateinit var exclusionDao: AuthRoleExclusionDao

    private val s = UUID.randomUUID().toString().take(8)
    private fun id(tag: String) = "$tag-$s".padEnd(36, '0').take(36)

    private val org = id("org1")
    private val otherOrg = id("org2")
    private val tenantA = id("tA"); private val tenantB = id("tB"); private val tenantC = id("tC"); private val tenantD = id("tD")
    private val wang = id("wang"); private val lin = id("lin"); private val zhang = id("zhang"); private val zhou = id("zhou")
    private val li = id("li"); private val wu = id("wu"); private val chen = id("chen"); private val outsider = id("outs")
    private val userAdmin = id("rUA"); private val report = id("rRP"); private val notify = id("rNT")

    @BeforeTest
    fun enableOrganizationMode() {
        modeProperties.enabled = true
        KudosContextHolder.clear()
    }

    @AfterTest
    fun disableOrganizationMode() {
        modeProperties.enabled = false
        KudosContextHolder.clear()
    }

    // region fixtures

    private fun seed() {
        root(org, "甲公司-$s")
        root(otherOrg, "乙公司-$s")
        listOf(tenantA, tenantB, tenantC, tenantD).forEach(::tenant)
        listOf(tenantA, tenantB, tenantC, tenantD).forEach { tenants.associate(it, org, "seed") }
        listOf(tenantA, tenantB, tenantC).forEach { tenants.setOpen(it, true, null, "seed") }
        listOf(wang, lin, zhang, zhou, li, wu, chen).forEach { account(it, org) }
        account(outsider, otherOrg)
        role(userAdmin, "user-admin", "user:admin:*")
        role(report, "report-view", "report:view")
        role(notify, "notify-send", "notice:send")
        managers.assignAdmin(org, zhang, "seed")
        managers.grantManagementRole(org, li, ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN, emptyList(), "seed")
        managers.grantManagementRole(org, wu, ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN, emptyList(), "seed")
        managers.grantManagementRole(org, chen, ManagementRoleKind.TENANT_PERMISSION_ADMIN, listOf(tenantB), "seed")
        members.saveDefaults(org, wang, listOf(userAdmin, report), null, "seed")
        members.saveDefaults(org, lin, listOf(notify), null, "seed")
        members.saveOverride(org, tenantB, wang, userAdmin, RoleOverrideAction.REMOVE, null, "seed")
    }

    private fun root(id: String, name: String) {
        orgDao.insert(UserOrg {
            this.id = id; this.organizationId = id; this.nodeKind = "ORGANIZATION"; this.tenantId = ""
            this.name = name; this.orgTypeDictCode = "company"; this.active = true; this.builtIn = false
        })
    }

    private fun tenant(id: String) {
        tenantDao.insert(SysTenant { this.id = id; this.name = "tenant-$id"; this.active = true; this.builtIn = false })
        tenantSystemDao.insert(SysTenantSystem { this.tenantId = id; this.systemCode = SYSTEM })
    }

    private fun account(id: String, organizationId: String) {
        accountDao.insert(UserAccount {
            this.id = id; this.username = "u-${id.take(10)}"; this.tenantId = ""; this.organizationId = organizationId
            this.loginPassword = ""; this.supervisorId = ROOT_SUPERVISOR; this.active = true; this.builtIn = false
        })
    }

    private fun role(id: String, code: String, permission: String) {
        roleDao.insert(AuthRole {
            this.id = id; this.code = "$code-$s"; this.name = code; this.tenantId = org; this.organizationId = org
            this.subsysCode = SYSTEM; this.active = true; this.builtIn = false
        })
        bindingDao.insert(AuthRoleResource { this.roleId = id; this.permissionCode = permission; this.effect = "ALLOW" })
    }

    private fun loginAs(userId: String, tenantId: String?) = KudosContextHolder.set(KudosContext().apply {
        user = SessionUserPrincipal(
            id = userId, tenantId = tenantId.orEmpty(), username = userId,
            organizationId = accountDao.get(userId)?.organizationId, subSystemCode = SYSTEM,
        )
    })

    private fun decide(userId: String, tenantId: String?, code: String): AuthzDecision {
        loginAs(userId, tenantId)
        return decisions.decide(AuthzRequest(subject = SubjectRef.ofUser(userId), permissionCode = code))
    }

    // endregion

    /** K-1: defaults apply everywhere, the override only in B, and each request is judged in its tenant. */
    @Test
    fun k1_defaultsAndOverrideArePerTenant() {
        seed()
        assertEquals(setOf(userAdmin, report), effective.effectiveRoleIds(org, tenantA, wang))
        assertEquals(setOf(report), effective.effectiveRoleIds(org, tenantB, wang))
        assertEquals(setOf(userAdmin, report), effective.effectiveRoleIds(org, tenantC, wang))
        assertTrue(decide(wang, tenantA, "user:admin:delete").permitted)
        assertFalse(decide(wang, tenantB, "user:admin:delete").permitted)
        assertTrue(decide(wang, tenantB, "report:view").permitted)
    }

    /** K-3: a new default follows into B; a removed-then-restored default keeps B's REMOVE; idle overrides are flagged. */
    @Test
    fun k3_overridesOnlyRecordDifferences() {
        seed()
        members.saveDefaults(org, wang, listOf(userAdmin, report, notify), null, "add notify")
        assertEquals(setOf(report, notify), effective.effectiveRoleIds(org, tenantB, wang))

        members.saveDefaults(org, wang, listOf(report, notify), null, "drop user admin")
        val idle = effective.memberRoleViews(org, tenantB, wang).single { it.roleId == userAdmin }
        assertTrue(idle.ineffective, "a REMOVE of a non-default role does nothing today and is shown as such")

        members.saveDefaults(org, wang, listOf(userAdmin, report, notify), null, "restore")
        assertFalse(userAdmin in effective.effectiveRoleIds(org, tenantB, wang), "B's REMOVE survives the default coming back")
        assertTrue(userAdmin in effective.effectiveRoleIds(org, tenantA, wang))

        members.removeOverride(org, tenantB, wang, userAdmin, null, "follow defaults again")
        assertTrue(userAdmin in effective.effectiveRoleIds(org, tenantB, wang))
    }

    /** K-4: closed tenants admit nobody, administrators included; dissociation drops overrides and jurisdictions. */
    @Test
    fun k4_tenantOpeningAndDissociation() {
        seed()
        members.saveOverride(org, tenantD, wang, userAdmin, RoleOverrideAction.REMOVE, null, "prepare D")
        assertEquals(TenantEntryDenial.TENANT_NOT_OPEN, entries.check(tenantD, wang).denial)
        assertEquals(TenantEntryDenial.TENANT_NOT_OPEN, entries.check(tenantD, zhang).denial)
        assertFalse(decide(zhang, tenantD, "report:view").permitted)

        tenants.setOpen(tenantD, true, null, "go live")
        assertTrue(entries.check(tenantD, wang).allowed)
        assertEquals(setOf(report), effective.effectiveRoleIds(org, tenantD, wang), "the prepared override applies once open")
        assertTrue(decide(zhang, tenantD, "user:account:pagingSearch").permitted, "the organization administrator holds everything entitled")

        tenants.setOpen(tenantD, false, null, "pause")
        assertFalse(entries.check(tenantD, wang).allowed)

        KudosContextHolder.clear()  // dissociation is a platform act
        tenants.dissociate(tenantB, "leave")
        assertTrue(effective.memberRoleViews(org, tenantB, wang).none { it.override != null })
        assertTrue(managers.listManagementRoles(org).none { it.tenantId == tenantB }, "B's tenant permission administrator lost B")
        assertEquals(TenantEntryDenial.ORGANIZATION_MISMATCH, entries.check(tenantB, wang).denial)
    }

    /** K-5: removing every role keeps a member out; a later default brings them back, and the preview says so. */
    @Test
    fun k5_previewWarnsAboutRegainedEntry() {
        seed()
        members.saveOverride(org, tenantC, wang, report, RoleOverrideAction.REMOVE, null, "keep out")
        members.saveOverride(org, tenantC, wang, userAdmin, RoleOverrideAction.REMOVE, null, "keep out")
        assertEquals(TenantEntryDenial.NO_EFFECTIVE_ROLE, entries.check(tenantC, wang).denial)
        assertTrue(entries.accessibleTenants(wang).none { it.tenantId == tenantC })

        val preview = members.previewDefaults(org, wang, listOf(userAdmin, report, notify))
        assertTrue(preview.entryChanges.any { it.tenantId == tenantC && it.regains })
    }

    /** K-6: the three management identities can do exactly what they are given. */
    @Test
    fun k6_managementIdentities() {
        seed()
        loginAs(li, null)  // organization permission administrator
        members.saveOverride(org, tenantA, wang, notify, RoleOverrideAction.ADD, null, "li adds")
        assertFailsWith<OrganizationManagementException> {
            managers.grantManagementRole(org, lin, ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN, emptyList(), "peer")
        }
        assertFailsWith<OrganizationManagementException> { managers.assignAdmin(org, lin, "not allowed") }
        assertFailsWith<OrganizationManagementException> { policy.assertCanEndMembership(org, zhang) }
        assertFailsWith<OrganizationManagementException> { policy.assertCanEndMembership(org, wu) }
        assertFailsWith<OrganizationManagementException> {
            managers.revokeManagementRole(org, wu, ManagementRoleKind.ORGANIZATION_PERMISSION_ADMIN, emptyList(), "peer")
        }
        assertFailsWith<OrganizationManagementException> { members.saveDefaults(org, li, listOf(report), null, "self") }
        policy.assertCanEndMembership(org, wang)

        loginAs(chen, null)  // tenant permission administrator of B
        members.saveOverride(org, tenantB, wang, notify, RoleOverrideAction.ADD, null, "chen in B")
        assertFailsWith<OrganizationManagementException> {
            members.saveOverride(org, tenantA, wang, notify, RoleOverrideAction.REMOVE, null, "chen in A")
        }
        assertFailsWith<OrganizationManagementException> { members.saveDefaults(org, wang, listOf(report), null, "chen defaults") }
        assertFailsWith<OrganizationManagementException> {
            managers.grantManagementRole(org, lin, ManagementRoleKind.TENANT_PERMISSION_ADMIN, listOf(tenantB), "chen grants")
        }

        loginAs(zhang, null)  // organization administrator
        managers.assignAdmin(org, zhou, "second admin")
        assertFailsWith<OrganizationManagementException> { managers.revokeAdmin(org, zhang, "self") }
        managers.revokeAdmin(org, zhou, "back to one")
        loginAs(zhou, null)
        assertFailsWith<OrganizationManagementException> { managers.revokeAdmin(org, zhang, "not an admin any more") }
        KudosContextHolder.clear()
        assertFailsWith<IllegalArgumentException> { managers.revokeAdmin(org, zhang, "platform cannot leave none") }
    }

    /** G-12: an organization permission administrator does not edit a role it holds. */
    @Test
    fun k6_cannotEditHeldRole() {
        seed()
        members.saveDefaults(org, li, listOf(report), null, "li reads reports")
        loginAs(li, null)
        assertFailsWith<OrganizationManagementException> { policy.assertCanEditRoleDefinition(report) }
        policy.assertCanEditRoleDefinition(notify)
    }

    /** K-7: management-only members enter no tenant and work in the organization scope, management permissions only. */
    @Test
    fun k7_managementOnlyMemberWorksInOrganizationScope() {
        seed()
        assertTrue(entries.accessibleTenants(li).isEmpty())
        val context = AuthenticationContext(userId = li, tenantId = tenantA, authTime = Instant.now(), amr = setOf("pwd"), acr = "1")
        val target = targeting.resolve(context)
        assertEquals("", target.tenantId)
        assertEquals(org, target.organizationId)
        assertTrue(decide(li, null, "auth:role:pagingSearch").permitted)
        assertFalse(decide(li, null, "report:view").permitted)

        val outsiderContext = AuthenticationContext(userId = lin, tenantId = tenantD, authTime = Instant.now(), amr = setOf("pwd"), acr = "1")
        assertFailsWith<OrganizationSessionDeniedException> { targeting.resolve(outsiderContext) }
    }

    /** K-2: another organization's people reach nothing here. */
    @Test
    fun k2_crossOrganizationIsRefused() {
        seed()
        loginAs(outsider, null)
        assertFailsWith<OrganizationManagementException> { members.read(org, wang) }
        assertFalse(entries.check(tenantA, outsider).allowed)
        assertEquals(TenantEntryDenial.ORGANIZATION_MISMATCH, entries.check(tenantA, outsider).denial)
    }

    /** Duty separation holds per tenant: an override that completes an exclusive pair in one tenant is refused. */
    @Test
    fun sodHoldsPerTenantForOverridesAndDefaults() {
        seed()
        exclusionDao.insert(AuthRoleExclusion {
            this.roleAId = minOf(report, notify); this.roleBId = maxOf(report, notify)
            this.tenantId = org; this.organizationId = org
        })
        assertFailsWith<IllegalArgumentException> {
            members.saveOverride(org, tenantA, wang, notify, RoleOverrideAction.ADD, null, "would conflict in A")
        }
        assertFailsWith<IllegalArgumentException> {
            members.saveDefaults(org, wang, listOf(userAdmin, report, notify), null, "would conflict everywhere")
        }
    }

    /** G-12 covers inherited roles: holding a child means holding its ancestor's permissions. */
    @Test
    fun heldRoleIncludesAncestors() {
        seed()
        roleDao.updateProperties(report, mapOf(AuthRole::parentId.name to userAdmin))
        members.saveDefaults(org, li, listOf(report), null, "li reads reports")
        loginAs(li, null)
        assertFailsWith<OrganizationManagementException> { policy.assertCanEditRoleDefinition(userAdmin) }
    }

    /** A tenant permission administrator handles ordinary members, not other managers. */
    @Test
    fun tenantPermissionAdminCannotOverrideManagers() {
        seed()
        loginAs(chen, null)
        assertFailsWith<OrganizationManagementException> {
            members.saveOverride(org, tenantB, zhang, notify, RoleOverrideAction.ADD, null, "chen on the admin")
        }
    }

    /** A writer that has not seen the latest change is refused instead of overwriting it. */
    @Test
    fun staleRevisionIsRejected() {
        seed()
        val stale = members.read(org, wang).revision
        members.saveOverride(org, tenantA, wang, notify, RoleOverrideAction.ADD, stale, "first")
        assertFailsWith<OrganizationRevisionConflictException> {
            members.saveOverride(org, tenantC, wang, notify, RoleOverrideAction.ADD, stale, "second, stale")
        }
    }

    private companion object {
        const val SYSTEM = "default-sub-system"
        const val ROOT_SUPERVISOR = "00000000-0000-0000-0000-000000000000"
    }
}
