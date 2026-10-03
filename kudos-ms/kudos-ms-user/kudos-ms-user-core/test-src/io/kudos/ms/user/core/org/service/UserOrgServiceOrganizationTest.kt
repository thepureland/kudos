package io.kudos.ms.user.core.org.service

import io.kudos.base.query.Criteria
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.user.common.org.vo.request.UserOrgFormCreate
import io.kudos.ms.user.common.org.vo.request.UserOrgFormUpdate
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgInserted
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.impl.UserOrgNodeKind
import io.kudos.ms.user.core.org.service.impl.UserOrgService
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import io.kudos.ms.user.core.security.PLATFORM_ADMIN
import io.kudos.ms.user.core.security.PLATFORM_TENANT
import io.kudos.ms.user.core.security.accessGuard
import io.kudos.ms.user.core.security.department
import io.kudos.ms.user.core.security.directoryGate
import io.kudos.ms.user.core.security.legacyOrg
import io.kudos.ms.user.core.security.organizationMode
import io.kudos.ms.user.core.security.organizationRoot
import io.kudos.ms.user.core.security.setField
import io.kudos.ms.user.core.security.signIn
import io.kudos.ms.user.core.security.signOut
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure tests of the organization-mode paths of [UserOrgService]: organization roots, departments,
 * pinned ownership columns on update, root immobility, delete guards, and the unchanged legacy path.
 */
internal class UserOrgServiceOrganizationTest {

    private val dao = mock(UserOrgDao::class.java)
    private val userOrgUserDao = mock(UserOrgUserDao::class.java)
    private val accountDao = mock(UserAccountDao::class.java)
    private val tenantDao = mock(SysTenantDao::class.java)
    private val policy = mock(IOrganizationMemberPolicy::class.java)
    private val events = mutableListOf<Any>()

    private fun build(modeEnabled: Boolean = true, withPolicy: Boolean = true): UserOrgService {
        val guard = accessGuard()
        return UserOrgService(dao).also { svc ->
            setField(svc, "userOrgUserDao", userOrgUserDao)
            setField(svc, "userOrgHashCache", mock(UserOrgHashCache::class.java))
            setField(svc, "userAccountHashCache", mock(UserAccountHashCache::class.java))
            setField(svc, "userIdsByOrgIdCache", mock(UserIdsByOrgIdCache::class.java))
            setField(svc, "eventPublisher", ApplicationEventPublisher { events.add(it) })
            setField(svc, "tenantAccess", guard)
            setField(svc, "directoryGate", directoryGate(organizationMode(modeEnabled), guard, policy.takeIf { withPolicy }))
            setField(svc, "userAccountDao", accountDao)
            setField(svc, "sysTenantDao", tenantDao)
        }
    }

    private fun anyArg(): Any = ArgumentMatchers.any() ?: Any()
    private fun anyMapArg(): Map<String, *> = ArgumentMatchers.anyMap<String, Any?>() ?: emptyMap<String, Any?>()
    private fun anyCriteria(): Criteria = ArgumentMatchers.any(Criteria::class.java) ?: Criteria()
    private fun captured(captor: ArgumentCaptor<Any>): Any = captor.capture() ?: Any()

    private fun stub(vararg orgs: UserOrg) = orgs.forEach { whenCalled(dao.get(it.id)).thenReturn(it) }

    private fun insertedEntity(): UserOrg {
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(dao).insert(captured(captor))
        return captor.value as UserOrg
    }

    private fun updatedEntity(): UserOrg {
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(dao).update(captured(captor))
        return captor.value as UserOrg
    }

    private fun create(
        name: String = "n",
        tenantId: String? = null,
        parentId: String? = null,
        organizationId: String? = null,
        nodeKind: String? = null,
    ) = UserOrgFormCreate(name, null, tenantId, parentId, "1", null, null, organizationId, nodeKind)

    private fun update(
        id: String,
        name: String? = "renamed",
        tenantId: String? = null,
        parentId: String? = null,
        organizationId: String? = null,
        nodeKind: String? = null,
    ) = UserOrgFormUpdate(id, name, null, tenantId, parentId, "1", 7, null, organizationId, nodeKind)

    @AfterTest
    fun clear() {
        signOut()
        events.clear()
    }

    // ---- organization roots ----

    @Test
    fun trustedWorkCreatesASelfOwnedRootWithoutTenantOrParent() {
        whenCalled(dao.insert(anyArg())).thenReturn("generated")
        val id = build().insert(create(name = "Acme", tenantId = "tenant-a", organizationId = "spoofed", nodeKind = UserOrgNodeKind.ORGANIZATION))
        assertEquals("generated", id)
        val root = insertedEntity()
        assertEquals("Acme", root.name)
        assertEquals(root.id, root.organizationId)
        assertTrue(root.organizationId != "spoofed")
        assertEquals(UserOrgNodeKind.ORGANIZATION, root.nodeKind)
        assertEquals("", root.tenantId)
        assertNull(root.parentId)
        assertTrue(events.any { it is UserOrgInserted && it.id == "generated" })
        verifyNoInteractions(policy)
    }

    @Test
    fun platformAdministratorCreatesRoots() {
        whenCalled(dao.insert(anyArg())).thenReturn("generated")
        signIn(PLATFORM_ADMIN, PLATFORM_TENANT)
        build().insert(create(nodeKind = UserOrgNodeKind.ORGANIZATION))
        assertEquals(UserOrgNodeKind.ORGANIZATION, insertedEntity().nodeKind)
    }

    @Test
    fun organizationMembersAndLegacyPrincipalsCannotCreateRoots() {
        val service = build()
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { service.insert(create(nodeKind = UserOrgNodeKind.ORGANIZATION)) }
        signIn("u1", "tenant-a")
        assertFailsWith<IllegalArgumentException> { service.insert(create(nodeKind = UserOrgNodeKind.ORGANIZATION)) }
        verify(dao, never()).insert(anyArg())
        verifyNoInteractions(policy)
    }

    @Test
    fun rootsRequireOrganizationModeAndNoParent() {
        assertFailsWith<IllegalStateException> { build(modeEnabled = false).insert(create(nodeKind = UserOrgNodeKind.ORGANIZATION)) }
        assertFailsWith<IllegalArgumentException> {
            build().insert(create(nodeKind = UserOrgNodeKind.ORGANIZATION, parentId = "org-1"))
        }
        verify(dao, never()).insert(anyArg())
    }

    // ---- departments ----

    @Test
    fun departmentWithoutParentGoesUnderTheRootOfThePrincipalsOrganization() {
        stub(organizationRoot("org-1"))
        whenCalled(dao.insert(anyArg())).thenReturn("dept-new")
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertEquals("dept-new", build().insert(create(name = "Sales", tenantId = "tenant-a")))
        val dept = insertedEntity()
        assertEquals("org-1", dept.organizationId)
        assertEquals(UserOrgNodeKind.DEPARTMENT, dept.nodeKind)
        assertEquals("", dept.tenantId)
        assertEquals("org-1", dept.parentId)
        assertEquals("Sales", dept.name)
        verify(policy).assertCanManageDirectory("org-1")
    }

    @Test
    fun departmentOrganizationIsTakenFromItsParent() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"))
        whenCalled(dao.insert(anyArg())).thenReturn("dept-new")
        build().insert(create(parentId = "dept-a"))
        val dept = insertedEntity()
        assertEquals("org-1", dept.organizationId)
        assertEquals("dept-a", dept.parentId)
        verify(policy).assertCanManageDirectory("org-1")
    }

    @Test
    fun departmentUnderAParentOfAnotherOrganizationIsRefused() {
        stub(organizationRoot("org-1"), organizationRoot("org-2"), department("dept-b", "org-2"))
        // Trusted caller naming org-1 but a parent of org-2.
        val e = assertFailsWith<IllegalArgumentException> {
            build().insert(create(organizationId = "org-1", parentId = "dept-b"))
        }
        assertEquals("Parent and child departments must belong to the same organization", e.message)
        // Organization principal of org-1 placing a department under org-2.
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().insert(create(parentId = "dept-b")) }
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun departmentOfAnotherOrganizationIsRefusedForAnOrganizationPrincipal() {
        stub(organizationRoot("org-2"))
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().insert(create(organizationId = "org-2")) }
        verify(dao, never()).insert(anyArg())
        verifyNoInteractions(policy)
    }

    @Test
    fun departmentUnderALegacyParentIsRefused() {
        stub(organizationRoot("org-1"), legacyOrg("legacy", "tenant-a"))
        assertFailsWith<IllegalArgumentException> { build().insert(create(organizationId = "org-1", parentId = "legacy")) }
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun departmentNeedsAnActiveExistingRoot() {
        stub(organizationRoot("org-off", active = false), department("not-root", "org-1"))
        assertEquals(
            "ORGANIZATION_DISABLED",
            assertFailsWith<IllegalArgumentException> { build().insert(create(organizationId = "org-off")) }.message,
        )
        assertEquals(
            "ORGANIZATION_NOT_FOUND",
            assertFailsWith<IllegalArgumentException> { build().insert(create(organizationId = "missing")) }.message,
        )
        assertEquals(
            "ORGANIZATION_NOT_FOUND",
            assertFailsWith<IllegalArgumentException> { build().insert(create(organizationId = "not-root")) }.message,
        )
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun policyRefusalStopsTheDepartmentInsert() {
        stub(organizationRoot("org-1"))
        doThrow(IllegalArgumentException("NO")).`when`(policy).assertCanManageDirectory("org-1")
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().insert(create()) }
        verify(dao, never()).insert(anyArg())
    }

    @Test
    fun departmentsRequireOrganizationMode() {
        stub(organizationRoot("org-1"))
        assertFailsWith<IllegalStateException> { build(modeEnabled = false).insert(create(organizationId = "org-1")) }
        verify(dao, never()).insert(anyArg())
    }

    // ---- update ----

    @Test
    fun updateKeepsOwnershipColumnsWhenTheFormCarriesNulls() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertTrue(build().update(update("dept-a")))
        val entity = updatedEntity()
        assertEquals("dept-a", entity.id)
        assertEquals("renamed", entity.name)
        assertEquals(7, entity.sortNum)
        assertEquals("org-1", entity.organizationId)
        assertEquals(UserOrgNodeKind.DEPARTMENT, entity.nodeKind)
        assertEquals("", entity.tenantId)
        assertEquals("org-1", entity.parentId)
        verify(policy).assertCanManageDirectory("org-1")
    }

    @Test
    fun updateMayMoveADepartmentWithinItsOrganization() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"), department("dept-b", "org-1"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        assertTrue(build().update(update("dept-a", parentId = "dept-b")))
        assertEquals("dept-b", updatedEntity().parentId)
    }

    @Test
    fun updateCannotReassignOrganizationNodeKindOrParentAcrossOrganizations() {
        stub(organizationRoot("org-1"), organizationRoot("org-2"), department("dept-a", "org-1"), department("dept-b", "org-2"))
        val service = build()
        assertFailsWith<IllegalArgumentException> { service.update(update("dept-a", organizationId = "org-2")) }
        assertFailsWith<IllegalArgumentException> { service.update(update("dept-a", nodeKind = UserOrgNodeKind.ORGANIZATION)) }
        assertFailsWith<IllegalArgumentException> { service.update(update("dept-a", parentId = "dept-b")) }
        // A department cannot become its own ancestor.
        assertFailsWith<IllegalArgumentException> { service.update(update("dept-a", parentId = "dept-a")) }
        verify(dao, never()).update(anyArg())
    }

    @Test
    fun updateOfARootStaysParentlessAndIsPlatformOnly() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        assertTrue(build().update(update("org-1", parentId = "dept-a")))
        val entity = updatedEntity()
        assertNull(entity.parentId)
        assertEquals("org-1", entity.organizationId)
        assertEquals(UserOrgNodeKind.ORGANIZATION, entity.nodeKind)

        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().update(update("org-1")) }
    }

    @Test
    fun organizationPrincipalCannotUpdateNodesOfAnotherOrganization() {
        stub(organizationRoot("org-2"), department("dept-b", "org-2"))
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().update(update("dept-b")) }
        verify(dao, never()).update(anyArg())
        verifyNoInteractions(policy)
    }

    // ---- move ----

    @Test
    fun rootNeverMoves() {
        stub(organizationRoot("org-1"), organizationRoot("org-2"))
        val e = assertFailsWith<IllegalArgumentException> { build().moveOrg("org-1", "org-2", null) }
        assertEquals("An organization root cannot be moved", e.message)
        assertFailsWith<IllegalArgumentException> { build().moveOrg("org-1", null, 3) }
        verify(dao, never()).updateProperties(anyString(), anyMapArg())
    }

    @Test
    fun departmentMovedToNoParentGoesUnderItsRoot() {
        stub(organizationRoot("org-1"), department("dept-b", "org-1"), department("dept-a", "org-1", parentId = "dept-b"))
        whenCalled(dao.updateProperties(anyString(), anyMapArg())).thenReturn(true)
        assertTrue(build().moveOrg("dept-a", null, null))
        verify(dao).updateProperties("dept-a", mapOf(UserOrg::parentId.name to "org-1"))
        verify(policy).assertCanManageDirectory("org-1")
    }

    @Test
    fun departmentCannotMoveIntoAnotherOrganization() {
        stub(organizationRoot("org-1"), organizationRoot("org-2"), department("dept-a", "org-1"))
        assertFailsWith<IllegalArgumentException> { build().moveOrg("dept-a", "org-2", null) }
        verify(dao, never()).updateProperties(anyString(), anyMapArg())
    }

    // ---- delete guards ----

    @Test
    fun departmentWithSubDepartmentsOrMembersIsNotDeleted() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"), department("dept-b", "org-1"))
        whenCalled(dao.searchActiveChildOrgIds("dept-a")).thenReturn(listOf("child"))
        whenCalled(userOrgUserDao.searchUserIdsByOrgId("dept-b")).thenReturn(listOf("m1"))
        val service = build()
        assertEquals(
            "A department with sub-departments cannot be deleted",
            assertFailsWith<IllegalArgumentException> { service.deleteById("dept-a") }.message,
        )
        assertEquals(
            "A department with members cannot be deleted",
            assertFailsWith<IllegalArgumentException> { service.deleteById("dept-b") }.message,
        )
        verify(dao, never()).deleteById(anyString())
        verify(dao, never()).batchDeleteCriteria(anyCriteria())
    }

    @Test
    fun departmentWithOnlyInactiveChildrenIsNotDeletedEither() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"))
        whenCalled(dao.search(anyCriteria())).thenReturn(listOf(department("inactive-child", "org-1", "dept-a")))
        assertFailsWith<IllegalArgumentException> { build().deleteById("dept-a") }
        verify(dao, never()).deleteById(anyString())
    }

    @Test
    fun rootWithAccountsOrTenantsIsNotDeleted() {
        stub(organizationRoot("org-1"))
        val service = build()
        whenCalled(accountDao.search(anyCriteria())).thenReturn(listOf(UserAccount { id = "m1" }))
        assertEquals(
            "An organization with accounts cannot be deleted",
            assertFailsWith<IllegalArgumentException> { service.deleteById("org-1") }.message,
        )
        whenCalled(accountDao.search(anyCriteria())).thenReturn(emptyList())
        whenCalled(tenantDao.search(anyCriteria())).thenReturn(listOf(SysTenant { id = "tenant-a" }))
        assertEquals(
            "An organization that owns tenants cannot be deleted",
            assertFailsWith<IllegalArgumentException> { service.deleteById("org-1") }.message,
        )
        verify(dao, never()).deleteById(anyString())
        verify(dao, never()).batchDeleteCriteria(anyCriteria())
    }

    @Test
    fun deletingARootIsPlatformOnly() {
        stub(organizationRoot("org-1"))
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertFailsWith<IllegalArgumentException> { build().deleteById("org-1") }
        verify(dao, never()).deleteById(anyString())
        verifyNoInteractions(policy)
    }

    @Test
    fun emptyDepartmentIsDeletedAfterThePolicyAgrees() {
        stub(organizationRoot("org-1"), department("dept-a", "org-1"))
        whenCalled(dao.deleteById("dept-a")).thenReturn(true)
        whenCalled(dao.batchDeleteCriteria(anyCriteria())).thenReturn(1)
        signIn("m1", "tenant-a", organizationId = "org-1")
        assertTrue(build().deleteById("dept-a"))
        verify(policy).assertCanManageDirectory("org-1")
    }

    @Test
    fun batchDeleteChecksEveryOrganizationRowBeforeDeleting() {
        val ids = listOf("dept-a", "dept-b")
        whenCalled(dao.getByIds(ids)).thenReturn(listOf(department("dept-a", "org-1"), department("dept-b", "org-1")))
        whenCalled(userOrgUserDao.searchUserIdsByOrgId("dept-b")).thenReturn(listOf("m1"))
        assertFailsWith<IllegalArgumentException> { build().batchDelete(ids) }
        verify(dao, never()).batchDelete(ArgumentMatchers.anyCollection<String>() ?: emptyList())
        verify(dao, never()).batchDeleteCriteria(anyCriteria())
    }

    // ---- legacy rows ----

    @Test
    fun legacyInsertIsUnchangedWhenModeIsOff() {
        val form = create(tenantId = "tenant-a")
        whenCalled(dao.insert(form)).thenReturn("legacy-id")
        assertEquals("legacy-id", build(modeEnabled = false).insert(form))
        // The form itself goes to the DAO, exactly as before organization mode existed.
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(dao).insert(captured(captor))
        assertSame(form, captor.value)
        verifyNoInteractions(policy)
    }

    @Test
    fun legacyInsertWithoutGateBeanStillWorks() {
        val service = build(modeEnabled = false).also { setField(it, "directoryGate", null) }
        val form = create(tenantId = "tenant-a")
        whenCalled(dao.insert(form)).thenReturn("legacy-id")
        assertEquals("legacy-id", service.insert(form))
    }

    @Test
    fun legacyInsertIsRefusedOutsidePlatformTenantsWhenModeIsOn() {
        val service = build()
        val e = assertFailsWith<IllegalArgumentException> { service.insert(create(tenantId = "tenant-a")) }
        assertEquals("ORGANIZATION_REQUIRED", e.message)
        val form = create(tenantId = PLATFORM_TENANT)
        whenCalled(dao.insert(form)).thenReturn("platform-node")
        assertEquals("platform-node", service.insert(form))
    }

    @Test
    fun legacyUpdateAndMoveStayOnTheLegacyPath() {
        stub(legacyOrg("legacy", "tenant-a"), legacyOrg("legacy-parent", "tenant-a"))
        whenCalled(dao.update(anyArg())).thenReturn(true)
        whenCalled(dao.updateProperties(anyString(), anyMapArg())).thenReturn(true)
        signIn("u1", "tenant-a")
        val service = build()
        val form = update("legacy", tenantId = "tenant-a")
        assertTrue(service.update(form))
        val captor = ArgumentCaptor.forClass(Any::class.java)
        verify(dao).update(captured(captor))
        assertSame(form, captor.value)
        assertTrue(service.moveOrg("legacy", "legacy-parent", null))
        verify(dao).updateProperties("legacy", mapOf(UserOrg::parentId.name to "legacy-parent"))
        verifyNoInteractions(policy)
    }
}
