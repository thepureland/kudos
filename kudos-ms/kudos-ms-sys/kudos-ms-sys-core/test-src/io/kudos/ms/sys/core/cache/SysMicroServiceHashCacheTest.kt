package io.kudos.ms.sys.core.cache

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.ms.sys.core.dao.SysMicroServiceDao
import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.*
import kotlin.test.*

/**
 * [SysMicroServiceHashCache] 单元测试（Hash 缓存，按 code 存取）。
 *
 * 覆盖：按 code 单条/批量获取、按 atomicService 查询、全量刷新、新增/更新/删除/批量删除后同步；
 * 本地缓存开启时二次取为同一对象引用。
 *
 * 测试数据：`SysMicroServiceHashCacheTest.sql`。
 * 需 Docker 运行 Redis，且 sys_cache 中已配置 SYS_MICRO_SERVICE__HASH（hash=true）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceHashCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: SysMicroServiceHashCache

    @Resource
    private lateinit var sysMicroServiceDao: SysMicroServiceDao

    private fun isLocalCacheEnabled(): Boolean = HashCacheKit.isLocalCacheEnabled(SysMicroServiceHashCache.CACHE_NAME)

    private val newMicroServiceName = "新微服务名称"

    @Test
    fun getMicroServiceByCode() {
        cacheHandler.reloadAll(true)
        val code = "code-1_8400"
        val item = cacheHandler.getMicroServiceByCode(code)
        assertNotNull(item)
        assertEquals(code, item.code)
        assertEquals("micro_service-1", item.name)
        val itemAgain = cacheHandler.getMicroServiceByCode(code)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
        assertNull(cacheHandler.getMicroServiceByCode("no_exist_code"))
    }

    @Test
    fun getMicroServicesByCodes() {
        cacheHandler.reloadAll(true)
        val code1 = "code-1_8400"
        val code2 = "code-2_8400"
        val result = cacheHandler.getMicroServicesByCodes(listOf(code1, code2))
        assertEquals(2, result.size)
        assertNotNull(result[code1])
        assertNotNull(result[code2])
        val resultAgain = cacheHandler.getMicroServicesByCodes(listOf(code1, code2))
        if (isLocalCacheEnabled()) {
            assertSame(result[code1], resultAgain[code1])
            assertSame(result[code2], resultAgain[code2])
        }
        val partial = cacheHandler.getMicroServicesByCodes(listOf("no_exist_code-1_8400", code2))
        assertEquals(1, partial.size)
        assertTrue(cacheHandler.getMicroServicesByCodes(emptyList()).isEmpty())
    }

    @Test
    fun getMicroServicesByType() {
        cacheHandler.reloadAll(true)
        val atomicList = cacheHandler.getMicroServicesByType(true)
        val nonAtomicList = cacheHandler.getMicroServicesByType(false)
        assertTrue(atomicList.size >= 3)
        assertTrue(nonAtomicList.size >= 6)
        assertTrue(atomicList.all { it.atomicService == true })
        assertTrue(nonAtomicList.all { it.atomicService == false })
        val atomicAgain = cacheHandler.getMicroServicesByType(true)
        assertEquals(atomicList.size, atomicAgain.size)
        assertEquals(atomicList.map { it.id }.toSet(), atomicAgain.map { it.id }.toSet())
        if (isLocalCacheEnabled() && atomicList.isNotEmpty()) {
            val firstCode = assertNotNull(atomicList.first().code)
            assertSame(cacheHandler.getMicroServiceByCode(firstCode), atomicAgain.find { it.code == firstCode })
        }
    }

    @Test
    fun listAtomicServices() {
        cacheHandler.reloadAll(true)
        val list = cacheHandler.listAtomicServices()
        assertTrue(list.isNotEmpty())
        assertTrue(list.all { it.atomicService == true })
    }

    @Test
    fun syncOnInsert() {
        cacheHandler.reloadAll(true)
        val code = insertNewRecordToDb()
        cacheHandler.syncOnInsert(code)
        val item = cacheHandler.getMicroServiceByCode(code)
        assertNotNull(item)
        val itemAgain = cacheHandler.getMicroServiceByCode(code)
        if (isLocalCacheEnabled()) assertSame(item, itemAgain)
    }

    @Test
    fun syncOnUpdate() {
        cacheHandler.reloadAll(true)
        val code = "code-2_8400"
        val success = sysMicroServiceDao.updateProperties(code, mapOf(SysMicroService::name.name to newMicroServiceName))
        assertTrue(success)
        cacheHandler.syncOnUpdate(code)
        assertEquals(newMicroServiceName, cacheHandler.getMicroServiceByCode(code)?.name)
    }

    @Test
    fun syncOnDelete() {
        cacheHandler.reloadAll(true)
        val code = insertNewRecordToDb()
        val count = sysMicroServiceDao.batchDelete(listOf(code))
        assertEquals(1, count)
        cacheHandler.syncOnDelete(code)
        assertNull(cacheHandler.getMicroServiceByCode(code))
    }

    @Test
    fun syncOnBatchDelete() {
        cacheHandler.reloadAll(true)
        val code1 = insertNewRecordToDb()
        val code2 = insertNewRecordToDb()
        val codes = listOf(code1, code2)
        val count = sysMicroServiceDao.batchDelete(codes)
        assertEquals(2, count)
        cacheHandler.syncOnBatchDelete(codes)
        assertNull(cacheHandler.getMicroServiceByCode(code1))
        assertNull(cacheHandler.getMicroServiceByCode(code2))
    }

    private fun insertNewRecordToDb(): String {
        val unique = UUID.randomUUID().toString().replace("-", "").take(12)
        val sysMicroService = SysMicroService().apply {
            code = "tc_${unique}"
            name = "测试微服务_${unique}"
            context = "/test"
            active = true
        }
        return sysMicroServiceDao.insert(sysMicroService)
    }
}
