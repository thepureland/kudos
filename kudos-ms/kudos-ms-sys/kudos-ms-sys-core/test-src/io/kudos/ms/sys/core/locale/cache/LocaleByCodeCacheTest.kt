package io.kudos.ms.sys.core.locale.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.locale.vo.SysLocaleCacheEntry
import io.kudos.ms.sys.core.locale.dao.SysLocaleDao
import io.kudos.ms.sys.core.locale.event.SysLocaleBatchDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleDeleted
import io.kudos.ms.sys.core.locale.event.SysLocaleInserted
import io.kudos.ms.sys.core.locale.event.SysLocaleUpdated
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


/**
 * junit test for LocaleByCodeCache
 *
 * Test data source: `LocaleByCodeCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class LocaleByCodeCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: LocaleByCodeCache

    @Resource
    private lateinit var dao: SysLocaleDao

    @Test
    fun reloadAll_then_getLocale() {
        cacheHandler.reloadAll(clear = true)
        val item = cacheHandler.getLocale("ko_KR")
        assertNotNull(item)
        assertEquals("ko_KR", item.code)
    }

    /** Items with active=false should not be present in the cache. */
    @Test
    fun getLocale_inactive_returnsNull() {
        cacheHandler.reloadAll(clear = true)
        assertNull(cacheHandler.getLocale("ru_RU"))
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(clear = true)
        val newCode = "test_locale_insert"
        val po = SysLocale().apply {
            code = newCode
            displayName = "Test"
            englishName = "Test Insert"
            sortNo = 9000
            active = true
            builtIn = false
        }
        dao.insert(po)

        cacheHandler.syncOnInsert(po, po.id)

        val viaCache = KeyValueCacheKit.getValue(cacheHandler.cacheName(), newCode) as? SysLocaleCacheEntry
        assertNotNull(viaCache)
        val viaHandler = cacheHandler.getLocale(newCode)
        assertNotNull(viaHandler)
        assertEquals(newCode, viaHandler.code)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(clear = true)
        val id = "30000000-0000-0000-0000-000000006001" // ko_KR seeded
        val newDisplay = "Korean-Updated"
        val success = dao.updateProperties(id, mapOf(SysLocale::displayName.name to newDisplay))
        assert(success)

        cacheHandler.syncOnUpdate(null, id)

        val cached = cacheHandler.getLocale("ko_KR")
        assertNotNull(cached)
        assertEquals(newDisplay, cached.displayName)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(clear = true)
        val code = "ko_KR"
        assertNotNull(cacheHandler.getLocale(code))

        cacheHandler.syncOnDelete(code)

        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), code))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(clear = true)
        val codes = setOf("ko_KR", "it_IT")
        // Both should initially be active
        assertNotNull(cacheHandler.getLocale("ko_KR"))
        assertNotNull(cacheHandler.getLocale("it_IT"))

        cacheHandler.syncOnBatchDelete(codes)

        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), "ko_KR"))
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), "it_IT"))
    }

    /** syncOnUpdate with a non-null business object reads the code from the object rather than the DB. */
    @Test
    fun syncOnUpdate_withNonNullEntity() {
        cacheHandler.reloadAll(clear = true)
        val id = "30000000-0000-0000-0000-000000006001" // ko_KR seeded
        val newDisplay = "Korean-NonNull"
        dao.updateProperties(id, mapOf(SysLocale::displayName.name to newDisplay))
        val po = assertNotNull(dao.get(id))

        cacheHandler.syncOnUpdate(po, id)

        val cached = cacheHandler.getLocale("ko_KR")
        assertNotNull(cached)
        assertEquals(newDisplay, cached.displayName)
    }

    // ---------- transactional event listeners (delegate to syncOn*) ----------

    @Test
    fun onEvents_delegateToSync() {
        cacheHandler.reloadAll(clear = true)
        // inserted -> looks up the PO by id then syncOnInsert
        val newCode = "test_locale_on_insert"
        val po = SysLocale().apply {
            code = newCode
            displayName = "On-Insert"
            englishName = "On Insert"
            sortNo = 9100
            active = true
            builtIn = false
        }
        dao.insert(po)
        cacheHandler.on(SysLocaleInserted(id = po.id))
        assertNotNull(cacheHandler.getLocale(newCode))

        // inserted event for a missing id is a no-op (PO lookup returns null)
        cacheHandler.on(SysLocaleInserted(id = "00000000-0000-0000-0000-0000000000aa"))

        // updated -> syncOnUpdate
        dao.updateProperties(po.id, mapOf(SysLocale::displayName.name to "On-Insert-Updated"))
        cacheHandler.on(SysLocaleUpdated(id = po.id))
        assertEquals("On-Insert-Updated", cacheHandler.getLocale(newCode)?.displayName)

        // deleted -> syncOnDelete by code
        dao.deleteById(po.id)
        cacheHandler.on(SysLocaleDeleted(id = po.id, code = newCode))
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), newCode))

        // batch deleted -> syncOnBatchDelete by codes
        cacheHandler.reloadAll(clear = true)
        assertNotNull(cacheHandler.getLocale("ko_KR"))
        cacheHandler.on(SysLocaleBatchDeleted(ids = listOf("30000000-0000-0000-0000-000000006001"), codes = setOf("ko_KR")))
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), "ko_KR"))
    }
}
