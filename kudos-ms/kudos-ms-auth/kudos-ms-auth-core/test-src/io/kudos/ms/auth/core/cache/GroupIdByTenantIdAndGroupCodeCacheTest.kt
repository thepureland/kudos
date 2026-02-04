package io.kudos.ms.auth.core.cache

import io.kudos.ms.auth.core.dao.AuthGroupDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for GroupIdByTenantIdAndGroupCodeCacheHandler
 *
 * 测试数据来源：`GroupIdByTenantIdAndGroupCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class GroupIdByTenantIdAndGroupCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: GroupIdByTenantIdAndGroupCodeCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    @Test
    fun getGroupId() {
        // 存在的
        var tenantId = "tenant-001-yoCqktm5"
        var code = "GROUP_ADMIN"
        val groupId2 = cacheHandler.getGroupId(tenantId, code)
        val groupId3 = cacheHandler.getGroupId(tenantId, code)
        assertNotNull(groupId2)
        assertEquals("7e2b8b93-1111-1111-1111-111111111111", groupId2)
        assertEquals(groupId2, groupId3)

        // 不存在的用户组编码
        code = "GROUP_NO_EXIST"
        assertNull(cacheHandler.getGroupId(tenantId, code))

        // 不存在的租户
        tenantId = "no_exist_tenant"
        code = "GROUP_ADMIN"
        assertNull(cacheHandler.getGroupId(tenantId, code))

        // inactive 用户组（只缓存 active=true 的）
        tenantId = "tenant-001-yoCqktm5"
        code = "GROUP_TEST"
        assertNull(cacheHandler.getGroupId(tenantId, code))
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis()
        val groupCode = "GROUP_TEST_INSERT_${timestamp}"
        val authGroup = AuthGroup.Companion().apply {
            this.tenantId = tenantId
            this.code = groupCode
            this.name = "测试用户组_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id = authGroupDao.insert(authGroup)

        // 同步缓存
        cacheHandler.syncOnInsert(authGroup, id)

        // 验证新记录是否在缓存中
        val groupId = cacheHandler.getGroupId(tenantId, groupCode)
        assertNotNull(groupId)
        assertEquals(id, groupId)
    }

    @Test
    fun syncOnUpdate() {
        val tenantId = "tenant-001-yoCqktm5"
        val groupCode = "GROUP_USER"
        val id = "7e2b8b93-2222-2222-2222-222222222222"

        // 先获取一次，确保缓存中有数据
        val groupIdBefore = cacheHandler.getGroupId(tenantId, groupCode)
        assertNotNull(groupIdBefore)

        // 更新数据库记录
        val success = authGroupDao.updateProperties(id, mapOf(AuthGroup::name.name to "更新后的用户组名"))
        assert(success)

        // 同步缓存
        cacheHandler.syncOnUpdate(null, id)

        // 验证缓存已被清除并重新加载
        val groupIdAfter = cacheHandler.getGroupId(tenantId, groupCode)
        assertNotNull(groupIdAfter)
        assertEquals(id, groupIdAfter)
    }

    @Test
    fun syncOnUpdateActive() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis()
        val groupCode = "GROUP_TEST_ACTIVE_${timestamp}"
        val authGroup = AuthGroup.Companion().apply {
            this.tenantId = tenantId
            this.code = groupCode
            this.name = "测试用户组_${timestamp}"
            this.subsysCode = "default"
            this.active = false
        }
        val id = authGroupDao.insert(authGroup)

        // 由false更新为true
        val success = authGroupDao.updateProperties(id, mapOf(AuthGroup::active.name to true))
        assert(success)
        cacheHandler.syncOnUpdateActive(id, true)
        var groupId = cacheHandler.getGroupId(tenantId, groupCode)
        assertNotNull(groupId)
        assertEquals(id, groupId)

        // 由true更新为false
        authGroupDao.updateProperties(id, mapOf(AuthGroup::active.name to false))
        cacheHandler.syncOnUpdateActive(id, false)
        groupId = cacheHandler.getGroupId(tenantId, groupCode)
        assertNull(groupId)
    }

    @Test
    fun syncOnDelete() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis()
        val groupCode = "GROUP_TEST_DELETE_${timestamp}"
        val authGroup = AuthGroup.Companion().apply {
            this.tenantId = tenantId
            this.code = groupCode
            this.name = "测试用户组_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id = authGroupDao.insert(authGroup)

        // 先获取一次，确保缓存中有数据
        val groupIdBefore = cacheHandler.getGroupId(tenantId, groupCode)
        assertNotNull(groupIdBefore)

        // 删除数据库记录
        val deleteSuccess = authGroupDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(authGroup, id)

        // 验证缓存已被清除
        val groupIdAfter = cacheHandler.getGroupId(tenantId, groupCode)
        assertNull(groupIdAfter)
    }

    @Test
    fun syncOnBatchDelete() {
        val tenantId = "tenant-001-yoCqktm5"
        val timestamp = System.currentTimeMillis() % 1000000000
        val groupCode1 = "G${timestamp}1" // 确保不超过32字符
        val groupCode2 = "G${timestamp}2" // 确保不超过32字符

        val authGroup1 = AuthGroup.Companion().apply {
            this.tenantId = tenantId
            this.code = groupCode1
            this.name = "测试用户组1_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id1 = authGroupDao.insert(authGroup1)

        val authGroup2 = AuthGroup.Companion().apply {
            this.tenantId = tenantId
            this.code = groupCode2
            this.name = "测试用户组2_${timestamp}"
            this.subsysCode = "default"
            this.active = true
        }
        val id2 = authGroupDao.insert(authGroup2)

        // 先获取一次，确保缓存中有数据
        val groupId1Before = cacheHandler.getGroupId(tenantId, groupCode1)
        val groupId2Before = cacheHandler.getGroupId(tenantId, groupCode2)
        assertNotNull(groupId1Before)
        assertNotNull(groupId2Before)

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = authGroupDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        val tenantAndCodes = listOf(Pair(tenantId, groupCode1), Pair(tenantId, groupCode2))
        cacheHandler.syncOnBatchDelete(ids, tenantAndCodes)

        // 验证缓存已被清除
        val groupId1After = cacheHandler.getGroupId(tenantId, groupCode1)
        val groupId2After = cacheHandler.getGroupId(tenantId, groupCode2)
        assertNull(groupId1After)
        assertNull(groupId2After)
    }

}
