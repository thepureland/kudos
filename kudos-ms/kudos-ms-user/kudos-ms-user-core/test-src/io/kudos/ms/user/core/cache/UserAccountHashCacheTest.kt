package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.user.core.dao.UserAccountDao
import io.kudos.ms.user.core.model.po.UserAccount
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
 * [UserAccountHashCache] 单元测试（Hash 缓存，按 id 存取、按 tenantId+username 查询）。
 *
 * 覆盖：按 id 单条/批量获取、按租户+用户名获取用户、全量刷新、新增/更新/删除/批量删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`sql/h2/cache/UserAccountHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 USER_ACCOUNT__HASH（hash=true）。
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserAccountHashCache

    @Resource
    private lateinit var userAccountDao: UserAccountDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(UserAccountHashCache.CACHE_NAME)

    private val newUsername = "new_test_user_${System.currentTimeMillis()}"

    private val tenant001 = "tenant-001-ujdERXYn"
    private val tenant002 = "tenant-002-ujdERXYn"
    private val userId1 = "61146119-1111-1111-1111-111111111111"
    private val userId2 = "61146119-2222-2222-2222-222222222222"

    @Test
    fun getUserById() {
        cacheHandler.reloadAll(true)
        val item = cacheHandler.getUserById(userId1)
        assertNotNull(item)
        assertEquals(userId1, item.id)
        assertEquals("admin", item.username)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getUserById(userId1)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getUserById("no_exist_id"))
    }

    @Test
    fun getUsersByIds() {
        cacheHandler.reloadAll(true)
        val result = cacheHandler.getUsersByIds(listOf(userId1, userId2))
        assertEquals(2, result.size)
        assertNotNull(result[userId1])
        assertNotNull(result[userId2])
        assertEquals("admin", result[userId1]?.username)
        assertEquals("zhangsan", result[userId2]?.username)
        val resultAgain = cacheHandler.getUsersByIds(listOf(userId1, userId2))
        if (isLocalCacheEnabled()) {
            assertSame(result[userId1], resultAgain[userId1])
            assertSame(result[userId2], resultAgain[userId2])
        }
        val partial = cacheHandler.getUsersByIds(listOf("no_exist_id", userId2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getUsersByIds(emptyList()).isEmpty())
    }

    @Test
    fun getUsersByTenantIdAndUsername() {
        cacheHandler.reloadAll(true)
        val admin = cacheHandler.getUsersByTenantIdAndUsername(tenant001, "admin")
        assertNotNull(admin)
        assertEquals(userId1, admin.id)
        assertEquals("admin", admin.username)
        val zhangsan = cacheHandler.getUsersByTenantIdAndUsername(tenant001, "zhangsan")
        assertNotNull(zhangsan)
        assertEquals(userId2, zhangsan.id)
        val zhaoliu = cacheHandler.getUsersByTenantIdAndUsername(tenant002, "zhaoliu")
        assertNotNull(zhaoliu)
        assertEquals("61146119-5555-5555-5555-555555555555", zhaoliu.id)
        assertNull(cacheHandler.getUsersByTenantIdAndUsername(tenant001, "no_exist_user"))
        assertNull(cacheHandler.getUsersByTenantIdAndUsername("no_exist_tenant", "admin"))
        val adminAgain = cacheHandler.getUsersByTenantIdAndUsername(tenant001, "admin")
        if (isLocalCacheEnabled()) assertSame(admin, adminAgain)
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        cacheHandler.syncOnInsert(id)
        val item = cacheHandler.getUserById(id)
        assertNotNull(item)
        assertEquals(tenant001, item.tenantId)
        val itemAgain = cacheHandler.getUserById(id)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val success = userAccountDao.updateProperties(userId2, mapOf(UserAccount::username.name to newUsername))
        assertTrue(success)
        cacheHandler.syncOnUpdate(userId2)
        assertEquals(newUsername, cacheHandler.getUserById(userId2)?.username)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val id = insertNewRecordToDb()
        val count = userAccountDao.batchDelete(listOf(id))
        assertEquals(1, count)
        cacheHandler.syncOnDelete(id)
        assertNull(cacheHandler.getUserById(id))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = userAccountDao.batchDelete(ids)
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(ids)
        assertNull(cacheHandler.getUserById(id1))
        assertNull(cacheHandler.getUserById(id2))
    }

    private fun insertNewRecordToDb(): String {
        val timestamp = System.currentTimeMillis()
        val userAccount = UserAccount().apply {
            username = "u${timestamp % 1000000000}"
            tenantId = tenant001
            loginPassword = "password"
            supervisorId = userId1
            active = true
        }
        return userAccountDao.insert(userAccount)
    }
}
