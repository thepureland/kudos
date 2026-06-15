package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountThirdFormCreate / UserAccountThirdFormUpdate
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountThirdFormsTest {

    private val t = LocalDateTime.of(2026, 6, 15, 8, 0)

    private fun create() = UserAccountThirdFormCreate(
        userId = "u-1",
        accountProviderDictCode = "wechat",
        accountProviderIssuer = "issuer",
        subject = "open-id",
        unionId = "union",
        externalDisplayName = "Alice",
        externalEmail = "a@b.com",
        avatarUrl = "http://x/y.png",
        lastLoginTime = t,
        tenantId = "t-1",
        remark = "r",
    )

    private fun update() = UserAccountThirdFormUpdate(
        id = "th-1",
        userId = "u-1",
        accountProviderDictCode = "wechat",
        accountProviderIssuer = "issuer",
        subject = "open-id",
        unionId = "union",
        externalDisplayName = "Alice",
        externalEmail = "a@b.com",
        avatarUrl = "http://x/y.png",
        lastLoginTime = t,
        tenantId = "t-1",
        remark = "r",
    )

    @Test
    fun createProperties() {
        val c = create()
        assertEquals("wechat", c.accountProviderDictCode)
        assertEquals("open-id", c.subject)
        assertEquals(t, c.lastLoginTime)
        val base: IUserAccountThirdFormBase = c
        assertEquals("union", base.unionId)
    }

    @Test
    fun createDataClassContractAndNulls() {
        val a = create()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(subject = "other"))
        assertTrue(a.toString().contains("wechat"))
        assertNull(a.copy(unionId = null).unionId)
    }

    @Test
    fun updateProperties() {
        val u = update()
        assertEquals("th-1", u.id)
        val asEntity: IIdEntity<String> = u
        assertEquals("th-1", asEntity.id)
        assertEquals("open-id", u.subject)
    }

    @Test
    fun updateDataClassContract() {
        val a = update()
        assertEquals(a, a.copy())
        assertNotEquals(a, a.copy(id = "th-2"))
        assertTrue(a.toString().contains("th-1"))
    }

}
