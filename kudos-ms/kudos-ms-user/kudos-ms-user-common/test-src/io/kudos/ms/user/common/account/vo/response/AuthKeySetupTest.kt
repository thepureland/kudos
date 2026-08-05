package io.kudos.ms.user.common.account.vo.response

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for AuthKeySetup
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthKeySetupTest {

    private fun setup() = AuthKeySetup(
        secret = "JBSWY3DPEHPK3PXP",
        otpauthUrl = "otpauth://totp/Kudos:alice?secret=JBSWY3DPEHPK3PXP&issuer=Kudos",
    )

    @Test
    fun properties() {
        val s = setup()
        assertEquals("JBSWY3DPEHPK3PXP", s.secret)
        assertTrue(s.otpauthUrl.startsWith("otpauth://totp/"))
    }

    @Test
    fun dataClassContract() {
        val a = setup()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(secret = "OTHER"))
        assertTrue(a.toString().contains("JBSWY3DPEHPK3PXP"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = setup()
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
