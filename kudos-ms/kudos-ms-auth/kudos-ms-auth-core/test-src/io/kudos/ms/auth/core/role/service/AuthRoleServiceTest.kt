package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for AuthRoleService
 *
 * Test data source: `AuthRoleServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleService: IAuthRoleService

    @Test
    fun getRoleByTenantIdAndCode() {
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val roleCode = "svc-role-test-1-bq0Y0mrl"
        val cacheItem = authRoleService.getRoleByTenantIdAndCode(tenantId, roleCode)
        assertNotNull(cacheItem)
        assertEquals(cacheItem.code, roleCode)
        
        // Test a non-existent role.
        val notExist = authRoleService.getRoleByTenantIdAndCode(tenantId, "non-existent")
        assertNull(notExist)
    }

    @Test
    fun getRoleRecord() {
        val id = "249363d1-0000-0000-0000-000000000025"
        val record = authRoleService.getRoleRecord(id)
        assertNotNull(record)
        assertEquals(record.code, "svc-role-test-1-bq0Y0mrl")
        
        // Test a non-existent role.
        val notExist = authRoleService.getRoleRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getRolesByTenantId() {
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val roles = authRoleService.getRolesByTenantId(tenantId)
        assertTrue(roles.size >= 4)
        assertTrue(roles.any { it.code == "svc-role-test-1-bq0Y0mrl" })
        assertTrue(roles.any { it.code == "svc-role-test-2-bq0Y0mrl" })
    }

    @Test
    fun getRolesBySubSystemCode() {
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val subSystemCode = "ams"
        val roles = authRoleService.getRolesBySubsysCode(tenantId, subSystemCode)
        assertTrue(roles.size >= 3)
        assertTrue(roles.any { it.code == "svc-role-test-1-bq0Y0mrl" })
        assertTrue(roles.any { it.code == "svc-role-test-2-bq0Y0mrl" })
        
        // Test another subsystem.
        val subSystemCode2 = "svc-subsys-role-test-1-bq0Y0mrl"
        val roles2 = authRoleService.getRolesBySubsysCode(tenantId, subSystemCode2)
        assertTrue(roles2.any { it.code == "svc-role-test-3-bq0Y0mrl" })
    }

    @Test
    fun updateActive() {
        val id = "249363d1-0000-0000-0000-000000000025"
        // First set to false
        assertTrue(authRoleService.updateActive(id, false))
        var role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertNotEquals(role.active, true)
        
        // Then set to true
        assertTrue(authRoleService.updateActive(id, true))
        role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertEquals(role.active, true)
    }

    @Test
    fun getUsersByRoleCode() {
        val tenantId = "svc-tenant-user-test-1-249363d1"
        val roleCode = "svc-role-user-test-1-249363d1"
        val users = authRoleService.getUsersByRoleCode(tenantId, roleCode)
        assertTrue(users.size >= 2)
        assertTrue(users.any { it.username == "svc-user-test-1-249363d1" })
        assertTrue(users.any { it.username == "svc-user-test-2-249363d1" })
    }

    // -----------------------------------------------------------------------
    // Role inheritance (parent_id): ancestor walk + parent validation.
    // Fixture chain: root(030) <- mid(031) <- leaf(032)
    // -----------------------------------------------------------------------

    private val rootRoleId = "249363d1-0000-0000-0000-000000000030"
    private val midRoleId = "249363d1-0000-0000-0000-000000000031"
    private val leafRoleId = "249363d1-0000-0000-0000-000000000032"
    private val otherTenantRoleId = "249363d1-0000-0000-0000-000000000033"
    private val otherSubsysRoleId = "249363d1-0000-0000-0000-000000000034"

    @Test
    fun getAncestorRoleIds_returnsChainDirectParentFirst() {
        val ancestors = authRoleService.getAncestorRoleIds(leafRoleId)
        // Nearest ancestor first: mid, then root.
        assertEquals(listOf(midRoleId, rootRoleId), ancestors)
    }

    @Test
    fun getAncestorRoleIds_rootHasNoAncestors() {
        assertTrue(authRoleService.getAncestorRoleIds(rootRoleId).isEmpty())
    }

    @Test
    fun update_settingSelfAsParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = leafRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingDescendantAsParent_rejected() {
        // root's parent = leaf, but leaf is root's descendant -> cycle.
        val form = AuthRoleFormUpdate(
            id = rootRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = leafRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingCrossTenantParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = otherTenantRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingCrossSubsystemParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = otherSubsysRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingNonExistentParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = "non-existent-parent-id", remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingValidAncestorReparent_succeeds() {
        // Re-parent leaf directly under root (skipping mid) — same tenant + subsystem, no cycle.
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = rootRoleId, remark = null,
        )
        assertTrue(authRoleService.update(form))
        assertEquals(listOf(rootRoleId), authRoleService.getAncestorRoleIds(leafRoleId))
    }

}
