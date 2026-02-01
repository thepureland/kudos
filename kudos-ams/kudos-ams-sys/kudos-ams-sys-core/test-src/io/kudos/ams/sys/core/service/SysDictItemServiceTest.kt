package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysDictItemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDictItemService
 *
 * 测试数据来源：`SysDictItemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDictItemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDictItemService: ISysDictItemService

    @Test
    fun getDictItemTree() {
        val dictId = "20000000-0000-0000-0000-000000000029"
        val tree = sysDictItemService.getDictItemTree(dictId, null)
        assertTrue(tree.isNotEmpty())
        val rootNode = tree.firstOrNull { it.itemCode == "svc-item-code-1" }
        assertNotNull(rootNode)
        
        // 测试树形结构：子节点应该在父节点的children中
        assertTrue(rootNode.children?.any { it.itemCode == "svc-item-code-2" } == true)
    }

    @Test
    fun getDictItemsByDictId() {
        val dictId = "20000000-0000-0000-0000-000000000029"
        val items = sysDictItemService.getDictItemsByDictId(dictId)
        assertTrue(items.isNotEmpty())
    }

    @Test
    fun getChildItems() {
        val parentId = "20000000-0000-0000-0000-000000000029"
        val children = sysDictItemService.getChildItems(parentId)
        assertTrue(children.any { it.itemCode == "svc-item-code-2" })
    }
}
