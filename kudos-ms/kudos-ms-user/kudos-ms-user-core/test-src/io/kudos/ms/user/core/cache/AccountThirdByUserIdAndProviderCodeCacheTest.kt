package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.user.common.vo.user.UserAccountThirdCacheItem
import io.kudos.ms.user.core.dao.UserAccountThirdDao
import io.kudos.ms.user.core.model.po.UserAccountThird
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for AccountThirdByUserIdAndProviderCodeCacheHandler
 *
 * 测试数据来源：`AccountThirdByUserIdAndProviderCodeCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AccountThirdByUserIdAndProviderCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: AccountThirdByUserIdAndProviderCodeCache

    @Resource
    private lateinit var dao: UserAccountThirdDao

    private val newEmail = "updated@example.com"

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)

        val userId = "9a1a0000-0000-0000-0000-000000000001"
        val providerCode = "WX"
        val cacheItem = cacheHandler.getAccountThird(userId, providerCode)
        assertNotNull(cacheItem)

        // inactive 不应缓存
        assertNull(cacheHandler.getAccountThird(userId, "GITHUB"))

        val newRecord = insertNewRecordToDb(userId, "ALIPAY", "sub-new", true)

        val idUpdate = "9b1a0000-0000-0000-0000-000000000002"
        dao.updateProperties(idUpdate, mapOf(UserAccountThird::externalEmail.name to newEmail))

        val idDelete = "9b1a0000-0000-0000-0000-000000000004"
        dao.deleteById(idDelete)

        cacheHandler.reloadAll(false)

        val cacheItemNew = cacheHandler.getAccountThird(userId, "ALIPAY")
        assertNotNull(cacheItemNew)
        assertEquals(newRecord.id, cacheItemNew.id)

        val cacheItemUpdate = cacheHandler.getAccountThird(userId, "QQ")
        assertNotNull(cacheItemUpdate)
        assertEquals(newEmail, cacheItemUpdate.externalEmail)

        val userId2 = "9a1a0000-0000-0000-0000-000000000002"
        val cacheItemDelete = cacheHandler.getAccountThird(userId2, "WX")
        assertNotNull(cacheItemDelete)

        cacheHandler.reloadAll(true)
        val cacheItemDeleteAfterClear = cacheHandler.getAccountThird(userId2, "WX")
        assertNull(cacheItemDeleteAfterClear)
    }

    @Test
    fun getAccountThird() {
        val userId = "9a1a0000-0000-0000-0000-000000000001"
        val cacheItem = cacheHandler.getAccountThird(userId, "WX")
        assertNotNull(cacheItem)

        // inactive 不应缓存
        assertNull(cacheHandler.getAccountThird(userId, "GITHUB"))

        // 不存在
        assertNull(cacheHandler.getAccountThird(userId, "NO_PROVIDER"))
    }

    @Test
    fun syncOnInsert() {
        val userId = "9a1a0000-0000-0000-0000-000000000001"
        val accountThird = insertNewRecordToDb(userId, "DING", "sub-ding", true)

        cacheHandler.syncOnInsert(accountThird, accountThird.id!!)

        val key = cacheHandler.getKey(userId, "DING")
        @Suppress("UNCHECKED_CAST")
        val cacheItem = CacheKit.getValue(cacheHandler.cacheName(), key) as UserAccountThirdCacheItem?
        assertNotNull(cacheItem)
        assertEquals(accountThird.id, cacheItem.id)
    }

    @Test
    fun syncOnUpdate() {
        val id = "9b1a0000-0000-0000-0000-000000000002"
        val success = dao.updateProperties(id, mapOf(UserAccountThird::externalEmail.name to newEmail))
        assertTrue(success)
        val accountThird = dao.get(id)!!

        cacheHandler.syncOnUpdate(accountThird, id)

        val key = cacheHandler.getKey(accountThird.userId, accountThird.accountProviderDictCode)
        @Suppress("UNCHECKED_CAST")
        val cacheItem = CacheKit.getValue(cacheHandler.cacheName(), key) as UserAccountThirdCacheItem?
        assertNotNull(cacheItem)
        assertEquals(newEmail, cacheItem.externalEmail)
    }

    @Test
    fun syncOnUpdateActive() {
        var id = "9b1a0000-0000-0000-0000-000000000002"
        var success = dao.updateProperties(id, mapOf(UserAccountThird::active.name to false))
        assertTrue(success)
        cacheHandler.syncOnUpdateActive(id, false)
        val userId = "9a1a0000-0000-0000-0000-000000000001"
        val key = cacheHandler.getKey(userId, "QQ")
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getAccountThird(userId, "QQ"))

        id = "9b1a0000-0000-0000-0000-000000000003"
        success = dao.updateProperties(id, mapOf(UserAccountThird::active.name to true))
        assertTrue(success)
        cacheHandler.syncOnUpdateActive(id, true)
        val key2 = cacheHandler.getKey(userId, "GITHUB")
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key2))
        assertNotNull(cacheHandler.getAccountThird(userId, "GITHUB"))
    }

    @Test
    fun syncOnDelete() {
        val id = "9b1a0000-0000-0000-0000-000000000001"
        val accountThird = dao.get(id)!!
        val deleteSuccess = dao.deleteById(id)
        assertTrue(deleteSuccess)

        cacheHandler.syncOnDelete(accountThird, id)

        val key = cacheHandler.getKey(accountThird.userId, accountThird.accountProviderDictCode)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getAccountThird(accountThird.userId, accountThird.accountProviderDictCode))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "9b1a0000-0000-0000-0000-000000000001"
        val id2 = "9b1a0000-0000-0000-0000-000000000002"
        val userId = "9a1a0000-0000-0000-0000-000000000001"
        val keys = listOf(
            Pair(userId, "WX"),
            Pair(userId, "QQ")
        )

        val count = dao.batchDelete(listOf(id1, id2))
        assertEquals(2, count)

        cacheHandler.syncOnBatchDelete(listOf(id1, id2), keys)

        keys.forEach {
            assertNull(CacheKit.getValue(cacheHandler.cacheName(), cacheHandler.getKey(it.first, it.second)))
        }
    }

    private fun insertNewRecordToDb(
        userId: String,
        providerCode: String,
        subject: String,
        active: Boolean
    ): UserAccountThird {
        val accountThird = UserAccountThird {
            this.userId = userId
            this.accountProviderDictCode = providerCode
            this.accountProviderIssuer = null
            this.subject = subject
            this.unionId = null
            this.externalDisplayName = providerCode.lowercase()
            this.externalEmail = "${providerCode.lowercase()}@example.com"
            this.tenantId = "tenant-third-1"
            this.active = active
            this.builtIn = false
        }
        dao.insert(accountThird)
        return accountThird
    }
}
