package io.kudos.ms.user.core.login.service

import io.kudos.ms.user.core.login.dao.UserLogLoginDao
import io.kudos.ms.user.core.login.model.po.UserLogLogin
import io.kudos.ms.user.core.login.service.impl.UserLogLoginService
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for [UserLogLoginService] — verifies the in-memory sort (descending by loginTime)
 * and `take(limit)` capping applied on top of the DAO results, plus the toLong() count conversions
 * and the success/failure flag pass-through. The DAO is a Mockito mock, so no DB is required.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserLogLoginServicePureTest {

    private val dao = mock(UserLogLoginDao::class.java)
    private val service = UserLogLoginService(dao)

    // Interface-typed view so calls that omit the optional limit go through the interface's
    // default-argument synthetic (the impl redeclares the methods without defaults).
    private val iface: io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService = service

    private val base = LocalDateTime.of(2024, 1, 10, 12, 0, 0)

    private fun log(userId: String, tenantId: String, minutesOffset: Long): UserLogLogin {
        val po = mock(UserLogLogin::class.java)
        whenCalled(po.userId).thenReturn(userId)
        whenCalled(po.tenantId).thenReturn(tenantId)
        whenCalled(po.loginTime).thenReturn(base.plusMinutes(minutesOffset))
        return po
    }

    @Test
    fun getLoginsByUserId_sortsDescendingAndCapsAtLimit() {
        // Provided out of order: offsets 0, 30, 10 -> expected descending: 30, 10, 0
        val l0 = log("u1", "t1", 0)
        val l30 = log("u1", "t1", 30)
        val l10 = log("u1", "t1", 10)
        whenCalled(dao.searchByUserId("u1")).thenReturn(listOf(l0, l30, l10))

        val result = service.getLoginsByUserId("u1", 100)
        assertEquals(listOf(l30, l10, l0), result)
    }

    @Test
    fun getLoginsByUserId_limitTruncates() {
        val l0 = log("u1", "t1", 0)
        val l30 = log("u1", "t1", 30)
        val l10 = log("u1", "t1", 10)
        whenCalled(dao.searchByUserId("u1")).thenReturn(listOf(l0, l30, l10))

        val result = service.getLoginsByUserId("u1", 2)
        assertEquals(listOf(l30, l10), result)
    }

    @Test
    fun getLoginsByTenantId_sortsDescendingAndCaps() {
        val a = log("u1", "t1", 5)
        val b = log("u2", "t1", 50)
        whenCalled(dao.searchByTenantId("t1")).thenReturn(listOf(a, b))

        val result = service.getLoginsByTenantId("t1", 1)
        assertEquals(listOf(b), result)
    }

    @Test
    fun getLoginsByTimeRange_sortsDescendingNoCap() {
        val start = base.minusDays(1)
        val end = base.plusDays(1)
        val a = log("u1", "t1", 0)
        val b = log("u1", "t1", 20)
        whenCalled(dao.searchByFilters("t1", "u1", start, end)).thenReturn(listOf(a, b))

        val result = service.getLoginsByTimeRange("t1", "u1", start, end)
        assertEquals(listOf(b, a), result)
    }

    @Test
    fun getRecentLogins_passesNullTimesAndCaps() {
        val a = log("u1", "t1", 0)
        val b = log("u1", "t1", 20)
        val c = log("u1", "t1", 40)
        whenCalled(dao.searchByFilters(eq("t1"), isNull(), isNull(), isNull())).thenReturn(listOf(a, b, c))

        val result = service.getRecentLogins("t1", null, 2)
        assertEquals(listOf(c, b), result)
    }

    @Test
    fun countLogins_convertsToLong() {
        whenCalled(dao.countByFilters("t1", "u1", null, null)).thenReturn(7)
        assertEquals(7L, service.countLogins("t1", "u1", null, null))
    }

    @Test
    fun countSuccessLogins_passesTrueFlag() {
        whenCalled(dao.countByLoginSuccess(eqBool(true), eq("t1"), isNull(), isNull(), isNull())).thenReturn(4)
        assertEquals(4L, service.countSuccessLogins("t1", null, null, null))
    }

    @Test
    fun countFailureLogins_passesFalseFlag() {
        whenCalled(dao.countByLoginSuccess(eqBool(false), eq("t1"), isNull(), isNull(), isNull())).thenReturn(2)
        assertEquals(2L, service.countFailureLogins("t1", null, null, null))
    }

    // ---- default-argument paths (limit omitted, resolved via the interface synthetic) ----

    @Test
    fun getLoginsByUserId_defaultLimit() {
        val a = log("u1", "t1", 0)
        whenCalled(dao.searchByUserId("u1")).thenReturn(listOf(a))
        assertEquals(listOf(a), iface.getLoginsByUserId("u1"))
    }

    @Test
    fun getLoginsByTenantId_defaultLimit() {
        val a = log("u1", "t1", 0)
        whenCalled(dao.searchByTenantId("t1")).thenReturn(listOf(a))
        assertEquals(listOf(a), iface.getLoginsByTenantId("t1"))
    }

    @Test
    fun getRecentLogins_defaultLimit() {
        val a = log("u1", "t1", 0)
        whenCalled(dao.searchByFilters(eq("t1"), isNull(), isNull(), isNull())).thenReturn(listOf(a))
        assertEquals(listOf(a), iface.getRecentLogins("t1", null))
    }

    // Mockito matcher returns null; a non-null Kotlin Boolean param needs the `?:` fallback.
    private fun eqBool(value: Boolean): Boolean = eq(value) ?: value
}
