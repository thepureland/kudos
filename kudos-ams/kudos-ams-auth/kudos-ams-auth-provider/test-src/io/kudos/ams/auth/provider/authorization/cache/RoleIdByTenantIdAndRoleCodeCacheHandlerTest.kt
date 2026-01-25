package io.kudos.ams.auth.provider.authorization.cache

import io.kudos.ams.auth.provider.authorization.dao.AuthRoleDao
import io.kudos.ams.auth.provider.authorization.model.po.AuthRole
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for RoleIdByTenantIdAndRoleCodeCacheHandler
 *
 * 测试数据来源：`RoleIdByTenantIdAndRoleCodeCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RoleIdByTenantIdAndRoleCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: RoleIdByTenantIdAndRoleCodeCacheHandler

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Test
    fun getRoleId() {
        // 存在的
        var tenantId = "tenant-001-yoCqktm5"
        var code = "ROLE_ADMIN"
        val roleId2 = cacheHandler.getRoleId(tenantId, code)
        val roleId3 = cacheHandler.getRoleId(tenantId, code)
        assertNotNull(roleId2)
        assertEquals("6e2b8b93-1111-1111-1111-111111111111", roleId2)
        assertEquals(roleId2, roleId3)

        // 不存在的角色编码
        code = "ROLE_NO_EXIST"
        assertNull(cacheHandler.getRoleId(tenantId, code))

        // 不存在的租户
        tenantId = "no_exist_tenant"
        code = "ROLE_ADMIN"
        assertNull(cacheHandler.getRoleId(tenantId, code))

        // inactive 角色（只缓存 active=true 的）
        tenantId = "tenant-001-yoCqktm5"
        code = "ROLE_TEST"
        assertNull(cacheHandler.getRoleId(tenantId, code))
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis()
        val roleCode = "ROLE_TEST_INSERT_${timestamp}"
        val authRole = AuthRole().apply {
            this.tenantId = tenantId
            this.code = roleCode
            this.name = "测试角色_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id = authRoleDao.insert(authRole)

        // 同步缓存
        cacheHandler.syncOnInsert(authRole, id)

        // 验证新记录是否在缓存中
        val roleId = cacheHandler.getRoleId(tenantId, roleCode)
        assertNotNull(roleId)
        assertEquals(id, roleId)
    }

    @Test
    fun syncOnUpdate() {
        val tenantId = "tenant-001-yoCqktm5"
        val roleCode = "ROLE_USER"
        val id = "6e2b8b93-2222-2222-2222-222222222222"
        
        // 先获取一次，确保缓存中有数据
        val roleIdBefore = cacheHandler.getRoleId(tenantId, roleCode)
        assertNotNull(roleIdBefore)

        // 更新数据库记录
        val success = authRoleDao.updateProperties(id, mapOf(AuthRole::name.name to "更新后的角色名"))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(null, id)

        // 验证缓存已被清除并重新加载
        val roleIdAfter = cacheHandler.getRoleId(tenantId, roleCode)
        assertNotNull(roleIdAfter)
        assertEquals(id, roleIdAfter)
    }

    @Test
    fun syncOnUpdateActive() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis()
        val roleCode = "ROLE_TEST_ACTIVE_${timestamp}"
        val authRole = AuthRole().apply {
            this.tenantId = tenantId
            this.code = roleCode
            this.name = "测试角色_${timestamp}"
            this.subsysCode = "default"
            this.active = false
        }
        val id = authRoleDao.insert(authRole)

        // 由false更新为true
        val success = authRoleDao.updateProperties(id, mapOf(AuthRole::active.name to true))
        assert(success)
        cacheHandler.syncOnUpdateActive(id, true)
        var roleId = cacheHandler.getRoleId(tenantId, roleCode)
        assertNotNull(roleId)
        assertEquals(id, roleId)

        // 由true更新为false
        authRoleDao.updateProperties(id, mapOf(AuthRole::active.name to false))
        cacheHandler.syncOnUpdateActive(id, false)
        roleId = cacheHandler.getRoleId(tenantId, roleCode)
        assertNull(roleId)
    }

    @Test
    fun syncOnDelete() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis()
        val roleCode = "ROLE_TEST_DELETE_${timestamp}"
        val authRole = AuthRole().apply {
            this.tenantId = tenantId
            this.code = roleCode
            this.name = "测试角色_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id = authRoleDao.insert(authRole)

        // 先获取一次，确保缓存中有数据
        val roleIdBefore = cacheHandler.getRoleId(tenantId, roleCode)
        assertNotNull(roleIdBefore)

        // 删除数据库记录
        val deleteSuccess = authRoleDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(authRole, id)

        // 验证缓存已被清除
        val roleIdAfter = cacheHandler.getRoleId(tenantId, roleCode)
        assertNull(roleIdAfter)
    }

    @Test
    fun syncOnBatchDelete() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis() % 1000000000
        val roleCode1 = "R${timestamp}1" // 确保不超过32字符
        val roleCode2 = "R${timestamp}2" // 确保不超过32字符
        
        val authRole1 = AuthRole().apply {
            this.tenantId = tenantId
            this.code = roleCode1
            this.name = "测试角色1_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id1 = authRoleDao.insert(authRole1)
        
        val authRole2 = AuthRole().apply {
            this.tenantId = tenantId
            this.code = roleCode2
            this.name = "测试角色2_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id2 = authRoleDao.insert(authRole2)

        // 先获取一次，确保缓存中有数据
        val roleId1Before = cacheHandler.getRoleId(tenantId, roleCode1)
        val roleId2Before = cacheHandler.getRoleId(tenantId, roleCode2)
        assertNotNull(roleId1Before)
        assertNotNull(roleId2Before)

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = authRoleDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        val tenantAndCodes = listOf(Pair(tenantId, roleCode1), Pair(tenantId, roleCode2))
        cacheHandler.syncOnBatchDelete(ids, tenantAndCodes)

        // 验证缓存已被清除
        val roleId1After = cacheHandler.getRoleId(tenantId, roleCode1)
        val roleId2After = cacheHandler.getRoleId(tenantId, roleCode2)
        assertNull(roleId1After)
        assertNull(roleId2After)
    }

}
