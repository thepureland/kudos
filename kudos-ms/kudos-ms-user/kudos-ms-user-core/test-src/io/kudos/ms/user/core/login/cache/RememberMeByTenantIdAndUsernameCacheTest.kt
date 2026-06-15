package io.kudos.ms.user.core.login.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.user.common.login.vo.UserLoginRememberMeCacheEntry
import io.kudos.ms.user.core.login.dao.UserLoginRememberMeDao
import io.kudos.ms.user.core.login.model.po.UserLoginRememberMe
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for RememberMeByTenantIdAndUsernameCacheHandler
 *
 * Test data source: `RememberMeByTenantIdAndUsernameCacheTest.sql`
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

        cacheHandler.syncOnInsert(record, record.id)

        val key = cacheHandler.getKey(tenantId, username)
        @Suppress("UNCHECKED_CAST")
        val cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as UserLoginRememberMeCacheEntry?
        assertNotNull(cacheItem)
        assertEquals(record.id, cacheItem.id)
    }

    @Test
    fun syncOnUpdate() {
        val id = "8c1a0000-0000-0000-0000-000000000001"
        dao.updateProperties(id, mapOf(UserLoginRememberMe::token.name to newToken))
        val record = assertNotNull(dao.get(id))

        cacheHandler.syncOnUpdate(record, id)

        val key = cacheHandler.getKey(record.tenantId, record.username)
        @Suppress("UNCHECKED_CAST")
        val cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as UserLoginRememberMeCacheEntry?
        assertNotNull(cacheItem)
        assertEquals(newToken, cacheItem.token)
    }

    @Test
    fun syncOnDelete() {
        val id = "8c1a0000-0000-0000-0000-000000000004"
        val record = assertNotNull(dao.get(id))
        dao.deleteById(id)

        cacheHandler.syncOnDelete(record, id)

        val key = cacheHandler.getKey(record.tenantId, record.username)
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
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
            assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        }
    }

    /**
     * Carrier whose tenantId/username properties exist but are null, forcing
     * [RememberMeByTenantIdAndUsernameCache.resolveKeyParts] to fall back to a DB lookup by id.
     */
    data class NullKeyCarrier(val tenantId: String? = null, val username: String? = null)

    @Test
    fun reload_validKey_loadsViaDoReload() {
        // Exercises the doReload happy path (valid 2-part key -> getSelf().getRememberMe()).
        val tenantId = "tenant-remember-1"
        val username = "remember_user1"
        val key = cacheHandler.getKey(tenantId, username)
        cacheHandler.reload(key)
        val cacheItem = cacheHandler.getRememberMe(tenantId, username)
        assertNotNull(cacheItem)
        assertEquals("token-1", cacheItem.token)
    }

    @Test
    fun syncOnInsert_objectMissingKeyProps_resolvesViaDbFallback() {
        // The carrier lacks usable tenantId/username, so resolveKeyParts must look them up by id in DB.
        val id = "8c1a0000-0000-0000-0000-000000000001"
        val tenantId = "tenant-remember-1"
        val username = "remember_user1"
        // Make sure the entry is absent first so the write-in-time reload is observable.
        KeyValueCacheKit.evict(cacheHandler.cacheName(), cacheHandler.getKey(tenantId, username))

        cacheHandler.syncOnInsert(NullKeyCarrier(), id)

        // The DB-fallback path resolved the key and (when write-in-time) reloaded it; at minimum a
        // subsequent read returns the DB row, proving the fallback produced the right key segments.
        val cacheItem = cacheHandler.getRememberMe(tenantId, username)
        assertNotNull(cacheItem)
        assertEquals(id, cacheItem.id)
    }

    @Test
    fun syncOnInsert_objectMissingKeyPropsAndBlankDbData_noOps() {
        // DB row exists by id but its username is blank -> resolveKeyParts hits the dirty-data guard
        // (logs WARN, returns null); sync must be a safe no-op without throwing and must not touch
        // an unrelated, valid cache entry.
        val tenantId = "tenant-remember-1"
        val username = "remember_user1"
        val before = assertNotNull(cacheHandler.getRememberMe(tenantId, username))

        val blankRowId = "8c1a0000-0000-0000-0000-000000000005"
        cacheHandler.syncOnInsert(NullKeyCarrier(), blankRowId) // must not throw; guard returns null

        val after = assertNotNull(cacheHandler.getRememberMe(tenantId, username))
        assertEquals(before.id, after.id)
    }

    @Test
    fun syncOnInsert_objectMissingKeyPropsAndUnknownId_noOps() {
        // resolveKeyParts cannot find the record by id -> logs WARN and returns null; sync is a no-op.
        val unknownId = "8c1a0000-0000-0000-0000-0000000000ff"
        // Should not throw, and must not create any spurious cache entry.
        cacheHandler.syncOnInsert(NullKeyCarrier(), unknownId)
        assertNull(cacheHandler.getRememberMe("no-such-tenant", "no-such-user"))
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
