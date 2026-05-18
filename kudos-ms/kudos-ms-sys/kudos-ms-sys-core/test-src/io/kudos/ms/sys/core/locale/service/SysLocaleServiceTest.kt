package io.kudos.ms.sys.core.locale.service

import io.kudos.ms.sys.core.locale.cache.LocaleByCodeCache
import io.kudos.ms.sys.core.locale.model.po.SysLocale
import io.kudos.ms.sys.core.locale.service.iservice.ISysLocaleService
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
 * junit test for SysLocaleService
 *
 * 测试数据来源：`SysLocaleServiceTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysLocaleServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysLocaleService: ISysLocaleService

    @Resource
    private lateinit var localeByCodeCache: LocaleByCodeCache

    private val seededIdActive = "30000000-0000-0000-0000-000000005001"
    private val seededIdInactive = "30000000-0000-0000-0000-000000005002"
    private val seededCodeActive = "ja_JP"
    private val seededCodeInactive = "es_ES"

    @Test
    fun get_byId_entity() {
        val po = sysLocaleService.get(seededIdActive)
        assertNotNull(po)
        assertEquals(seededIdActive, po.id)
        assertEquals(seededCodeActive, po.code)
    }

    /** 启用项可从按 code 缓存读到。 */
    @Test
    fun getLocaleByCode_active() {
        localeByCodeCache.reloadAll(clear = true)
        val entry = sysLocaleService.getLocaleByCode(seededCodeActive)
        assertNotNull(entry)
        assertEquals(seededIdActive, entry.id)
        assertEquals(seededCodeActive, entry.code)
    }

    /** 未启用项不应在按 code 缓存中命中。 */
    @Test
    fun getLocaleByCode_inactive_returnsNull() {
        localeByCodeCache.reloadAll(clear = true)
        assertNull(sysLocaleService.getLocaleByCode(seededCodeInactive))
    }

    /** listActiveLocales 按 sort_no 升序返回启用项。 */
    @Test
    fun listActiveLocales_orderBySortNo() {
        val list = sysLocaleService.listActiveLocales()
        // 至少包含本测试种子的启用项
        assertTrue(list.any { it.code == seededCodeActive })
        // 不应包含未启用
        assertTrue(list.none { it.code == seededCodeInactive })
        // 升序检查
        val sorted = list.sortedBy { it.sortNo }
        assertEquals(sorted.map { it.id }, list.map { it.id })
    }

    /** updateActive：停用后按 code 缓存不应命中；恢复后再次命中。 */
    @Test
    fun updateActive_syncsCache() {
        localeByCodeCache.reloadAll(clear = true)
        assertNotNull(sysLocaleService.getLocaleByCode(seededCodeActive))

        assertTrue(sysLocaleService.updateActive(seededIdActive, false))
        localeByCodeCache.reloadAll(clear = true)
        assertNull(sysLocaleService.getLocaleByCode(seededCodeActive))

        assertTrue(sysLocaleService.updateActive(seededIdActive, true))
        localeByCodeCache.reloadAll(clear = true)
        assertNotNull(sysLocaleService.getLocaleByCode(seededCodeActive))
    }

    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysLocaleService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysLocaleService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    @Test
    fun insert_and_deleteById_syncCache() {
        localeByCodeCache.reloadAll(clear = true)
        val unique = UUID.randomUUID().toString().take(6).lowercase()
        val code = "xx_$unique"

        val id = sysLocaleService.insert(
            SysLocale().apply {
                this.code = code
                displayName = "Test-$unique"
                englishName = "Test $unique"
                sortNo = 999
                active = true
                builtIn = false
            }
        )
        localeByCodeCache.reloadAll(clear = true)
        assertNotNull(sysLocaleService.getLocaleByCode(code))

        assertTrue(sysLocaleService.deleteById(id))
        localeByCodeCache.reloadAll(clear = true)
        assertNull(sysLocaleService.getLocaleByCode(code))
    }

    @Test
    fun batchDelete_syncCache() {
        localeByCodeCache.reloadAll(clear = true)
        val u1 = UUID.randomUUID().toString().take(6).lowercase()
        val u2 = UUID.randomUUID().toString().take(6).lowercase()
        val code1 = "yy_$u1"
        val code2 = "zz_$u2"
        val id1 = sysLocaleService.insert(SysLocale().apply {
            code = code1; displayName = "T-$u1"; englishName = "T $u1"; sortNo = 800; active = true; builtIn = false
        })
        val id2 = sysLocaleService.insert(SysLocale().apply {
            code = code2; displayName = "T-$u2"; englishName = "T $u2"; sortNo = 801; active = true; builtIn = false
        })
        localeByCodeCache.reloadAll(clear = true)
        assertNotNull(sysLocaleService.getLocaleByCode(code1))
        assertNotNull(sysLocaleService.getLocaleByCode(code2))

        assertEquals(2, sysLocaleService.batchDelete(listOf(id1, id2)))
        localeByCodeCache.reloadAll(clear = true)
        assertNull(sysLocaleService.getLocaleByCode(code1))
        assertNull(sysLocaleService.getLocaleByCode(code2))
    }
}
