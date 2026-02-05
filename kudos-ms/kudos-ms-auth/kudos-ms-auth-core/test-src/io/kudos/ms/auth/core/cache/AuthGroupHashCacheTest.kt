package io.kudos.ms.auth.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.auth.core.dao.AuthGroupDao
import io.kudos.ms.auth.core.model.po.AuthGroup
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [AuthGroupHashCache] 单元测试（Hash 缓存，按 id 存取、按 tenantId+code 查询，不区分 active）。
 *
 * 覆盖：按 id 单条/批量获取、按租户+用户组编码获取用户组、全量刷新、新增/更新/删除/批量删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`sql/h2/cache/AuthGroupHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 AUTH_GROUP__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthGroupHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: AuthGroupHashCache

    @Resource
    private lateinit var authGroupDao: AuthGroupDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(AuthGroupHashCache.CACHE_NAME)

    private val newGroupName = "新用户组名称_${System.currentTimeMillis()}"

    private val tenant001 = "tenant-001-hashAuth"
    private val tenant002 = "tenant-002-hashAuth"
    private val groupId1 = "ag-hash-1111-1111-1111-1111111111111"
    private val groupId2 = "ag-hash-2222-2222-2222-2222222222222"

    @Test
    fun getGroupById() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getGroupById(groupId1)
        assertNotNull(item)
        assertEquals(groupId1, item.id)
        assertEquals("GROUP_ADMIN", item.code)
        assertEquals("管理员组", item.name)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getGroupById(groupId1)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getGroupById("no_exist_id"))
    }

    @Test
    fun getGroupsByIds() {
        cacheHandler.reloadAll(true)
        val result = cacheHandler.getGroupsByIds(listOf(groupId1, groupId2))
        assertEquals(2, result.size)
        assertNotNull(result[groupId1])
        assertNotNull(result[groupId2])
        assertEquals("GROUP_ADMIN", result[groupId1]?.code)
        assertEquals("GROUP_USER", result[groupId2]?.code)
        val resultAgain = cacheHandler.getGroupsByIds(listOf(groupId1, groupId2))
        if (isLocalCacheEnabled()) {
            assertSame(result[groupId1], resultAgain[groupId1])
            assertSame(result[groupId2], resultAgain[groupId2])
        }
        val partial = cacheHandler.getGroupsByIds(listOf("no_exist_id", groupId2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getGroupsByIds(emptyList()).isEmpty())
    }

    @Test
    fun getGroupByTenantIdAndGroupCode() {
        cacheHandler.reloadAll(true)
        val admin = cacheHandler.getGroupByTenantIdAndGroupCode(tenant001, "GROUP_ADMIN")
        assertNotNull(admin)
        assertEquals(groupId1, admin.id)
        assertEquals("GROUP_ADMIN", admin.code)
        val user = cacheHandler.getGroupByTenantIdAndGroupCode(tenant001, "GROUP_USER")
        assertNotNull(user)
        assertEquals(groupId2, user.id)
        val tenant2Admin = cacheHandler.getGroupByTenantIdAndGroupCode(tenant002, "GROUP_ADMIN")
        assertNotNull(tenant2Admin)
        assertEquals("ag-hash-5555-5555-5555-5555555555555", tenant2Admin.id)
        assertNull(cacheHandler.getGroupByTenantIdAndGroupCode(tenant001, "GROUP_NO_EXIST"))
        assertNull(cacheHandler.getGroupByTenantIdAndGroupCode("no_exist_tenant", "GROUP_ADMIN"))
        val adminAgain = cacheHandler.getGroupByTenantIdAndGroupCode(tenant001, "GROUP_ADMIN")
        if (isLocalCacheEnabled()) assertSame(admin, adminAgain)
    }

    @Test
    fun getGroupByTenantIdAndGroupCodeInactiveAlsoReturned() {
        cacheHandler.reloadAll(true)
        val inactive = cacheHandler.getGroupByTenantIdAndGroupCode(tenant001, "GROUP_TEST")
        assertNotNull(inactive, "不区分 active 时，inactive 用户组也应能按 tenantId+code 查到")
        assertEquals("ag-hash-4444-4444-4444-4444444444444", inactive.id)
        assertEquals(false, inactive.active)
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        cacheHandler.syncOnInsert(id)
        val item = cacheHandler.getGroupById(id)
        assertNotNull(item)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getGroupById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val success = authGroupDao.updateProperties(groupId2, mapOf(AuthGroup::name.name to newGroupName))
        assertTrue(success)
        cacheHandler.syncOnUpdate(groupId2)
        assertEquals(newGroupName, cacheHandler.getGroupById(groupId2)?.name)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        val count = authGroupDao.batchDelete(listOf(id))
        assertEquals(1, count)
        cacheHandler.syncOnDelete(id)
        assertNull(cacheHandler.getGroupById(id))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = authGroupDao.batchDelete(ids)
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(ids)
        assertNull(cacheHandler.getGroupById(id1))
        assertNull(cacheHandler.getGroupById(id2))
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val authGroup = AuthGroup.Companion().apply {
            code = "TEST_GROUP_${timestamp}"
            name = "测试用户组_${timestamp}"
            tenantId = tenant001
            subsysCode = "ams"
            active = true
            builtIn = false
        }
        return authGroupDao.insert(authGroup)
    }
}
