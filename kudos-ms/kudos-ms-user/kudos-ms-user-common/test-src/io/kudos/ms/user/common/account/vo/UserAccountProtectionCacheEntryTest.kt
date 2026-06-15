package io.kudos.ms.user.common.account.vo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountProtectionCacheEntry
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountProtectionCacheEntryTest {

    private fun entry() = UserAccountProtectionCacheEntry(
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
        active = true,
        builtIn = false,
        createUserId = null, createUserName = null, createTime = null,
        updateUserId = null, updateUserName = null, updateTime = null,
    )

    @Test
    fun properties() {
        val e = entry()
        assertEquals("p-1", e.id)
        assertEquals("u-1", e.userId)
        assertEquals("a1", e.answer1)
        assertNull(e.question3)
        assertEquals(5, e.totalValidateCount)
        assertEquals(2, e.matchQuestionCount)
    }

    @Test
    fun dataClassContract() {
        val a = entry()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(errorTimes = 3))
        assertTrue(a.toString().contains("p-1"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = entry()
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
