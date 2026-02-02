package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysDictService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
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

    @Test
    fun getDictFromCache() {
        val dictId = "20000000-0000-0000-0000-000000000028"
        val cacheItem = sysDictService.getDictFromCache(dictId)
        assertNotNull(cacheItem)
    }

    @Test
    fun getDictsByAtomicServiceCode() {
        val atomicServiceCode = "svc-module-dict-test-1"
        val dicts = sysDictService.getDictsByAtomicServiceCode(atomicServiceCode)
        assertTrue(dicts.any { it.id == "20000000-0000-0000-0000-000000000028" })
    }

    @Test
    fun getDictByAtomicServiceAndType() {
        val atomicServiceCode = "svc-module-dict-test-1"
        val dictType = "svc-dict-type-1"
        val dict = sysDictService.getDictByAtomicServiceAndType(atomicServiceCode, dictType)
        assertNotNull(dict)
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000028"
        assertTrue(sysDictService.updateActive(id, false))
        assertTrue(sysDictService.updateActive(id, true))
    }
}
