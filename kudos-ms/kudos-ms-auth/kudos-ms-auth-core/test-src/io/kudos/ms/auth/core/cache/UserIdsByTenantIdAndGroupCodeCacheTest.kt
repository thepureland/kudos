package io.kudos.ms.auth.core.cache

import io.kudos.ms.auth.core.dao.AuthGroupDao
import io.kudos.ms.auth.core.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.ms.auth.core.model.po.AuthGroupUser
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByTenantIdAndGroupCodeCacheHandler
 *
 * 测试数据来源：`UserIdsByTenantIdAndGroupCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByTenantIdAndGroupCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserIdsByTenantIdAndGroupCodeCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Resource
    private lateinit var authGroupUserDao: AuthGroupUserDao

    @Test
    fun getUserIds() {
        // 存在的租户和用户组
        var tenantId = "tenant-001-58TWQx6c"
        var groupCode = "GROUP_ADMIN"
        val userIds2 = cacheHandler.getUserIds(tenantId, groupCode)
        val userIds3 = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的用户组
        groupCode = "GROUP_NO_EXIST"
        val userIds4 = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIds4.isEmpty())

        // 不存在的租户
        tenantId = "no_exist_tenant"
        groupCode = "GROUP_ADMIN"
        val userIds5 = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIds5.isEmpty())
    }

    @Test
    fun syncOnGroupUserInsert() {
        val tenantId = "tenant-001-58TWQx6c"
        val groupCode = "GROUP_USER"
        val groupId = "20796e8c-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        val userId = "20796e8c-3333-3333-3333-333333333333"

        // 先获取一次，记录初始用户数量
        val userIdsBefore = cacheHandler.getUserIds(tenantId, groupCode)
        val beforeSize = userIdsBefore.size

        // 插入一条新的用户组-用户关系记录
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // 同步缓存（模拟用户组-用户关系新增）
        cacheHandler.syncOnGroupUserInsert(tenantId, groupCode)

        // 验证缓存已被清除并重新加载，应该包含新插入的用户
        val userIdsAfter = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIdsAfter.size > beforeSize, "同步后应该包含新插入的用户ID")
        assertTrue(userIdsAfter.contains(userId), "应该包含新插入的用户ID")

        // 清理测试数据
        authGroupUserDao.deleteById(id)
    }

    @Test
    fun syncOnGroupUserDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val groupCode = "GROUP_USER"
        val groupId = "20796e8c-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        val userId = "20796e8c-3333-3333-3333-333333333333"

        // 先插入一条用户组-用户关系记录
        val authGroupUser = AuthGroupUser.Companion().apply {
            this.groupId = groupId
            this.userId = userId
        }
        val id = authGroupUserDao.insert(authGroupUser)

        // 先同步缓存，确保缓存中有新插入的数据
        cacheHandler.syncOnGroupUserInsert(tenantId, groupCode)

        // 获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIdsBefore.contains(userId), "新插入的用户关系应该在缓存中")

        // 删除数据库记录
        val deleteSuccess = authGroupUserDao.deleteById(id)
        assertTrue(deleteSuccess, "删除应该成功")

        // 同步缓存（模拟用户组-用户关系删除）
        cacheHandler.syncOnGroupUserDelete(tenantId, groupCode)

        // 验证缓存已被清除并重新加载，应该不包含已删除的用户
        val userIdsAfter = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(!userIdsAfter.contains(userId), "删除后，缓存应该被清除，不应该包含已删除的用户ID")
    }

    @Test
    fun syncOnGroupUpdate() {
        val oldTenantId = "tenant-001-58TWQx6c"
        val oldGroupCode = "GROUP_USER"
        val newTenantId = "tenant-001-58TWQx6c"
        val newGroupCode = "GROUP_USER_UPDATED"
        val groupId = "20796e8c-2222-2222-2222-222222222222" // GROUP_USER 的 ID

        // 先获取一次，确保缓存中有数据
        val userIdsBefore = cacheHandler.getUserIds(oldTenantId, oldGroupCode)

        // 更新用户组编码
        val group = authGroupDao.get(groupId)
        assertTrue(group != null, "用户组应该存在")
        val success = authGroupDao.updateProperties(groupId, mapOf(AuthGroup::code.name to newGroupCode))
        assertTrue(success, "更新应该成功")

        // 同步缓存（模拟用户组信息更新）
        cacheHandler.syncOnGroupUpdate(oldTenantId, oldGroupCode, newTenantId, newGroupCode)

        // 验证旧缓存已被清除，新缓存可以获取数据
        val userIdsNew = cacheHandler.getUserIds(newTenantId, newGroupCode)
        // 旧缓存应该被清除，新缓存应该能获取到数据（用户关系不变，只是用户组编码变了）
        assertEquals(
            userIdsBefore.size,
            userIdsNew.size,
            "新用户组编码应该能获取到相同的用户列表"
        )

        // 恢复用户组编码
        authGroupDao.updateProperties(groupId, mapOf(AuthGroup::code.name to oldGroupCode))
    }

    @Test
    fun syncOnGroupDelete() {
        val tenantId = "tenant-001-58TWQx6c"
        val groupCode = "GROUP_USER"

        // 先获取一次，确保缓存中有数据（即使为空列表）
        cacheHandler.getUserIds(tenantId, groupCode)

        // 删除数据库中的用户组记录
        val groupId = "20796e8c-2222-2222-2222-222222222222" // GROUP_USER 的 ID
        authGroupDao.deleteById(groupId)

        // 同步缓存（模拟用户组删除）
        cacheHandler.syncOnGroupDelete(tenantId, groupCode)

        // 验证缓存已被清除，重新获取应该返回空列表（因为用户组已不存在）
        val userIdsAfter = cacheHandler.getUserIds(tenantId, groupCode)
        assertTrue(userIdsAfter.isEmpty(), "删除用户组后，缓存应该被清除，重新获取应该返回空列表")
    }

}
