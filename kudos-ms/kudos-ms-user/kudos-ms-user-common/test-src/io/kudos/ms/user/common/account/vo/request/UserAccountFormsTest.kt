package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for UserAccountFormCreate / UserAccountFormUpdate
 *
 * Covers IUserAccountFormBase overrides, the FormUpdate primary key (IIdEntity), data-class
 * contract and null handling of optional fields.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountFormsTest {

    private val t = LocalDateTime.of(2026, 6, 15, 12, 0)

    private fun create() = UserAccountFormCreate(
        username = "alice",
        tenantId = "t-1",
        loginPassword = "lp",
        securityPassword = "sp",
        accountTypeDictCode = "1",
        accountStatusDictCode = "0",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        lastLoginTime = t,
        lastLoginIp = 10L,
        lastLogoutTime = null,
        loginErrorTimes = 0,
        securityPasswordErrorTimes = 0,
        sessionKey = "sk",
        authenticationKey = "ak",
        orgId = "o-1",
        supervisorId = "s-1",
        remark = "r",
    )

    private fun update() = UserAccountFormUpdate(
        id = "u-1",
        username = "alice",
        tenantId = "t-1",
        loginPassword = "lp",
        securityPassword = "sp",
        accountTypeDictCode = "1",
        accountStatusDictCode = "0",
        defaultLocale = "zh_CN",
        defaultTimezone = "Asia/Shanghai",
        defaultCurrency = "CNY",
        lastLoginTime = t,
        lastLoginIp = 10L,
        lastLogoutTime = null,
        loginErrorTimes = 0,
        securityPasswordErrorTimes = 0,
        sessionKey = "sk",
        authenticationKey = "ak",
        orgId = "o-1",
        supervisorId = "s-1",
        remark = "r",
    )

    @Test
    fun createProperties() {
        val c = create()
        assertEquals("alice", c.username)
        assertEquals("t-1", c.tenantId)
        assertEquals("lp", c.loginPassword)
        assertEquals(t, c.lastLoginTime)
        assertEquals(10L, c.lastLoginIp)
        assertEquals("o-1", c.orgId)
        // IUserAccountFormBase reference
        val base: IUserAccountFormBase = c
        assertEquals("alice", base.username)
    }

    @Test
    fun createDataClassContractAndNulls() {
        val a = create()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(username = "bob"))
        assertTrue(a.toString().contains("alice"))
        val withNulls = a.copy(loginPassword = null, remark = null)
        assertNull(withNulls.loginPassword)
        assertNull(withNulls.remark)
    }

    @Test
    fun updateProperties() {
        val u = update()
        assertEquals("u-1", u.id)
        assertEquals("alice", u.username)
        val asEntity: IIdEntity<String> = u
        assertEquals("u-1", asEntity.id)
        val base: IUserAccountFormBase = u
        assertEquals("t-1", base.tenantId)
    }

    @Test
    fun updateDataClassContract() {
        val a = update()
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(id = "u-2"))
        assertTrue(a.toString().contains("u-1"))
    }

}
