package io.kudos.ms.user.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ms.user.common.vo.contact.UserContactWayCacheItem
import io.kudos.ms.user.core.dao.UserContactWayDao
import io.kudos.ms.user.core.model.po.UserContactWay
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for UserContactWayByUserIdCacheHandler
 *
 * 测试数据来源：`UserContactWayByUserIdCacheTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserContactWayByUserIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: UserContactWayByUserIdCache

    @Resource
    private lateinit var dao: UserContactWayDao

    private val newValue = "contact-way-updated"

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)

        val userId = "7a1a0000-0000-0000-0000-000000000001"
        val cacheItems = cacheHandler.getContactWays(userId)
        assertTrue(cacheItems.size >= 2)
        assertFalse(cacheItems.any { it.id == "8b1a0000-0000-0000-0000-000000000003" })

        val newContact = insertNewRecordToDb(userId, "01", "13300000009", true)

        val idUpdate = "8b1a0000-0000-0000-0000-000000000004"
        dao.updateProperties(idUpdate, mapOf(UserContactWay::contactWayValue.name to newValue))

        val idDelete = "8b1a0000-0000-0000-0000-000000000005"
        dao.deleteById(idDelete)

        cacheHandler.reloadAll(false)

        val cacheItemsNew = cacheHandler.getContactWays(userId)
        assertTrue(cacheItemsNew.any { it.id == newContact.id })

        val userId2 = "7a1a0000-0000-0000-0000-000000000002"
        val cacheItemsUpdate = cacheHandler.getContactWays(userId2)
        assertEquals(newValue, cacheItemsUpdate.first { it.id == idUpdate }.contactWayValue)
        assertFalse(cacheItemsUpdate.any { it.id == idDelete })

        cacheHandler.reloadAll(true)
        val cacheItemsDelete = cacheHandler.getContactWays(userId2)
        assertFalse(cacheItemsDelete.any { it.id == idDelete })
    }

    @Test
    fun getContactWays() {
        val userId = "7a1a0000-0000-0000-0000-000000000001"
        val cacheItems = cacheHandler.getContactWays(userId)
        assertTrue(cacheItems.isNotEmpty())
        assertFalse(cacheItems.any { it.id == "8b1a0000-0000-0000-0000-000000000003" })

        val userIdNoContact = "7a1a0000-0000-0000-0000-000000000003"
        assertTrue(cacheHandler.getContactWays(userIdNoContact).isEmpty())

        val userIdNotExist = "no_exist_user_id"
        assertTrue(cacheHandler.getContactWays(userIdNotExist).isEmpty())
    }

    @Test
    fun syncOnInsert() {
        val userId = "7a1a0000-0000-0000-0000-000000000001"
        val contactWay = insertNewRecordToDb(userId, "02", "sync_insert@example.com", true)

        cacheHandler.syncOnInsert(contactWay, contactWay.id!!)

        val key = cacheHandler.getKey(userId)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<UserContactWayCacheItem>?
        assertNotNull(cacheItems)
        assertTrue(cacheItems.any { it.id == contactWay.id })
        assertTrue(cacheHandler.getContactWays(userId).any { it.id == contactWay.id })
    }

    @Test
    fun syncOnUpdate() {
        val id = "8b1a0000-0000-0000-0000-000000000004"
        val success = dao.updateProperties(id, mapOf(UserContactWay::contactWayValue.name to newValue))
        assertTrue(success)
        val contactWay = dao.getAs(id)!!

        cacheHandler.syncOnUpdate(contactWay, id)

        val key = cacheHandler.getKey(contactWay.userId!!)
        @Suppress("UNCHECKED_CAST")
        val cacheItems = CacheKit.getValue(cacheHandler.cacheName(), key) as List<UserContactWayCacheItem>?
        assertNotNull(cacheItems)
        assertEquals(newValue, cacheItems.first { it.id == id }.contactWayValue)
        assertEquals(newValue, cacheHandler.getContactWays(contactWay.userId!!).first { it.id == id }.contactWayValue)
    }

    @Test
    fun syncOnUpdateActive() {
        var id = "8b1a0000-0000-0000-0000-000000000002"
        var success = dao.updateProperties(id, mapOf(UserContactWay::active.name to false))
        assertTrue(success)
        cacheHandler.syncOnUpdateActive(id, false)
        val userId = "7a1a0000-0000-0000-0000-000000000001"
        val key = cacheHandler.getKey(userId)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertFalse(cacheHandler.getContactWays(userId).any { it.id == id })

        id = "8b1a0000-0000-0000-0000-000000000003"
        success = dao.updateProperties(id, mapOf(UserContactWay::active.name to true))
        assertTrue(success)
        cacheHandler.syncOnUpdateActive(id, true)
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertTrue(cacheHandler.getContactWays(userId).any { it.id == id })
    }

    @Test
    fun syncOnDelete() {
        val id = "8b1a0000-0000-0000-0000-000000000001"
        val contactWay = dao.getAs(id)!!
        val deleteSuccess = dao.deleteById(id)
        assertTrue(deleteSuccess)

        cacheHandler.syncOnDelete(contactWay, id)

        val key = cacheHandler.getKey(contactWay.userId!!)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertFalse(cacheHandler.getContactWays(contactWay.userId!!).any { it.id == id })
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "8b1a0000-0000-0000-0000-000000000004"
        val id2 = "8b1a0000-0000-0000-0000-000000000005"
        val userId1 = "7a1a0000-0000-0000-0000-000000000002"
        val ids = listOf(id1, id2)

        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        cacheHandler.syncOnBatchDelete(ids, listOf(userId1))

        val key = cacheHandler.getKey(userId1)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
    }

    private fun insertNewRecordToDb(
        userId: String,
        contactWayDictCode: String,
        contactWayValue: String,
        active: Boolean
    ): UserContactWay {
        val contactWay = UserContactWay {
            this.userId = userId
            this.contactWayDictCode = contactWayDictCode
            this.contactWayValue = contactWayValue
            this.contactWayStatusDictCode = "00"
            this.priority = 1
            this.active = active
            this.builtIn = false
        }
        dao.insert(contactWay)
        return contactWay
    }
}
