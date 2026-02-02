package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysResourceService
 *
 * 测试数据来源：`SysResourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysResourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysResourceService: ISysResourceService

    @Test
    fun getResourceById() {
        val id = "20000000-0000-0000-0000-000000000031"
        val cacheItem = sysResourceService.getResourceById(id)
        assertNotNull(cacheItem)
    }

    @Test
    fun getResourceBySubSystemAndUrl() {
        val subSystemCode = "svc-subsys-res-test-1"
        val url = "/svc-res-test-1"
        val resourceId = sysResourceService.getResourceBySubSystemAndUrl(subSystemCode, url)
        assertNotNull(resourceId)
    }

    @Test
    fun getResourcesBySubSystemCode() {
        val subSystemCode = "svc-subsys-res-test-1"
        val resources = sysResourceService.getResourcesBySubSystemCode(subSystemCode)
        assertTrue(resources.isNotEmpty())
    }

    @Test
    fun getChildResources() {
        val parentId = "20000000-0000-0000-0000-000000000031"
        val children = sysResourceService.getChildResources(parentId)
        assertTrue(children.any { it.id == "20000000-0000-0000-0000-000000000032" })
    }

    @Test
    fun getResourceTree() {
        val subSystemCode = "svc-subsys-res-test-1"
        val tree = sysResourceService.getResourceTree(subSystemCode, null)
        assertTrue(tree.isNotEmpty())
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000031"
        assertTrue(sysResourceService.updateActive(id, false))
        assertTrue(sysResourceService.updateActive(id, true))
    }
}
