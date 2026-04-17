package io.kudos.ms.sys.core.i18n.service

import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysI18NService
 *
 * 测试数据来源：`SysI18NServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysI18NServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysI18nService: ISysI18nService

    @Resource
    private lateinit var sysI18nHashCache: SysI18nHashCache

    private val seededIdZh = "20000000-0000-0000-0000-000000008449"
    private val locale = "zh_CN"
    private val i18nTypeDictCode = "label"
    private val namespace = "label"
    private val atomicServiceCode = "svc-as-i18n-test-1_8449"
    private val key = "svc-i18n-key-1"

    @Test
    fun get_byId_entity() {
        val row = sysI18nService.get(seededIdZh)
        assertNotNull(row)
        assertEquals(seededIdZh, row.id)
    }

    /** `get(id, SysI18nCacheEntry::class)` 与 `getI18nFromCache` 在刷新 Hash 后一致。 */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysI18nHashCache.reloadAll(clear = true)
        val fromGet = sysI18nService.get(seededIdZh, SysI18nCacheEntry::class)
        val fromCache = sysI18nService.getI18nFromCache(seededIdZh)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromGet.id, fromCache.id)
        assertEquals(key, fromGet.key)
    }

    @Test
    fun getI18nValueFromCache() {
        sysI18nHashCache.reloadAll(clear = true)
        val value = sysI18nService.getI18nValueFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode, key)
        assertNotNull(value)
        assertEquals("svc-i18n-value-1", value)
    }

    @Test
    fun getI18nsFromCache() {
        sysI18nHashCache.reloadAll(clear = true)
        val i18ns = sysI18nService.getI18nsFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode)
        assertTrue(i18ns.isNotEmpty())
        assertTrue(i18ns.containsKey(key))
        assertEquals("svc-i18n-value-1", i18ns[key])
    }

    @Test
    fun batchGetI18nsFromCache() {
        sysI18nHashCache.reloadAll(clear = true)
        val batch = sysI18nService.batchGetI18nsFromCache(
            locale,
            mapOf(i18nTypeDictCode to listOf(namespace)),
            setOf(atomicServiceCode)
        )
        val byType = requireNotNull(batch[i18nTypeDictCode]) { "batch missing i18nTypeDictCode" }
        val byNs = requireNotNull(byType[namespace]) { "batch missing namespace" }
        assertEquals("svc-i18n-value-1", byNs[key])
    }

    @Test
    fun updateActive() {
        sysI18nHashCache.reloadAll(clear = true)
        assertTrue(sysI18nService.updateActive(seededIdZh, false))
        assertTrue(sysI18nService.updateActive(seededIdZh, true))
        sysI18nHashCache.reloadAll(clear = true)
        assertNotNull(sysI18nService.getI18nFromCache(seededIdZh))
    }

    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysI18nService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysI18nService.deleteById("00000000-0000-0000-0000-000000000001"))
    }
}
