package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysDictService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictService
 *
 * 测试数据来源：`V1.0.0.28__SysDictServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictServiceTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var sysDictService: ISysDictService

    @Test
    fun getDictFromCache() {
        val dictId = "20000000-0000-0000-0000-000000000028"
        val cacheItem = sysDictService.getDictFromCache(dictId)
        assertNotNull(cacheItem)
    }

    @Test
    fun getDictsByModuleCode() {
        val moduleCode = "svc-module-dict-test-1"
        val dicts = sysDictService.getDictsByModuleCode(moduleCode)
        assertTrue(dicts.any { it.id == "20000000-0000-0000-0000-000000000028" })
    }

    @Test
    fun getDictByModuleAndType() {
        val moduleCode = "svc-module-dict-test-1"
        val dictType = "svc-dict-type-1"
        val dict = sysDictService.getDictByModuleAndType(moduleCode, dictType)
        assertNotNull(dict)
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000028"
        assertTrue(sysDictService.updateActive(id, false))
        assertTrue(sysDictService.updateActive(id, true))
    }
}
