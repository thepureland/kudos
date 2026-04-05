package io.kudos.ms.sys.core.dict.service
import io.kudos.ms.sys.common.dict.vo.SysDictCacheEntry
import io.kudos.ms.sys.core.dict.cache.SysDictHashCache
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictService
 *
 * 测试数据来源：`SysDictServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictService: ISysDictService

    @Resource
    private lateinit var sysDictHashCache: SysDictHashCache

    private val seededId = "20000000-0000-0000-0000-000000007514"
    private val atomicServiceCode = "svc-module-dict-test-1"
    private val dictType = "svc-dict-type-1"

    /** `get(id, SysDictCacheEntry::class)` 与 `getDictFromCache` 在刷新 Hash 后一致。 */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysDictHashCache.reloadAll(clear = true)
        val fromGet = sysDictService.get(seededId, SysDictCacheEntry::class)
        val fromCache = sysDictService.getDictFromCache(seededId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
        assertEquals(seededId, fromGet.id)
    }

    /** 按主键从 Hash 读取缓存项。 */
    @Test
    fun getDictFromCache_byId() {
        sysDictHashCache.reloadAll(clear = true)
        val item = sysDictService.getDictFromCache(seededId)
        assertNotNull(item)
        assertEquals(dictType, item.dictType)
    }

    /** 按原子服务编码从缓存取字典列表。 */
    @Test
    fun getDictsFromCacheByAtomicServiceCode() {
        sysDictHashCache.reloadAll(clear = true)
        val dicts = sysDictService.getDictsFromCacheByAtomicServiceCode(atomicServiceCode)
        assertTrue(dicts.any { it.id == seededId })
    }

    /** 直查库的列表行与主键一致。 */
    @Test
    fun getDictByAtomicServiceAndType_and_getRecord() {
        val row = sysDictService.getDictByAtomicServiceAndType(atomicServiceCode, dictType)
        assertNotNull(row)
        assertEquals(seededId, row.id)

        val byId = sysDictService.getRecord(seededId)
        assertNotNull(byId)
        assertEquals(seededId, byId.id)
        assertEquals(dictType, byId.dictType)
    }

    /** 启用状态更新后缓存侧 `active` 与库一致。 */
    @Test
    fun updateActive() {
        sysDictHashCache.reloadAll(clear = true)
        assertTrue(sysDictService.updateActive(seededId, false))
        sysDictHashCache.reloadAll(clear = true)
        assertFalse(requireNotNull(sysDictService.getDictFromCache(seededId)).active)

        assertTrue(sysDictService.updateActive(seededId, true))
        sysDictHashCache.reloadAll(clear = true)
        assertTrue(requireNotNull(sysDictService.getDictFromCache(seededId)).active)
    }

    /** 主键不存在时 `updateActive` 返回 false。 */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysDictService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** 主键不存在时 `deleteById` 返回 false。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDictService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** 种子数据无字典项时，启用字典项查询为空列表（链路可调用）。 */
    @Test
    fun getActiveDictItemsFromCache_emptyWhenNoItems() {
        val items = sysDictService.getActiveDictItemsFromCache(dictType, atomicServiceCode)
        assertTrue(items.isEmpty())
        assertTrue(sysDictService.getActiveDictItemMapFromCache(dictType, atomicServiceCode).isEmpty())
    }
}
