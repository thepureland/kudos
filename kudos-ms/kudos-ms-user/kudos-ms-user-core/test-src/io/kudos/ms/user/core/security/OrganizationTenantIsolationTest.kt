package io.kudos.ms.user.core.security

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.org.vo.request.UserOrgQuery
import io.kudos.ms.user.common.org.vo.response.UserOrgDetail
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.impl.UserOrgService
import org.mockito.Mockito.*
import kotlin.test.*

internal class OrganizationTenantIsolationTest {
    private val dao = mock(UserOrgDao::class.java)
    private val service = UserOrgService(dao)

    @BeforeTest
    fun login() = bindPrincipal("operator", "tenant-a")

    @AfterTest
    fun clear() = KudosContextHolder.clear()

    @Test
    fun foreignOrganizationCrudAndMembershipReadsAreRejectedBeforeCacheAccess() {
        `when`(dao.get("foreign")).thenReturn(org("foreign", "tenant-b"))
        val actions: List<() -> Any?> = listOf(
            { service.get("foreign", UserOrgDetail::class) },
            { service.getOrgUsers("foreign") },
            { service.getOrgAdmins("foreign") },
            { service.getChildOrgs("foreign") },
            { service.getParentOrg("foreign") },
            { service.updateActive("foreign", false) },
            { service.moveOrg("foreign", null, null) },
            { service.update(org("foreign", "tenant-a")) },
            { service.deleteById("foreign") },
        )
        actions.forEach { assertFailsWith<IllegalArgumentException> { it() } }
        assertTrue(mockingDetails(dao).invocations.all { it.method.name == "get" })
    }

    @Test
    fun foreignTreeAndExplicitListFiltersAreRejectedBeforeQuerying() {
        assertFailsWith<IllegalArgumentException> { service.getOrgTree("tenant-b", null) }
        assertFailsWith<IllegalArgumentException> { service.pagingSearch(UserOrgQuery(tenantId = "tenant-b")) }
        verifyNoInteractions(dao)
    }

    @Test
    fun organizationCannotMoveUnderForeignParentOrItsOwnDescendant() {
        `when`(dao.get("own")).thenReturn(org("own", "tenant-a"))
        `when`(dao.get("foreign")).thenReturn(org("foreign", "tenant-b"))
        `when`(dao.get("child")).thenReturn(org("child", "tenant-a", "own"))
        assertFailsWith<IllegalArgumentException> { service.moveOrg("own", "foreign", null) }
        assertFailsWith<IllegalArgumentException> { service.moveOrg("own", "child", null) }
        assertTrue(mockingDetails(dao).invocations.all { it.method.name == "get" })
    }

    @Test
    fun tenantCannotBeReassignedThroughGenericUpdate() {
        `when`(dao.get("own")).thenReturn(org("own", "tenant-a"))
        assertFailsWith<IllegalArgumentException> { service.update(org("own", "tenant-b")) }
        assertTrue(mockingDetails(dao).invocations.all { it.method.name == "get" })
    }

    @Test
    fun batchChecksEveryTenantBeforeMutation() {
        val ids = listOf("own", "foreign")
        `when`(dao.getByIds(ids)).thenReturn(listOf(org("own", "tenant-a"), org("foreign", "tenant-b")))
        assertFailsWith<IllegalArgumentException> { service.batchDelete(ids) }
        assertEquals(listOf("getByIds"), mockingDetails(dao).invocations.map { it.method.name })
    }

    private fun org(id: String, tenant: String, parent: String? = null) = UserOrg {
        this.id = id; tenantId = tenant; parentId = parent; name = id
    }
}
