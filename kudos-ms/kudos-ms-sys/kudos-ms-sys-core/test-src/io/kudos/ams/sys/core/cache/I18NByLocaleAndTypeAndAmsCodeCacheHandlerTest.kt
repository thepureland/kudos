package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.ms.sys.core.dao.SysI18nDao
import io.kudos.ms.sys.core.model.po.SysI18n
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for I18NByLocaleAndTypeAndAmsCodeCacheHandler
 *
 * 测试数据来源：`I18NByLocaleAndTypeAndAmsCodeCacheHandler.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class I18NByLocaleAndTypeAndAmsCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: I18NByLocaleAndTypeAndAmsCodeCacheHandler

    @Resource
    private lateinit var dao: SysI18nDao

    private val newValue = "value-updated"

    override fun getTestDataSqlPath(): String {
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        return "sql/${rdbType.name.lowercase()}/cache/I18NByLocaleAndTypeAndAmsCodeCacheHandler.sql"
    }

    @Test
    fun reloadAll() {
        cacheHandler.reloadAll(true)

        val locale = "zh_CN"
        val i18nTypeDictCode = "label"
        val atomicServiceCode = "as-i18n-test-1"
        val cacheItems = cacheHandler.getI18ns(locale, i18nTypeDictCode, atomicServiceCode)
        assertTrue(cacheItems.size >= 5)
        assertFalse(cacheItems.containsKey("i18n.key.3"))

        val deleteLocale = "en_US"
        val deleteType = "label"
        val deleteAtomicServiceCode = "as-i18n-test-2"
        val deleteCacheItems = cacheHandler.getI18ns(deleteLocale, deleteType, deleteAtomicServiceCode)
        assertTrue(deleteCacheItems.isNotEmpty())

        val newKey = "i18n.key.new"
        insertNewRecordToDb(locale, i18nTypeDictCode, atomicServiceCode, newKey, "value-new")

        val idUpdate = "40000000-0000-0000-0000-000000000208"
        dao.updateProperties(idUpdate, mapOf(SysI18n::value.name to newValue))

        val idDelete = "40000000-0000-0000-0000-000000000207"
        dao.deleteById(idDelete)

        cacheHandler.reloadAll(false)

        val cacheItemsNew = cacheHandler.getI18ns(locale, i18nTypeDictCode, atomicServiceCode)
        assertTrue(cacheItemsNew.containsKey(newKey))
        assertEquals(newValue, cacheItemsNew["i18n.key.4"])

        val cacheItemsDeleteGroup = cacheHandler.getI18ns(deleteLocale, deleteType, deleteAtomicServiceCode)
        assertTrue(cacheItemsDeleteGroup.containsKey("i18n.key.2"))

        cacheHandler.reloadAll(true)
        val cacheItemsDeleteGroupCleared = cacheHandler.getI18ns(deleteLocale, deleteType, deleteAtomicServiceCode)
        assertTrue(cacheItemsDeleteGroupCleared.isEmpty())
    }

    @Test
    fun getI18ns() {
        val cacheItems = cacheHandler.getI18ns("zh_CN", "label", "as-i18n-test-1")
        assertTrue(cacheItems.containsKey("i18n.key.1"))
        assertFalse(cacheItems.containsKey("i18n.key.3"))

        val emptyItems = cacheHandler.getI18ns("ja_JP", "label", "as-i18n-test-1")
        assertTrue(emptyItems.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        val locale = "zh_CN"
        val i18nTypeDictCode = "label"
        val atomicServiceCode = "as-i18n-test-1"
        val key = "i18n.key.insert"
        val i18n = insertNewRecordToDb(locale, i18nTypeDictCode, atomicServiceCode, key, "value-insert")

        cacheHandler.syncOnInsert(i18n, i18n.id!!)

        val cacheKey = cacheHandler.getKey(locale, i18nTypeDictCode, atomicServiceCode)
        @Suppress("UNCHECKED_CAST")
        val cacheMap = CacheKit.getValue(cacheHandler.cacheName(), cacheKey) as Map<String, String>?
        assertNotNull(cacheMap)
        assertTrue(cacheMap.containsKey(key))
        assertTrue(cacheHandler.getI18ns(locale, i18nTypeDictCode, atomicServiceCode).containsKey(key))
    }

    @Test
    fun syncOnUpdate() {
        val id = "40000000-0000-0000-0000-000000000208"
        dao.updateProperties(id, mapOf(SysI18n::value.name to newValue))
        val i18n = dao.get(id)!!

        cacheHandler.syncOnUpdate(i18n, id)

        val cacheKey = cacheHandler.getKey(i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode)
        @Suppress("UNCHECKED_CAST")
        val cacheMap = CacheKit.getValue(cacheHandler.cacheName(), cacheKey) as Map<String, String>?
        assertNotNull(cacheMap)
        assertEquals(newValue, cacheMap["i18n.key.4"])
        assertEquals(newValue, cacheHandler.getI18ns(i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode)["i18n.key.4"])
    }

    @Test
    fun syncOnUpdateActive() {
        var id = "40000000-0000-0000-0000-000000000210"
        var success = dao.updateProperties(id, mapOf(SysI18n::active.name to false))
        assertTrue(success)
        cacheHandler.syncOnUpdateActive(id, false)
        val key1 = cacheHandler.getKey("zh_CN", "label", "as-i18n-test-1")
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key1))
        assertFalse(cacheHandler.getI18ns("zh_CN", "label", "as-i18n-test-1").containsKey("i18n.key.6"))

        id = "40000000-0000-0000-0000-000000000211"
        success = dao.updateProperties(id, mapOf(SysI18n::active.name to true))
        assertTrue(success)
        cacheHandler.syncOnUpdateActive(id, true)
        val key2 = cacheHandler.getKey("zh_CN", "label", "as-i18n-test-1")
        assertNotNull(CacheKit.getValue(cacheHandler.cacheName(), key2))
        assertTrue(cacheHandler.getI18ns("zh_CN", "label", "as-i18n-test-1").containsKey("i18n.key.7"))
    }

    @Test
    fun syncOnDelete() {
        val id = "40000000-0000-0000-0000-000000000209"
        val i18n = dao.get(id)!!
        val deleteSuccess = dao.deleteById(id)
        assertTrue(deleteSuccess)

        cacheHandler.syncOnDelete(i18n, id)

        val key = cacheHandler.getKey(i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode)
        assertNull(CacheKit.getValue(cacheHandler.cacheName(), key))
        assertFalse(cacheHandler.getI18ns(i18n.locale, i18n.i18nTypeDictCode, i18n.atomicServiceCode).containsKey("i18n.key.5"))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "40000000-0000-0000-0000-000000000201"
        val id2 = "40000000-0000-0000-0000-000000000204"
        val i18n1 = dao.get(id1)!!
        val i18n2 = dao.get(id2)!!
        val keys = listOf(
            cacheHandler.getKey(i18n1.locale, i18n1.i18nTypeDictCode, i18n1.atomicServiceCode),
            cacheHandler.getKey(i18n2.locale, i18n2.i18nTypeDictCode, i18n2.atomicServiceCode)
        )

        val count = dao.batchDelete(listOf(id1, id2))
        assertEquals(2, count)

        cacheHandler.syncOnBatchDelete(listOf(id1, id2), keys)

        keys.forEach {
            assertNull(CacheKit.getValue(cacheHandler.cacheName(), it))
        }
    }

    private fun insertNewRecordToDb(
        locale: String,
        i18nTypeDictCode: String,
        atomicServiceCode: String,
        key: String,
        value: String
    ): SysI18n {
        val i18n = SysI18n {
            this.locale = locale
            this.atomicServiceCode = atomicServiceCode
            this.i18nTypeDictCode = i18nTypeDictCode
            this.key = key
            this.value = value
            this.active = true
        }
        dao.insert(i18n)
        return i18n
    }
}
