package io.kudos.ms.user.common.passport.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for SessionUserPrincipal
 *
 * Covers property accessors, the IIdEntity contract (id is the primary key), data-class contract
 * and Java serialization round-trip (it lives in HttpSession).
 *
 * @author K
 * @since 1.0.0
 */
internal class SessionUserPrincipalTest {

    private fun principal() = SessionUserPrincipal(id = "u-1", tenantId = "t-1", username = "alice")

    @Test
    fun propertiesAndIsIdEntity() {
        val p = principal()
        assertEquals("u-1", p.id)
        assertEquals("t-1", p.tenantId)
        assertEquals("alice", p.username)
        val asEntity: IIdEntity<String> = p
        assertEquals("u-1", asEntity.id)
    }

    @Test
    fun dataClassContract() {
        val a = principal()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(id = "u-2"))
        assertNotEquals(a, a.copy(username = "bob"))
        assertTrue(a.toString().contains("alice"))
    }

    @Test
    fun serializationRoundTrip() {
        val original = principal()
        val bytes = ByteArrayOutputStream().use { bos ->
            ObjectOutputStream(bos).use { it.writeObject(original) }
            bos.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(original, restored)
    }

}
