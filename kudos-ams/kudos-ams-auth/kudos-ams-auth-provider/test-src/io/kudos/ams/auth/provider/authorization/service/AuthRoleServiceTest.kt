package io.kudos.ams.auth.provider.authorization.service

import io.kudos.ams.auth.provider.authorization.service.iservice.IAuthRoleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for AuthRoleService
 *
 * 测试数据来源：`AuthRoleServiceTest.sql`
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
        val tenantId = "svc-tenant-role-test-1"
        val roleCode = "svc-role-test-1"
        val cacheItem = authRoleService.getRoleByTenantIdAndCode(tenantId, roleCode)
        assertNotNull(cacheItem)
        assertTrue(cacheItem.code == roleCode)
        
        // 测试不存在的角色
        val notExist = authRoleService.getRoleByTenantIdAndCode(tenantId, "non-existent")
        assertNull(notExist)
    }

    @Test
    fun getRoleRecord() {
        val id = "30000000-0000-0000-0000-000000000025"
        val record = authRoleService.getRoleRecord(id)
        assertNotNull(record)
        assertTrue(record.code == "svc-role-test-1")
        
        // 测试不存在的角色
        val notExist = authRoleService.getRoleRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getRolesByTenantId() {
        val tenantId = "svc-tenant-role-test-1"
        val roles = authRoleService.getRolesByTenantId(tenantId)
        assertTrue(roles.size >= 4)
        assertTrue(roles.any { it.code == "svc-role-test-1" })
        assertTrue(roles.any { it.code == "svc-role-test-2" })
    }

    @Test
    fun getRolesBySubsysCode() {
        val tenantId = "svc-tenant-role-test-1"
        val subsysCode = "ams"
        val roles = authRoleService.getRolesBySubsysCode(tenantId, subsysCode)
        assertTrue(roles.size >= 3)
        assertTrue(roles.any { it.code == "svc-role-test-1" })
        assertTrue(roles.any { it.code == "svc-role-test-2" })
        
        // 测试另一个子系统
        val subsysCode2 = "svc-subsys-role-test-1"
        val roles2 = authRoleService.getRolesBySubsysCode(tenantId, subsysCode2)
        assertTrue(roles2.any { it.code == "svc-role-test-3" })
    }

    @Test
    fun updateActive() {
        val id = "30000000-0000-0000-0000-000000000025"
        // 先设置为false
        assertTrue(authRoleService.updateActive(id, false))
        var role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertFalse(role.active == true)
        
        // 再设置为true
        assertTrue(authRoleService.updateActive(id, true))
        role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertTrue(role.active == true)
    }
}
