package io.kudos.ms.auth.core.service

import io.kudos.ms.auth.core.service.iservice.IAuthRoleService
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
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val roleCode = "svc-role-test-1-bq0Y0mrl"
        val cacheItem = authRoleService.getRoleByTenantIdAndCode(tenantId, roleCode)
        assertNotNull(cacheItem)
        assertEquals(cacheItem.code, roleCode)
        
        // 测试不存在的角色
        val notExist = authRoleService.getRoleByTenantIdAndCode(tenantId, "non-existent")
        assertNull(notExist)
    }

    @Test
    fun getRoleRecord() {
        val id = "249363d1-0000-0000-0000-000000000025"
        val record = authRoleService.getRoleRecord(id)
        assertNotNull(record)
        assertEquals(record.code, "svc-role-test-1-bq0Y0mrl")
        
        // 测试不存在的角色
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
        
        // 测试另一个子系统
        val subSystemCode2 = "svc-subsys-role-test-1-bq0Y0mrl"
        val roles2 = authRoleService.getRolesBySubsysCode(tenantId, subSystemCode2)
        assertTrue(roles2.any { it.code == "svc-role-test-3-bq0Y0mrl" })
    }

    @Test
    fun updateActive() {
        val id = "249363d1-0000-0000-0000-000000000025"
        // 先设置为false
        assertTrue(authRoleService.updateActive(id, false))
        var role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertNotEquals(role.active, true)
        
        // 再设置为true
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

}
