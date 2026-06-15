package io.kudos.test.api.contract.provider

import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test for [UserController] (pure method invocation; no Spring container, no MockMvc, no DB).
 *
 * @author K
 * @since 1.0.0
 */
internal class UserControllerTest {

    private val controller = UserController()

    @Test
    fun getUser_returnsOkWithEchoedIdAndFixedName() {
        val response = controller.getUser(1L)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertTrue(response.statusCode.is2xxSuccessful)
        val body = response.body
        assertNotNull(body)
        assertEquals(1L, body.id)
        assertEquals("Tom", body.name)
    }

    @Test
    fun getUser_echoesAnyId() {
        assertEquals(42L, controller.getUser(42L).body!!.id)
        assertEquals(0L, controller.getUser(0L).body!!.id)
        assertEquals(Long.MAX_VALUE, controller.getUser(Long.MAX_VALUE).body!!.id)
        assertEquals(Long.MIN_VALUE, controller.getUser(Long.MIN_VALUE).body!!.id)
        assertEquals(-7L, controller.getUser(-7L).body!!.id)
    }

    @Test
    fun getUser_nameIsAlwaysTomRegardlessOfId() {
        listOf(1L, 2L, 999L, -1L).forEach { id ->
            assertEquals("Tom", controller.getUser(id).body!!.name)
        }
    }

    @Test
    fun getUser_returnsFreshBodyInstanceEachCall() {
        val first = controller.getUser(5L).body
        val second = controller.getUser(5L).body
        assertNotNull(first)
        assertNotNull(second)
        // equal by value (data class) but distinct invocations both succeed
        assertEquals(first, second)
    }
}
