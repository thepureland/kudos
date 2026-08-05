package io.kudos.ms.auth.api.public.controller.platform

import io.kudos.ms.auth.common.platform.api.IPermittedResource
import io.kudos.ms.auth.core.platform.api.PermittedResourceApi
import io.kudos.ms.sys.common.resource.vo.response.MenuTreeNode
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [PermittedResourceController].
 *
 * The controller is a pure HTTP entry-point that forwards [getMenusForCurrentUser] verbatim to the
 * injected [PermittedResourceApi]. There is no branching, transformation or DB access in it, so the
 * api is mocked with Mockito and no Spring container / database is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class PermittedResourceControllerTest {

    private val api = mock(PermittedResourceApi::class.java)
    private val controller = PermittedResourceController(api)

    @Test
    fun getMenusForCurrentUser_returnsExactList_andDelegatesOnce() {
        val n1 = MenuTreeNode().also { it.id = "m1"; it.title = "Root" }
        val n2 = MenuTreeNode().also { it.id = "m2"; it.title = "Child" }
        val expected = listOf(n1, n2)
        whenCalled(api.getMenusForCurrentUser()).thenReturn(expected)

        val actual = controller.getMenusForCurrentUser()

        // identity pass-through: the very same list instance is returned unchanged
        assertSame(expected, actual)
        assertEquals(2, actual.size)
        assertSame(n1, actual[0])
        assertSame(n2, actual[1])
        verify(api).getMenusForCurrentUser()
        verifyNoMoreInteractions(api)
    }

    @Test
    fun getMenusForCurrentUser_preservesOrder() {
        val a = MenuTreeNode().also { it.id = "a" }
        val b = MenuTreeNode().also { it.id = "b" }
        val c = MenuTreeNode().also { it.id = "c" }
        // deliberately non-sorted insertion order must be preserved verbatim
        val expected = listOf(c, a, b)
        whenCalled(api.getMenusForCurrentUser()).thenReturn(expected)

        val actual = controller.getMenusForCurrentUser()

        assertEquals(listOf("c", "a", "b"), actual.map { it.id })
        verify(api).getMenusForCurrentUser()
    }

    @Test
    fun getMenusForCurrentUser_emptyList_delegates() {
        val expected = emptyList<MenuTreeNode>()
        whenCalled(api.getMenusForCurrentUser()).thenReturn(expected)

        val actual = controller.getMenusForCurrentUser()

        assertSame(expected, actual)
        assertTrue(actual.isEmpty())
        verify(api).getMenusForCurrentUser()
        verifyNoMoreInteractions(api)
    }

    @Test
    fun getMenusForCurrentUser_singleCallPerInvocation() {
        whenCalled(api.getMenusForCurrentUser()).thenReturn(emptyList())

        controller.getMenusForCurrentUser()
        controller.getMenusForCurrentUser()

        // two controller calls must produce exactly two api calls (no caching / memoization)
        verify(api, org.mockito.Mockito.times(2)).getMenusForCurrentUser()
    }

    @Test
    fun controller_isInstanceOfIPermittedResource() {
        // the controller must honour the shared API contract so the inherited @GetMapping path applies
        assertTrue(
            IPermittedResource::class.java.isAssignableFrom(PermittedResourceController::class.java)
        )
    }

    @Test
    fun controller_carriesRestControllerAnnotation() {
        val annotation = PermittedResourceController::class.java.getAnnotation(RestController::class.java)
        assertNotNull(annotation, "PermittedResourceController must be annotated with @RestController")
    }
}
