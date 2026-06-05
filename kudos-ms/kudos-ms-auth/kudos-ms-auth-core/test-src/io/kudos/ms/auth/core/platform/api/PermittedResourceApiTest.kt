package io.kudos.ms.auth.core.platform.api

import io.kudos.ms.sys.common.resource.vo.SysResourceCacheEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pure-logic test for PermittedResourceApi.buildMenuTree — menu tree assembly, including the
 * "attach as root when the parent is not permitted" rule and orderNum sorting.
 *
 * No DB / Spring needed: buildMenuTree does not touch the injected authRoleService, so a bare
 * instance is enough.
 *
 * @author K
 * @since 1.0.0
 */
internal class PermittedResourceApiTest {

    private val api = PermittedResourceApi()

    private fun res(id: String, parentId: String?, orderNum: Int?): SysResourceCacheEntry =
        SysResourceCacheEntry(
            id = id, name = "name-$id", url = "/$id", resourceTypeDictCode = "MENU",
            parentId = parentId, orderNum = orderNum, icon = null, subSystemCode = null,
            remark = null, active = true, builtIn = false, createUserId = null,
            createUserName = null, createTime = null, updateUserId = null,
            updateUserName = null, updateTime = null,
        )

    @Test
    fun emptyInputProducesNoRoots() {
        assertTrue(api.buildMenuTree(emptyList()).isEmpty())
    }

    @Test
    fun threeLevelTreeIsNestedUnderItsRoot() {
        val tree = api.buildMenuTree(
            listOf(res("r", null, 1), res("c", "r", 1), res("g", "c", 1))
        )
        assertEquals(1, tree.size)
        val root = tree.single()
        assertEquals("r", root.id)
        assertEquals(listOf("c"), root.children.map { it._getId() })
        val child = root.children.single()
        assertEquals(listOf("g"), child._getChildren().map { it._getId() })
    }

    @Test
    fun childWhoseParentIsNotPermittedAttachesAsRoot() {
        // 'r' is NOT in the permitted set; 'c' (parent=r) must surface as a root, with 'g' under it
        val tree = api.buildMenuTree(
            listOf(res("c", "r", 1), res("g", "c", 2))
        )
        assertEquals(listOf("c"), tree.map { it.id })
        assertEquals(listOf("g"), tree.single().children.map { it._getId() })
    }

    @Test
    fun blankParentIdIsTreatedAsRoot() {
        val tree = api.buildMenuTree(listOf(res("x", "", null)))
        assertEquals(listOf("x"), tree.map { it.id })
    }

    @Test
    fun rootsAreOrderedByOrderNum() {
        val tree = api.buildMenuTree(
            listOf(res("a", null, 3), res("b", null, 1), res("z", null, 2))
        )
        assertEquals(listOf("b", "z", "a"), tree.map { it.id })
    }
}
