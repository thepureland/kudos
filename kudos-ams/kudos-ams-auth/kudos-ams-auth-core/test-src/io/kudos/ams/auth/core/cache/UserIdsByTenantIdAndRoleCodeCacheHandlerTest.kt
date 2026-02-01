package io.kudos.ams.auth.core.cache

import io.kudos.ams.auth.core.dao.AuthRoleDao
import io.kudos.ams.auth.core.dao.AuthRoleUserDao
import io.kudos.ams.auth.core.model.po.AuthRole
import io.kudos.ams.auth.core.model.po.AuthRoleUser
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByTenantIdAndRoleCodeCacheHandler
 *
 * 测试数据来源：`UserIdsByTenantIdAndRoleCodeCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByTenantIdAndRoleCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByTenantIdAndRoleCodeCacheHandler

    @Resource
    private lateinit var authRoleDao: AuthRoleDao

    @Resource
    private lateinit var authRoleUserDao: AuthRoleUserDao

    @Test
    fun getUserIds() {
        // 存在的租户和角色
        var tenantId = "tenant-001-58TWQx6c"
        var roleCode = "ROLE_ADMIN"
        val userIds2 = cacheHandler.getUserIds(tenantId, roleCode)
        val userIds3 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的角色
        roleCode = "ROLE_NO_EXIST"
        val userIds4 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        roleCode = "ROLE_ADMIN"
        val userIds5 = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIds5.isEmpty())
    }

    @Test
    fun syncOnRoleUserInsert() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userId = "10796e8c-3333-3333-3333-333333333333"
        
        // 先获取一次，记录初始用户数量
        val userIdsBefore = cacheHandler.getUserIds(tenantId, roleCode)
        val beforeSize = userIdsBefore.size
        
        // 插入一条新的角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 同步缓存（模拟角色-用户关系新增）
        cacheHandler.syncOnRoleUserInsert(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsAfter.size > beforeSize, "同步后应该包含新插入的用户ID")
        assertTrue(userIdsAfter.contains(userId), "应该包含新插入的用户ID")
        
        // 清理测试数据
        authRoleUserDao.deleteById(id)
    }

    @Test
    fun syncOnRoleUserDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        val userId = "10796e8c-3333-3333-3333-333333333333"
        
        // 先插入一条角色-用户关系记录
        val authRoleUser = AuthRoleUser.Companion().apply {
            this.roleId = roleId
            this.userId = userId
        }
        val id = authRoleUserDao.insert(authRoleUser)
        
        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnRoleUserInsert(tenantId, roleCode)
        
        // 获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsBefore.contains(userId), "新插入的用户关系应该在缓存中")
        
        // 删除数据库记录
        val deleteSuccess = authRoleUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")
        
        // 同步缓存（模拟角色-用户关系删除）
        cacheHandler.syncOnRoleUserDelete(tenantId, roleCode)
        
        // 验证缓存已被清除并重新加载，应该不包含已删除的用户
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(!userIdsAfter.contains(userId), "删除后，缓存应该被清除，不应该包含已删除的用户ID")
    }

    @Test
    fun syncOnRoleUpdate() {
        val oldTenantId = "tenant-001-58TWQx6c"
        val oldRoleCode = "ROLE_USER"
        val newTenantId = "tenant-001-58TWQx6c"
        val newRoleCode = "ROLE_USER_UPDATED"
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        
        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(oldTenantId, oldRoleCode)
        
        // 更新角色编码
        val role = authRoleDao.get(roleId)
        assertTrue(role != null, "角色应该存在")
        val success = authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to newRoleCode))
        assertTrue(success, "更新应该成功")
        
        // 同步缓存（模拟角色信息更新）
        cacheHandler.syncOnRoleUpdate(oldTenantId, oldRoleCode, newTenantId, newRoleCode)
        
        // 验证旧缓存已被清除，新缓存可以获取数据
        val userIdsNew = cacheHandler.getUserIds(newTenantId, newRoleCode)
        // 旧缓存应该被清除，新缓存应该能获取到数据（用户关系不变，只是角色编码变了）
        assertEquals(
            userIdsBefore.size,
            userIdsNew.size,
            "新角色编码应该能获取到相同的用户列表"
        )
        
        // 恢复角色编码
        authRoleDao.updateProperties(roleId, mapOf(AuthRole::code.name to oldRoleCode))
    }

    @Test
    fun syncOnRoleDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val roleCode = "ROLE_USER"
        
        // 先获取一次，确保缓存中有数据（即使为空列表）
        cacheHandler.getUserIds(tenantId, roleCode)
        
        // 删除数据库中的角色记录
        val roleId = "10796e8c-2222-2222-2222-222222222222" // ROLE_USER 的 ID
        authRoleDao.deleteById(roleId)
        
        // 同步缓存（模拟角色删除）
        cacheHandler.syncOnRoleDelete(tenantId, roleCode)
        
        // 验证缓存已被清除，重新获取应该返回空列表（因为角色已不存在）
        val userIdsAfter = cacheHandler.getUserIds(tenantId, roleCode)
        assertTrue(userIdsAfter.isEmpty(), "删除角色后，缓存应该被清除，重新获取应该返回空列表")
    }

}
