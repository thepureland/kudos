package io.kudos.ms.user.api.admin.controller.account

import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [UserAccountThirdAdminController] (pure delegation; service mocked, no Spring container).
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountThirdAdminControllerTest {

    private val service = mock(IUserAccountThirdService::class.java)
    private val controller = UserAccountThirdAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun listByUserId_delegatesAndPreservesOrderAndIdentity() {
        val b1 = mock(UserAccountThird::class.java)
        val b2 = mock(UserAccountThird::class.java)
        val bindings = listOf(b1, b2)
        whenCalled(service.getByUserAccountId("u1")).thenReturn(bindings)

        val result = controller.listByUserId("u1")

        assertSame(bindings, result)
        assertEquals(listOf(b1, b2), result)
        verify(service).getByUserAccountId("u1")
    }

    @Test
    fun listByUserId_emptyResult() {
        whenCalled(service.getByUserAccountId("none")).thenReturn(emptyList())
        assertTrue(controller.listByUserId("none").isEmpty())
        verify(service).getByUserAccountId("none")
    }

}
