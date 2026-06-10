package io.kudos.ms.user.core.passport.service

import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.service.impl.PassportService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure-logic tests for the brute-force login lockout decisions in PassportService:
 * shouldLockLogin (threshold gate) and canArmLoginLock (freeze-record ownership guard).
 *
 * No DB / Spring needed: both functions are pure companion functions.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportLoginLockTest {

    // ---- shouldLockLogin ---------------------------------------------------------------

    @Test
    fun belowThresholdDoesNotLock() {
        assertFalse(PassportService.shouldLockLogin(0, 5))
        assertFalse(PassportService.shouldLockLogin(4, 5))
    }

    @Test
    fun reachingThresholdLocks() {
        assertTrue(PassportService.shouldLockLogin(5, 5))
    }

    @Test
    fun exceedingThresholdStaysLocked() {
        assertTrue(PassportService.shouldLockLogin(6, 5))
        assertTrue(PassportService.shouldLockLogin(100, 5))
    }

    @Test
    fun nonPositiveThresholdDisablesLockout() {
        assertFalse(PassportService.shouldLockLogin(100, 0))
        assertFalse(PassportService.shouldLockLogin(100, -1))
    }

    @Test
    fun thresholdOfOneLocksOnFirstFailure() {
        assertTrue(PassportService.shouldLockLogin(1, 1))
    }

    // ---- canArmLoginLock ---------------------------------------------------------------

    @Test
    fun noFreezeRecordAllowsArming() {
        assertTrue(PassportService.canArmLoginLock(null))
        assertTrue(PassportService.canArmLoginLock(""))
        assertTrue(PassportService.canArmLoginLock("   "))
    }

    @Test
    fun ownLockTypeAllowsRearming() {
        // An expired auto lock must be re-armable on the next failure while the counter is still elevated.
        assertTrue(PassportService.canArmLoginLock(PassportService.LOGIN_LOCK_FREEZE_TYPE))
    }

    @Test
    fun foreignFreezeTypeIsNeverClobbered() {
        assertFalse(PassportService.canArmLoginLock("manual"))
        assertFalse(PassportService.canArmLoginLock("admin"))
        assertFalse(PassportService.canArmLoginLock("scheduled"))
    }

    // ---- PassportLoginResult.locked factory ----------------------------------------------

    @Test
    fun lockedFactoryCarriesStatusAndCount() {
        val res = PassportLoginResult.locked(5)
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        assertEquals(5, res.loginErrorTimes)
        assertNull(res.userInfo)
        assertTrue(!res.message.isNullOrBlank())
    }

    @Test
    fun lockedFactoryAllowsUnknownCount() {
        // Answered from the freeze gate where the exact counter may not be at hand.
        val res = PassportLoginResult.locked()
        assertEquals(PassportLoginStatusEnum.LOCKED, res.status)
        assertNull(res.loginErrorTimes)
    }
}
