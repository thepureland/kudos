package io.kudos.ms.auth.core.organization

import io.kudos.base.query.PagingSearchResult
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.exclusion.vo.request.AuthRoleExclusionFormUpdate
import io.kudos.ms.auth.common.exclusion.vo.request.AuthRoleExclusionQuery
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionRow
import io.kudos.ms.auth.core.organization.service.OrganizationManagementException
import io.kudos.ms.auth.core.organization.service.OrganizationManagerService
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.exclusion.dao.AuthRoleExclusionDao
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.sys.core.organization.init.OrganizationModeProperties
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Organization-owned SoD rules (auth_role_exclusion.tenant_id = organization id) are managed by their
 * own organization only: an organization administrator can update, list, scan and delete its own rules
 * and is refused on another organization's, and an update cannot move a rule's owner or pair.
 */
@EnabledIfDockerInstalled
class OrganizationSoDRuleIsolationTest : RdbAndRedisCacheTestBase() {

    @Resource private lateinit var modeProperties: OrganizationModeProperties
    @Resource private lateinit var orgDao: UserOrgDao
    @Resource private lateinit var accountDao: UserAccountDao
    @Resource private lateinit var roleDao: AuthRoleDao
    @Resource private lateinit var exclusionDao: AuthRoleExclusionDao
    @Resource private lateinit var exclusions: IAuthRoleExclusionService
    @Resource private lateinit var managers: OrganizationManagerService

    private val s = UUID.randomUUID().toString().take(8)
    private fun id(tag: String) = "$tag-$s".padEnd(36, '0').take(36)

    private val org = id("org1")
    private val otherOrg = id("org2")
    private val admin = id("adm1")
    private val otherAdmin = id("adm2")
    private val ownRule = id("ex1")
    private val foreignRule = id("ex2")

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

    private fun seed() {
        listOf(org to admin, otherOrg to otherAdmin).forEachIndexed { i, (organization, manager) ->
            orgDao.insert(UserOrg {
                this.id = organization; this.organizationId = organization; this.nodeKind = "ORGANIZATION"
                this.tenantId = ""; this.name = "org-$i-$s"; this.orgTypeDictCode = "company"
                this.active = true; this.builtIn = false
            })
            accountDao.insert(UserAccount {
                this.id = manager; this.username = "u-${manager.take(10)}"; this.tenantId = ""
                this.organizationId = organization; this.loginPassword = ""; this.supervisorId = ROOT_SUPERVISOR
                this.active = true; this.builtIn = false
            })
            managers.assignAdmin(organization, manager, "seed")
        }
        rule(ownRule, org, id("rA1"), id("rB1"))
        rule(foreignRule, otherOrg, id("rA2"), id("rB2"))
    }

    private fun rule(ruleId: String, organization: String, roleAId: String, roleBId: String) {
        listOf(roleAId, roleBId).forEach { roleId ->
            roleDao.insert(AuthRole {
                this.id = roleId; this.code = "c-${roleId.take(8)}-$s"; this.name = roleId; this.tenantId = organization
                this.organizationId = organization; this.subsysCode = SYSTEM; this.active = true; this.builtIn = false
            })
        }
        exclusionDao.insert(AuthRoleExclusion {
            this.id = ruleId; this.roleAId = roleAId; this.roleBId = roleBId; this.tenantId = organization
            this.organizationId = organization; this.description = "seed"
        })
    }

    private fun loginAs(userId: String) = KudosContextHolder.set(KudosContext().apply {
        user = SessionUserPrincipal(
            id = userId, tenantId = "", username = userId,
            organizationId = accountDao.get(userId)?.organizationId, subSystemCode = SYSTEM,
        )
    })

    @Suppress("UNCHECKED_CAST")
    private fun listIds(query: AuthRoleExclusionQuery): Set<String> =
        ((exclusions.pagingSearch(query) as PagingSearchResult<Any>).data as List<AuthRoleExclusionRow>)
            .map { it.id }.toSet()

    @Test
    fun foreignOrganizationRuleIsRefused() {
        seed()
        loginAs(admin)
        assertFailsWith<OrganizationManagementException> {
            exclusions.update(AuthRoleExclusionFormUpdate(id = foreignRule, description = "hijack"))
        }
        assertFailsWith<OrganizationManagementException> { exclusions.deleteById(foreignRule) }
        assertFailsWith<OrganizationManagementException> { exclusions.batchDelete(listOf(ownRule, foreignRule)) }
        assertFailsWith<OrganizationManagementException> { exclusions.findViolatingUserIds(foreignRule) }
        assertFailsWith<IllegalArgumentException> { listIds(AuthRoleExclusionQuery(tenantId = otherOrg)) }

        // Nothing of the other organization's rule changed.
        val stored = requireNotNull(exclusionDao.get(foreignRule))
        assertEquals("seed", stored.description)
        assertTrue(exclusionDao.get(ownRule) != null, "the refused batch deleted nothing")
    }

    @Test
    fun ownOrganizationRuleIsManageable() {
        seed()
        loginAs(admin)
        assertEquals(setOf(ownRule), listIds(AuthRoleExclusionQuery()), "only the caller's organization is listed")
        assertEquals(setOf(ownRule), listIds(AuthRoleExclusionQuery(tenantId = org)))
        assertEquals(ownRule, exclusions.findViolatingUserIds(ownRule).exclusionId)

        // Only the description moves: an attempt to re-own or re-pair the rule is pinned to the stored values.
        assertTrue(exclusions.update(AuthRoleExclusion {
            this.id = ownRule; this.description = "renamed"; this.tenantId = otherOrg
            this.organizationId = otherOrg; this.roleAId = id("rA2"); this.roleBId = id("rB2")
        }))
        val updated = requireNotNull(exclusionDao.get(ownRule))
        assertEquals("renamed", updated.description)
        assertEquals(org, updated.tenantId)
        assertEquals(org, updated.organizationId)
        assertEquals(id("rA1"), updated.roleAId)
        assertEquals(id("rB1"), updated.roleBId)

        assertTrue(exclusions.deleteById(ownRule))
        assertEquals(null, exclusionDao.get(ownRule))
    }

    @Test
    fun otherOrganizationAdministratorSeesOnlyItsOwn() {
        seed()
        loginAs(otherAdmin)
        assertEquals(setOf(foreignRule), listIds(AuthRoleExclusionQuery()))
        assertFailsWith<OrganizationManagementException> { exclusions.deleteById(ownRule) }
    }

    private companion object {
        const val SYSTEM = "default-sub-system"
        const val ROOT_SUPERVISOR = "00000000-0000-0000-0000-000000000000"
    }
}
