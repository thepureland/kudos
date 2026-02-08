package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.user.common.vo.loginremember.UserLoginRememberMeCacheItem
import io.kudos.ms.user.core.dao.UserLoginRememberMeDao
import io.kudos.ms.user.core.model.po.UserLoginRememberMe
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for RememberMeByTenantIdAndUsernameCacheHandler
 *
 * 测试数据来源：`RememberMeByTenantIdAndUsernameCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class RememberMeByTenantIdAndUsernameCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: RememberMeByTenantIdAndUsernameCache

    @Resource
    private lateinit var dao: UserLoginRememberMeDao

    private val newToken = "token-updated"

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)

        val tenantId = "tenant-remember-1"
        val username = "remember_user1"
        val cacheItem = cacheHandler.getRememberMe(tenantId, username)
        assertNotNull(cacheItem)
        assertEquals("token-1", cacheItem.token)

        val cacheItemOtherTenant = cacheHandler.getRememberMe("tenant-remember-2", username)
        assertNotNull(cacheItemOtherTenant)
        assertEquals("token-3", cacheItemOtherTenant.token)

        val newRecord = insertNewRecordToDb(
            userId = "7c1a0000-0000-0000-0000-000000000002",
            tenantId = tenantId,
            username = "remember_user_new",
            token = "token-new"
        )

        val idUpdate = "8c1a0000-0000-0000-0000-000000000002"
        dao.updateProperties(idUpdate, mapOf(UserLoginRememberMe::token.name to newToken))

        val idDelete = "8c1a0000-0000-0000-0000-000000000003"
        dao.deleteById(idDelete)

        cacheHandler.reloadAll(false)

        val cacheItemNew = cacheHandler.getRememberMe(tenantId, "remember_user_new")
        assertNotNull(cacheItemNew)
        assertEquals(newRecord.id, cacheItemNew.id)

        val cacheItemUpdate = cacheHandler.getRememberMe(tenantId, "remember_user2")
        assertNotNull(cacheItemUpdate)
        assertEquals(newToken, cacheItemUpdate.token)

        val cacheItemDelete = cacheHandler.getRememberMe(tenantId, "remember_user_delete")
        assertNotNull(cacheItemDelete)

        cacheHandler.reloadAll(true)
        val cacheItemDeleteAfterClear = cacheHandler.getRememberMe(tenantId, "remember_user_delete")
        assertNull(cacheItemDeleteAfterClear)
    }

    @Test
    fun getRememberMe() {
        val tenantId = "tenant-remember-1"
        val username = "remember_user1"
        val cacheItem = cacheHandler.getRememberMe(tenantId, username)
        assertNotNull(cacheItem)
        assertEquals("token-1", cacheItem.token)

        val cacheItemOtherTenant = cacheHandler.getRememberMe("tenant-remember-2", username)
        assertNotNull(cacheItemOtherTenant)
        assertEquals("token-3", cacheItemOtherTenant.token)

        assertNull(cacheHandler.getRememberMe(tenantId, "no_exist_user"))
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-remember-1"
        val username = "remember_user_insert"
        val record = insertNewRecordToDb(
            userId = "7c1a0000-0000-0000-0000-000000000002",
            tenantId = tenantId,
            username = username,
            token = "token-insert"
        )

        cacheHandler.syncOnInsert(record, record.id!!)

        val key = cacheHandler.getKey(tenantId, username)
        @Suppress("UNCHECKED_CAST")
        val cacheItem = CacheKit.getValue(cacheHandler.cacheName(), key) as UserLoginRememberMeCacheItem?
        assertNotNull(cacheItem)
        assertEquals(record.id, cacheItem.id)
    }

    @Test
    fun syncOnUpdate() {
        val id = "8c1a0000-0000-0000-0000-000000000001"
        dao.updateProperties(id, mapOf(UserLoginRememberMe::token.name to newToken))
        val record = dao.getAs(id)!!

        cacheHandler.syncOnUpdate(record, id)

        val key = cacheHandler.getKey(record.tenantId, record.username)
        @Suppress("UNCHECKED_CAST")
        val cacheItem = CacheKit.getValue(cacheHandler.cacheName(), key) as UserLoginRememberMeCacheItem?
        assertNotNull(cacheItem)
        assertEquals(newToken, cacheItem.token)
    }

    @Test
    fun syncOnDelete() {
        val id = "8c1a0000-0000-0000-0000-000000000004"
        val record = dao.getAs(id)!!
        dao.deleteById(id)

        cacheHandler.syncOnDelete(record, id)

        val key = cacheHandler.getKey(record.tenantId, record.username)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getRememberMe(record.tenantId, record.username))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "8c1a0000-0000-0000-0000-000000000001"
        val id2 = "8c1a0000-0000-0000-0000-000000000002"
        val count = dao.batchDelete(listOf(id1, id2))
        assertEquals(2, count)

        val keys = listOf(
            Pair("tenant-remember-1", "remember_user1"),
            Pair("tenant-remember-1", "remember_user2")
        )
        cacheHandler.syncOnBatchDelete(listOf(id1, id2), keys)

        keys.forEach {
            val key = cacheHandler.getKey(it.first, it.second)
            assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        }
    }

    private fun insertNewRecordToDb(
        userId: String,
        tenantId: String,
        username: String,
        token: String
    ): UserLoginRememberMe {
        val record = UserLoginRememberMe {
            this.id = UUID.randomUUID().toString()
            this.userId = userId
            this.tenantId = tenantId
            this.username = username
            this.token = token
            this.lastUsed = LocalDateTime.now()
        }
        dao.insert(record)
        return record
    }
}
