package io.kudos.ms.user.common.contact.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserContactWayFormCreate / UserContactWayFormUpdate
 *
 * Note: the form base uses Short priority (vs the cache entry's Int priority).
 *
 * @author K
 * @since 1.0.0
 */
internal class UserContactWayFormsTest {

    private fun create() = UserContactWayFormCreate(
        userId = "u-1",
        contactWayDictCode = "email",
        contactWayValue = "a@b.com",
        contactWayStatusDictCode = "1",
        priority = 10.toShort(),
        remark = "primary",
    )

    private fun update() = UserContactWayFormUpdate(
        id = "cw-1",
        userId = "u-1",
        contactWayDictCode = "email",
        contactWayValue = "a@b.com",
        contactWayStatusDictCode = "1",
        priority = 10.toShort(),
        remark = "primary",
    )

    @Test
    fun createProperties() {
        val c = create()
        assertEquals("email", c.contactWayDictCode)
        assertEquals(10.toShort(), c.priority)
        val base: IUserContactWayFormBase = c
        assertEquals("a@b.com", base.contactWayValue)
    }

    @Test
    fun createDataClassContractAndNulls() {
        val a = create()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(priority = 99.toShort()))
        assertTrue(a.toString().contains("email"))
        assertNull(a.copy(priority = null).priority)
    }

    @Test
    fun updateProperties() {
        val u = update()
        assertEquals("cw-1", u.id)
        val asEntity: IIdEntity<String> = u
        assertEquals("cw-1", asEntity.id)
    }

    @Test
    fun updateDataClassContract() {
        val a = update()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(id = "cw-2"))
        assertTrue(a.toString().contains("cw-1"))
    }

}
