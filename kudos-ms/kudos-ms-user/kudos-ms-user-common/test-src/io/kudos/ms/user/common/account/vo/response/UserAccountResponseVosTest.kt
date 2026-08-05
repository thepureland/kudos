package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for the account response VOs: UserAccountDetail / UserAccountEdit / UserAccountRow,
 * the protection / third / org-user response VOs.
 *
 * Each VO: default ctor (id == "", everything else null), a populated copy and the data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountResponseVosTest {

    @Test
    fun userAccountDetailDefaultsAndContract() {
        val d = UserAccountDetail()
        assertEquals("", d.id)
        assertNull(d.username)
        assertNull(d.loginPassword)
        val asEntity: IIdEntity<String> = d
        assertEquals("", asEntity.id)

        val full = d.copy(id = "u-1", username = "alice", loginPassword = "lp", orgId = "o-1", active = true)
        assertEquals("u-1", full.id)
        assertEquals("alice", full.username)
        assertEquals(full, full.copy())
        assertEquals(full.hashCode(), full.copy().hashCode())
        assertNotEquals(full, full.copy(username = "bob"))
        assertTrue(full.toString().contains("alice"))
    }

    @Test
    fun userAccountEditDefaultsAndContract() {
        val e = UserAccountEdit()
        assertEquals("", e.id)
        assertNull(e.sessionKey)
        val full = e.copy(id = "u-1", username = "alice", authenticationKey = "ak", remark = "r")
        assertEquals("ak", full.authenticationKey)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(id = "u-2"))
        assertTrue(full.toString().contains("alice"))
    }

    @Test
    fun userAccountRowDefaultsAndContract() {
        val r = UserAccountRow()
        assertEquals("", r.id)
        assertNull(r.accountTypeDictCode)
        val full = r.copy(id = "u-1", username = "alice", sessionKey = "sk", active = false)
        assertEquals("sk", full.sessionKey)
        assertEquals(false, full.active)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(active = true))
        assertTrue(full.toString().contains("alice"))
    }

    @Test
    fun protectionDetailEditRow() {
        val d = UserAccountProtectionDetail()
        assertEquals("", d.id)
        assertNull(d.userId)
        val dFull = d.copy(id = "p-1", userId = "u-1", totalValidateCount = 5)
        assertEquals(5, dFull.totalValidateCount)
        assertNotEquals(dFull, dFull.copy(userId = "u-2"))
        assertTrue(dFull.toString().contains("p-1"))

        val e = UserAccountProtectionEdit(id = "p-1", userId = "u-1", matchQuestionCount = 2)
        assertEquals(2, e.matchQuestionCount)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(id = "p-2"))

        val row = UserAccountProtectionRow(id = "p-1", userId = "u-1", active = true)
        assertEquals(true, row.active)
        assertEquals(row, row.copy())
        assertNotEquals(row, row.copy(active = false))
    }

    @Test
    fun thirdDetailEdit() {
        val d = UserAccountThirdDetail()
        assertEquals("", d.id)
        val dFull = d.copy(id = "th-1", subject = "open-id", accountProviderDictCode = "wechat")
        assertEquals("open-id", dFull.subject)
        assertNotEquals(dFull, dFull.copy(subject = "x"))
        assertTrue(dFull.toString().contains("wechat"))

        val e = UserAccountThirdEdit(id = "th-1", subject = "open-id", tenantId = "t-1")
        assertEquals("t-1", e.tenantId)
        assertEquals(e, e.copy())
        assertNotEquals(e, e.copy(id = "th-2"))
    }

    @Test
    fun thirdRow() {
        val r = UserAccountThirdRow()
        assertEquals("", r.id)
        val full = r.copy(id = "th-1", unionId = "union", active = true)
        assertEquals("union", full.unionId)
        assertEquals(full, full.copy())
        assertNotEquals(full, full.copy(unionId = "u2"))
        assertTrue(full.toString().contains("th-1"))
    }

    @Test
    fun orgUserDetail() {
        val d = UserOrgUserDetail()
        assertEquals("", d.id)
        assertNull(d.orgId)
        assertNull(d.orgAdmin)
        val full = d.copy(id = "ou-1", orgId = "o-1", userId = "u-1", orgAdmin = true)
        assertEquals(true, full.orgAdmin)
        assertEquals("o-1", full.orgId)
        assertEquals(full, full.copy())
        assertEquals(full.hashCode(), full.copy().hashCode())
        assertNotEquals(full, full.copy(orgAdmin = false))
        assertTrue(full.toString().contains("ou-1"))
    }

}
