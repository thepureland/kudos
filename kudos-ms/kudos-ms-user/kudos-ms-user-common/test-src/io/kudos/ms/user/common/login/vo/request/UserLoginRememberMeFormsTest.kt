package io.kudos.ms.user.common.login.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserLoginRememberMeFormCreate / UserLoginRememberMeFormUpdate
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLoginRememberMeFormsTest {

    private val t = LocalDateTime.of(2026, 6, 15, 7, 0)

    private fun create() = UserLoginRememberMeFormCreate(username = "alice", token = "tok", lastUsed = t)

    private fun update() = UserLoginRememberMeFormUpdate(id = "rm-1", username = "alice", token = "tok", lastUsed = t)

    @Test
    fun createProperties() {
        val c = create()
        assertEquals("alice", c.username)
        assertEquals("tok", c.token)
        assertEquals(t, c.lastUsed)
        val base: IUserLoginRememberMeFormBase = c
        assertEquals("alice", base.username)
    }

    @Test
    fun createDataClassContractAndNulls() {
        val a = create()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(token = "tok2"))
        assertTrue(a.toString().contains("alice"))
        assertNull(a.copy(lastUsed = null).lastUsed)
    }

    @Test
    fun updateProperties() {
        val u = update()
        assertEquals("rm-1", u.id)
        val asEntity: IIdEntity<String> = u
        assertEquals("rm-1", asEntity.id)
        assertEquals("alice", u.username)
    }

    @Test
    fun updateDataClassContract() {
        val a = update()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(id = "rm-2"))
        assertTrue(a.toString().contains("rm-1"))
    }

}
