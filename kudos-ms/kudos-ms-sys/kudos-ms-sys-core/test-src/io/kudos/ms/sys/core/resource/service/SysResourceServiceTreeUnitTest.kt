package io.kudos.ms.sys.core.resource.service

import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import io.kudos.ms.sys.common.resource.vo.response.BaseMenuTreeNode
import io.kudos.ms.sys.common.resource.vo.response.SysResourceRow
import io.kudos.ms.sys.core.dict.cache.SysDictItemHashCache
import io.kudos.ms.sys.core.resource.cache.SysResourceHashCache
import io.kudos.ms.sys.core.resource.dao.SysResourceDao
import io.kudos.ms.sys.core.resource.service.impl.SysResourceService
import io.kudos.ms.sys.core.system.cache.SysSystemHashCache
import org.mockito.Mockito.mock
import org.springframework.context.ApplicationEventPublisher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure unit test for the in-memory tree-building / sorting helpers of [SysResourceService]
 * ([SysResourceService.buildResourceTree], [SysResourceService.toTreeRow],
 * [SysResourceService.sortResourceTree], [SysResourceService.buildMenuTree],
 * [SysResourceService.filterChildrenRecursively]).
 *
 * All collaborators (DAO / caches / event publisher) are Mockito mocks and are never touched by these
 * pure helpers, so no Spring container and no database are required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysResourceServiceTreeUnitTest {

    private val service = SysResourceService(
        dao = mock(SysResourceDao::class.java),
        sysResourceHashCache = mock(SysResourceHashCache::class.java),
        sysDictItemHashCache = mock(SysDictItemHashCache::class.java),
        sysSystemHashCache = mock(SysSystemHashCache::class.java),
        eventPublisher = mock(ApplicationEventPublisher::class.java),
    )

    private fun row(
        id: String,
        parentId: String? = null,
        orderNum: Int? = null,
        name: String = "name-$id",
    ) = SysResourceRow(
        id = id,
        name = name,
        parentId = parentId,
        orderNum = orderNum,
        subSystemCode = "sub-1",
        resourceTypeDictCode = "menu",
    )

    private fun cacheEntry(
        id: String,
        parentId: String? = null,
        orderNum: Int? = null,
    ) = SysResourceCacheEntry(
        id = id,
        name = "name-$id",
        url = null,
        resourceTypeDictCode = "menu",
        parentId = parentId,
        orderNum = orderNum,
        icon = null,
        subSystemCode = "sub-1",
        remark = null,
        active = true,
        builtIn = false,
        createUserId = null,
        createUserName = null,
        createTime = null,
        updateUserId = null,
        updateUserName = null,
        updateTime = null,
    )

    // region toTreeRow

    @Test
    fun toTreeRow_copiesAllFieldsAndStartsWithEmptyChildren() {
        val src = row(id = "a", parentId = "p", orderNum = 7).copy(
            url = "/a",
            icon = "ic",
            remark = "rk",
            active = false,
            builtIn = true,
        )
        val node = service.toTreeRow(src)
        assertEquals("a", node.id)
        assertEquals("name-a", node.name)
        assertEquals("/a", node.url)
        assertEquals("menu", node.resourceTypeDictCode)
        assertEquals("p", node.parentId)
        assertEquals(7, node.orderNum)
        assertEquals("ic", node.icon)
        assertEquals("sub-1", node.subSystemCode)
        assertEquals("rk", node.remark)
        assertEquals(false, node.active)
        assertEquals(true, node.builtIn)
        assertTrue(node.children!!.isEmpty())
    }

    // endregion

    // region buildResourceTree

    @Test
    fun buildResourceTree_emptyInput_returnsEmpty() {
        assertTrue(service.buildResourceTree(emptyList(), null).isEmpty())
    }

    @Test
    fun buildResourceTree_nestsChildrenUnderParents() {
        val records = listOf(
            row(id = "root", parentId = null, orderNum = 1),
            row(id = "c1", parentId = "root", orderNum = 1),
            row(id = "c2", parentId = "root", orderNum = 2),
            row(id = "gc", parentId = "c1", orderNum = 1),
        )
        val roots = service.buildResourceTree(records, null)
        assertEquals(1, roots.size)
        val root = roots.single()
        assertEquals("root", root.id)
        assertEquals(listOf("c1", "c2"), root.children!!.map { it.id })
        val c1 = root.children!!.first { it.id == "c1" }
        assertEquals(listOf("gc"), c1.children!!.map { it.id })
    }

    @Test
    fun buildResourceTree_orphanWithMissingParent_becomesRoot() {
        // "orphan" references a parent that is not present in the records -> treated as a root.
        val records = listOf(
            row(id = "root", parentId = null, orderNum = 1),
            row(id = "orphan", parentId = "ghost", orderNum = 2),
        )
        val roots = service.buildResourceTree(records, null)
        assertEquals(setOf("root", "orphan"), roots.map { it.id }.toSet())
    }

    @Test
    fun buildResourceTree_sortsSiblingsByOrderNumNullsLast() {
        val records = listOf(
            row(id = "root", parentId = null, orderNum = 0),
            row(id = "b", parentId = "root", orderNum = 2),
            row(id = "a", parentId = "root", orderNum = 1),
            row(id = "z", parentId = "root", orderNum = null), // null sorts last
        )
        val root = service.buildResourceTree(records, null).single()
        assertEquals(listOf("a", "b", "z"), root.children!!.map { it.id })
    }

    @Test
    fun buildResourceTree_withParentIdSelector_returnsOnlyThatSubtree() {
        val records = listOf(
            row(id = "root", parentId = null, orderNum = 1),
            row(id = "c1", parentId = "root", orderNum = 1),
            row(id = "c2", parentId = "root", orderNum = 2),
        )
        val children = service.buildResourceTree(records, "root")
        assertEquals(listOf("c1", "c2"), children.map { it.id })
    }

    @Test
    fun buildResourceTree_withUnknownParentIdSelector_returnsEmpty() {
        val records = listOf(row(id = "root", parentId = null, orderNum = 1))
        assertTrue(service.buildResourceTree(records, "does-not-exist").isEmpty())
    }

    // endregion

    // region sortResourceTree

    @Test
    fun sortResourceTree_sortsRecursivelyInPlace() {
        val records = listOf(
            row(id = "root", parentId = null, orderNum = 1),
            row(id = "x", parentId = "root", orderNum = 3),
            row(id = "y", parentId = "root", orderNum = 1),
            row(id = "yc2", parentId = "y", orderNum = 5),
            row(id = "yc1", parentId = "y", orderNum = 2),
        )
        // buildResourceTree already invokes sortResourceTree; verify nested ordering.
        val root = service.buildResourceTree(records, null).single()
        assertEquals(listOf("y", "x"), root.children!!.map { it.id })
        val y = root.children!!.first { it.id == "y" }
        assertEquals(listOf("yc1", "yc2"), y.children!!.map { it.id })
    }

    // endregion

    // region buildMenuTree

    @Test
    fun buildMenuTree_nestsAndOrdersByOrderNum() {
        val resources = listOf(
            cacheEntry(id = "root", parentId = null, orderNum = 1),
            cacheEntry(id = "c2", parentId = "root", orderNum = 2),
            cacheEntry(id = "c1", parentId = "root", orderNum = 1),
        )
        val roots = service.buildMenuTree(resources) { item ->
            BaseMenuTreeNode().apply {
                id = item.id
                title = item.name
                parentId = item.parentId
                seqNo = item.orderNum
            }
        }
        assertEquals(1, roots.size)
        val root = roots.single()
        assertEquals("root", root.id)
        // children attached in orderNum order (c1 before c2)
        assertEquals(listOf("c1", "c2"), root.children.map { (it as BaseMenuTreeNode).id })
    }

    @Test
    fun buildMenuTree_blankParentIsRoot_andMissingParentIsRoot() {
        val resources = listOf(
            cacheEntry(id = "blankParent", parentId = "", orderNum = 1),
            cacheEntry(id = "orphan", parentId = "ghost", orderNum = 2),
        )
        val roots = service.buildMenuTree(resources) { item ->
            BaseMenuTreeNode().apply {
                id = item.id
                parentId = item.parentId
                seqNo = item.orderNum
            }
        }
        assertEquals(setOf("blankParent", "orphan"), roots.map { it.id }.toSet())
    }

    @Test
    fun buildMenuTree_emptyInput_returnsEmpty() {
        val roots = service.buildMenuTree<BaseMenuTreeNode>(emptyList()) { BaseMenuTreeNode() }
        assertTrue(roots.isEmpty())
    }

    // endregion

    // region filterChildrenRecursively

    @Test
    fun filterChildrenRecursively_collectsAllDescendants() {
        val resources = listOf(
            cacheEntry(id = "root", parentId = null),
            cacheEntry(id = "a", parentId = "root"),
            cacheEntry(id = "b", parentId = "root"),
            cacheEntry(id = "a1", parentId = "a"),
            cacheEntry(id = "a1x", parentId = "a1"),
            cacheEntry(id = "unrelated", parentId = "other"),
        )
        val out = mutableListOf<SysResourceCacheEntry>()
        service.filterChildrenRecursively("root", out, resources)
        assertEquals(setOf("a", "b", "a1", "a1x"), out.map { it.id }.toSet())
        assertTrue(out.none { it.id == "unrelated" })
        assertTrue(out.none { it.id == "root" })
    }

    @Test
    fun filterChildrenRecursively_noChildren_leavesAccumulatorEmpty() {
        val resources = listOf(cacheEntry(id = "leaf", parentId = "root"))
        val out = mutableListOf<SysResourceCacheEntry>()
        service.filterChildrenRecursively("leaf", out, resources)
        assertTrue(out.isEmpty())
    }

    // endregion
}
