package io.kudos.ms.user.common.passport.vo.response

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserInfoModel
 *
 * @author K
 * @since 1.0.0
 */
internal class UserInfoModelTest {

    private fun model() = UserInfoModel(
        id = "u-1",
        username = "alice",
        tenantId = "t-1",
        orgId = "o-1",
        accountTypeDictCode = "1",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        loginTime = LocalDateTime.of(2026, 6, 15, 10, 0),
    )

    @Test
    fun properties() {
        val m = model()
        assertEquals("u-1", m.id)
        assertEquals("alice", m.username)
        assertEquals("o-1", m.orgId)
        assertEquals(LocalDateTime.of(2026, 6, 15, 10, 0), m.loginTime)
    }

    @Test
    fun nullableFields() {
        val m = model().copy(orgId = null, accountTypeDictCode = null, defaultLocale = null)
        assertNull(m.orgId)
        assertNull(m.accountTypeDictCode)
        assertNull(m.defaultLocale)
    }

    @Test
    fun dataClassContract() {
        val a = model()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(username = "bob"))
        assertTrue(a.toString().contains("alice"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = model()
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
