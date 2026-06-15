package io.kudos.ms.msg.common.receiver.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MsgReceiverGroupFormCreate
 *
 * Covers IMsgReceiverGroupFormBase overrides, null handling, the data-class contract,
 * and the @MaxLength(128) constraint declared on the remark getter of the base interface.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgReceiverGroupFormCreateTest {

    @Test
    fun properties() {
        val form: IMsgReceiverGroupFormBase = MsgReceiverGroupFormCreate(
            receiverGroupTypeDictCode = "dept",
            defineTable = "tbl",
            nameColumn = "name",
            remark = "r",
            active = true,
        )
        assertEquals("dept", form.receiverGroupTypeDictCode)
        assertEquals("tbl", form.defineTable)
        assertEquals("name", form.nameColumn)
        assertEquals("r", form.remark)
        assertEquals(true, form.active)
    }

    @Test
    fun allNull() {
        val form = MsgReceiverGroupFormCreate(null, null, null, null, null)
        assertNull(form.receiverGroupTypeDictCode)
        assertNull(form.remark)
        assertNull(form.active)
    }

    @Test
    fun dataClassContract() {
        val a = MsgReceiverGroupFormCreate("dept", "tbl", "name", "r", true)
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(active = false))
        assertTrue(a.toString().contains("dept"))
    }

    @Test
    fun remarkHasMaxLength128() {
        val prop = IMsgReceiverGroupFormBase::class.memberProperties.first { it.name == "remark" }
        val maxLength = prop.getter.findAnnotation<MaxLength>()
        assertNotNull(maxLength, "remark getter should be annotated with @MaxLength")
        assertEquals(128, maxLength.max)
    }

}
