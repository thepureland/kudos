package io.kudos.ability.distributed.notify.common.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


internal class NotifyMessageVoTest {

    @Test
    fun noArgConstructor_defaultsNotifyTypeToBlank() {
        val message = NotifyMessageVo<String>()

        assertTrue(message.notifyType.isBlank())
    }

    @Test
    fun bodyOnlyConstructor_defaultsNotifyTypeToBlank() {
        val message = NotifyMessageVo("payload")

        assertTrue(message.notifyType.isBlank())
        assertEquals("payload", message.messageBody)
    }

    @Test
    fun typeAndBodyConstructor_setsBothFields() {
        val message = NotifyMessageVo("cache.invalidate", "payload")

        assertEquals("cache.invalidate", message.notifyType)
        assertEquals("payload", message.messageBody)
    }

}
