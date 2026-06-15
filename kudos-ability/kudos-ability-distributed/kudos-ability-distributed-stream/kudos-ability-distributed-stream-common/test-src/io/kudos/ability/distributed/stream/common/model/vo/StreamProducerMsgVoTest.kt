package io.kudos.ability.distributed.stream.common.model.vo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for [StreamProducerMsgVo]: default null fields and mutability of all properties
 * (bindName / msgHeaderJson / msgBodyJson / msgBodyClassName).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class StreamProducerMsgVoTest {

    @Test
    fun defaults_areAllNull() {
        val vo = StreamProducerMsgVo()
        assertNull(vo.bindName)
        assertNull(vo.msgHeaderJson)
        assertNull(vo.msgBodyJson)
        assertNull(vo.msgBodyClassName)
    }

    @Test
    fun properties_areMutable() {
        val vo = StreamProducerMsgVo().apply {
            bindName = "orders-out-0"
            msgHeaderJson = """{"h":"1"}"""
            msgBodyJson = """{"b":"2"}"""
            msgBodyClassName = "java.lang.String"
        }

        assertEquals("orders-out-0", vo.bindName)
        assertEquals("""{"h":"1"}""", vo.msgHeaderJson)
        assertEquals("""{"b":"2"}""", vo.msgBodyJson)
        assertEquals("java.lang.String", vo.msgBodyClassName)
    }

}
