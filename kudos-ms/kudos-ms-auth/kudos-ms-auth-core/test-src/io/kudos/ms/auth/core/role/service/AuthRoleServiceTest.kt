package io.kudos.ms.auth.core.role.service

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

    // ---------- Role inheritance (parent_id) ----------

    private val rootRoleId = "249363d1-0000-0000-0000-000000000040"
    private val midRoleId = "249363d1-0000-0000-0000-000000000041"
    private val leafRoleId = "249363d1-0000-0000-0000-000000000042"
    private val otherSubsysRoleId = "249363d1-0000-0000-0000-000000000043"
    private val otherTenantRoleId = "249363d1-0000-0000-0000-000000000044"

    @Test
    fun getAncestorRoleIds_returnsChainDirectParentFirst() {
        val chain = authRoleService.getAncestorRoleIds(leafRoleId)
        assertEquals(listOf(midRoleId, rootRoleId), chain, "leaf -> mid -> root, ordered closest first")
    }

    @Test
    fun getAncestorRoleIds_rootHasNoAncestors() {
        assertTrue(authRoleService.getAncestorRoleIds(rootRoleId).isEmpty())
    }

    @Test
    fun update_settingSelfAsParent_rejected() {
        val form = io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate(
            id = midRoleId,
            code = null,
            name = null,
            tenantId = null,
            subsysCode = null,
            parentId = midRoleId,
            remark = null,
        )
        val err = assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
        assertTrue(err.message!!.contains("itself") || err.message!!.contains("its own parent"))
    }

    @Test
    fun update_settingDescendantAsParent_rejected() {
        // Try to make leaf the parent of mid → would close mid <-> leaf cycle.
        val form = io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate(
            id = midRoleId,
            code = null,
            name = null,
            tenantId = null,
            subsysCode = null,
            parentId = leafRoleId,
            remark = null,
        )
        val err = assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
        assertTrue(err.message!!.contains("cycle"))
    }

    @Test
    fun update_settingCrossTenantParent_rejected() {
        val form = io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate(
            id = midRoleId,
            code = null,
            name = null,
            tenantId = "svc-tenant-inh-1",
            subsysCode = "ams",
            parentId = otherTenantRoleId,
            remark = null,
        )
        val err = assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
        assertTrue(err.message!!.contains("tenant"))
    }

    @Test
    fun update_settingCrossSubsystemParent_rejected() {
        val form = io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate(
            id = midRoleId,
            code = null,
            name = null,
            tenantId = "svc-tenant-inh-1",
            subsysCode = "ams",
            parentId = otherSubsysRoleId,
            remark = null,
        )
        val err = assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
        assertTrue(err.message!!.contains("subsystem"))
    }

    @Test
    fun update_settingNonExistentParent_rejected() {
        val form = io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate(
            id = midRoleId,
            code = null,
            name = null,
            tenantId = null,
            subsysCode = null,
            parentId = "00000000-0000-0000-0000-deadbeefdead",
            remark = null,
        )
        val err = assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
        assertTrue(err.message!!.contains("not found"))
    }

}
