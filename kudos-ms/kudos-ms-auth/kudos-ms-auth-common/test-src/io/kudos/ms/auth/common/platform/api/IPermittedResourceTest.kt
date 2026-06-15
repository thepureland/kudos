package io.kudos.ms.auth.common.platform.api

import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * test for IPermittedResource
 *
 * Covers the interface contract through a fake implementation: empty result (not logged in)
 * and a populated menu tree.
 *
 * @author K
 * @since 1.0.0
 */
internal class IPermittedResourceTest {

    private class FakeApi(private val loggedIn: Boolean) : IPermittedResource {
        override fun getMenusForCurrentUser(): List<MenuTreeNode> {
            if (!loggedIn) return emptyList()
            val node = MenuTreeNode().apply {
                id = "m-1"
                title = "Dashboard"
                index = "/dashboard"
                icon = "home"
            }
            return listOf(node)
        }
    }

    @Test
    fun notLoggedInReturnsEmpty() {
        assertTrue(FakeApi(loggedIn = false).getMenusForCurrentUser().isEmpty())
    }

    @Test
    fun loggedInReturnsMenus() {
        val menus = FakeApi(loggedIn = true).getMenusForCurrentUser()
        assertEquals(1, menus.size)
        val node = menus.single()
        assertEquals("m-1", node.id)
        assertEquals("Dashboard", node.title)
        assertEquals("/dashboard", node.index)
        assertEquals("home", node.icon)
    }
}
