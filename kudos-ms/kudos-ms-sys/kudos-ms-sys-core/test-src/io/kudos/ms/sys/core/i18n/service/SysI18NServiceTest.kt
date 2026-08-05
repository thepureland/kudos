package io.kudos.ms.sys.core.i18n.service

import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.cache.SysI18nHashCache
import io.kudos.ms.sys.core.i18n.model.po.SysI18n
import io.kudos.ms.sys.core.i18n.service.iservice.ISysI18nService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysI18NService
 *
 * Test data source: `SysI18NServiceTest.sql`
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

    /** `get(id, SysI18nCacheEntry::class)` should be consistent with `getI18nFromCache` after refreshing the Hash. */
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

    private fun newI18n(): SysI18n {
        val unique = UUID.randomUUID().toString().take(8)
        return SysI18n().apply {
            this.locale = "zh_CN"
            this.atomicServiceCode = atomicServiceCode
            this.i18nTypeDictCode = "label"
            this.namespace = "ns-$unique"
            this.key = "key-$unique"
            this.value = "value-$unique"
            this.remark = "from SysI18NServiceTest insert"
            this.active = true
            this.builtIn = false
        }
    }

    /** insert persists the entry and yields a readable record. */
    @Test
    fun insert_persistsAndReturnsId() {
        val po = newI18n()
        val id = sysI18nService.insert(po)
        assertTrue(id.isNotBlank())
        val row = sysI18nService.get(id)
        assertNotNull(row)
        assertEquals(po.key, row.key)
    }

    /** update(any) modifies an existing entry. */
    @Test
    fun update_modifiesExistingRow() {
        val id = sysI18nService.insert(newI18n())
        val po = SysI18n().apply {
            this.id = id
            this.value = "value-updated"
        }
        assertTrue(sysI18nService.update(po))
        assertEquals("value-updated", sysI18nService.get(id)?.value)
    }

    /** deleteById succeeds for an existing entry. */
    @Test
    fun deleteById_removesExistingRow() {
        val id = sysI18nService.insert(newI18n())
        assertTrue(sysI18nService.deleteById(id))
        assertNull(sysI18nService.get(id))
    }

    /** batchDelete removes multiple entries and returns the actual count. */
    @Test
    fun batchDelete_removesMultipleRows() {
        val id1 = sysI18nService.insert(newI18n())
        val id2 = sysI18nService.insert(newI18n())
        assertEquals(2, sysI18nService.batchDelete(listOf(id1, id2)))
        assertNull(sysI18nService.get(id1))
        assertNull(sysI18nService.get(id2))
    }

    /** batchDelete returns 0 (no event) when nothing matches. */
    @Test
    fun batchDelete_returnsZeroWhenNothingMatches() {
        assertEquals(0, sysI18nService.batchDelete(listOf("00000000-0000-0000-0000-0000000000a1")))
    }

    private fun form(
        id: String,
        namespace: String,
        key: String,
        value: String,
    ) = SysI18nFormUpdate(
        id = id,
        locale = "zh_CN",
        atomicServiceCode = atomicServiceCode,
        i18nTypeDictCode = "label",
        namespace = namespace,
        key = key,
        value = value,
        remark = null,
    )

    /** batchSaveOrUpdate inserts entries with blank id and updates entries with an existing id. */
    @Test
    fun batchSaveOrUpdate_insertsAndUpdates() {
        val u = UUID.randomUUID().toString().take(8)
        // existing entry to update
        val existingId = sysI18nService.insert(newI18n())

        val count = sysI18nService.batchSaveOrUpdate(
            listOf(
                form(id = "", namespace = "bns-$u", key = "bkey-$u", value = "bval-$u"),       // insert (blank id)
                form(id = existingId, namespace = "uns-$u", key = "ukey-$u", value = "uval-$u"), // update (existing id)
            )
        )
        assertEquals(2, count)
        assertEquals("uval-$u", sysI18nService.get(existingId)?.value)
    }

    /** batchSaveOrUpdate update path: updating a non-existent id changes nothing, so it is not counted as success. */
    @Test
    fun batchSaveOrUpdate_updateMissingIdNotCounted() {
        val u = UUID.randomUUID().toString().take(8)
        val count = sysI18nService.batchSaveOrUpdate(
            listOf(
                form(
                    id = "00000000-0000-0000-0000-0000000000b2",
                    namespace = "mns-$u",
                    key = "mkey-$u",
                    value = "mval-$u",
                )
            )
        )
        assertEquals(0, count)
    }

    /** batchSaveOrUpdate falls back to i18nTypeDictCode as namespace when the form namespace is blank. */
    @Test
    fun batchSaveOrUpdate_blankNamespaceFallsBackToType() {
        val u = UUID.randomUUID().toString().take(8)
        val key = "fbkey-$u"
        val count = sysI18nService.batchSaveOrUpdate(
            listOf(form(id = "", namespace = "  ", key = key, value = "fbval-$u"))
        )
        assertEquals(1, count)
        sysI18nHashCache.reloadAll(clear = true)
        // namespace should have been resolved to the type code ("label")
        val value = sysI18nService.getI18nValueFromCache("zh_CN", "label", "label", atomicServiceCode, key)
        assertEquals("fbval-$u", value)
    }

    /** getI18nValueFromCache returns null for an unknown key. */
    @Test
    fun getI18nValueFromCache_nullForUnknownKey() {
        sysI18nHashCache.reloadAll(clear = true)
        assertNull(sysI18nService.getI18nValueFromCache(locale, i18nTypeDictCode, namespace, atomicServiceCode, "no-such-key"))
    }
}
