package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountProtectionFormCreate / UserAccountProtectionFormUpdate
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountProtectionFormsTest {

    private fun create() = UserAccountProtectionFormCreate(
        userId = "u-1",
        question1 = "q1", answer1 = "a1",
        question2 = "q2", answer2 = "a2",
        question3 = null, answer3 = null,
        safeContactWayId = "c-1",
        totalValidateCount = 5,
        matchQuestionCount = 2,
        errorTimes = 0,
        remark = "r",
    )

    private fun update() = UserAccountProtectionFormUpdate(
        id = "p-1",
        userId = "u-1",
        question1 = "q1", answer1 = "a1",
        question2 = "q2", answer2 = "a2",
        question3 = null, answer3 = null,
        safeContactWayId = "c-1",
        totalValidateCount = 5,
        matchQuestionCount = 2,
        errorTimes = 0,
        remark = "r",
    )

    @Test
    fun createProperties() {
        val c = create()
        assertEquals("u-1", c.userId)
        assertEquals("a1", c.answer1)
        assertNull(c.question3)
        assertEquals(5, c.totalValidateCount)
        val base: IUserAccountProtectionFormBase = c
        assertEquals("q2", base.question2)
    }

    @Test
    fun createDataClassContract() {
        val a = create()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(errorTimes = 1))
        assertTrue(a.toString().contains("u-1"))
    }

    @Test
    fun updateProperties() {
        val u = update()
        assertEquals("p-1", u.id)
        val asEntity: IIdEntity<String> = u
        assertEquals("p-1", asEntity.id)
        assertEquals("u-1", u.userId)
    }

    @Test
    fun updateDataClassContract() {
        val a = update()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(id = "p-2"))
        assertTrue(a.toString().contains("p-1"))
    }

}
