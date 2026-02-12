package io.kudos.base.error

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * CustomRuntimeException测试用例
 *
 * @author AI: ChatGPT
 * @since 1.0.0
 */
internal class CustomRuntimeExceptionTest {

    @Test
    fun testConstructorWithCauseHavingNullMessage() {
        val cause = NullPointerException()
        val exception = CustomRuntimeException(cause)

        assertNotNull(exception.message)
        assertEquals(NullPointerException::class.java.name, exception.message)
    }
}
