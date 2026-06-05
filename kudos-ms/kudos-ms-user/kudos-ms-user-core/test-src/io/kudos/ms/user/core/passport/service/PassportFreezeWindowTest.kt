package io.kudos.ms.user.core.passport.service

import io.kudos.ms.user.core.passport.service.impl.PassportService
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic test for PassportService.isCurrentlyFrozen — the freeze effective-window decision.
 *
 * No DB / Spring needed: the function is a pure companion function. Boundaries are built relative
 * to `now()` with a 1-hour margin so the test is deterministic and never races the wall clock.
 *
 * @author K
 * @since 1.0.0
 */
internal class PassportFreezeWindowTest {

    private val now: LocalDateTime = LocalDateTime.now()
    private val pastHour: LocalDateTime = now.minusHours(1)
    private val past2Hour: LocalDateTime = now.minusHours(2)
    private val futureHour: LocalDateTime = now.plusHours(1)

    @Test
    fun noFreezeTypeMeansNotFrozen() {
        assertFalse(PassportService.isCurrentlyFrozen(null, pastHour, futureHour))
        assertFalse(PassportService.isCurrentlyFrozen("", pastHour, futureHour))
        assertFalse(PassportService.isCurrentlyFrozen("   ", pastHour, futureHour))
    }

    @Test
    fun nullStartAndNullEndMeansPermanentlyFrozen() {
        assertTrue(PassportService.isCurrentlyFrozen("ADMIN", null, null))
    }

    @Test
    fun futureStartIsNotYetEffective() {
        assertFalse(PassportService.isCurrentlyFrozen("ADMIN", futureHour, null))
        assertFalse(PassportService.isCurrentlyFrozen("ADMIN", futureHour, futureHour.plusHours(1)))
    }

    @Test
    fun pastStartWithNoEndIsEffectiveForever() {
        assertTrue(PassportService.isCurrentlyFrozen("ADMIN", pastHour, null))
    }

    @Test
    fun withinWindowIsFrozen() {
        assertTrue(PassportService.isCurrentlyFrozen("ADMIN", pastHour, futureHour))
    }

    @Test
    fun expiredWindowIsNotFrozen() {
        assertFalse(PassportService.isCurrentlyFrozen("ADMIN", past2Hour, pastHour))
    }

    @Test
    fun nullStartUsesUpperBoundOnly() {
        assertTrue(PassportService.isCurrentlyFrozen("ADMIN", null, futureHour))
        assertFalse(PassportService.isCurrentlyFrozen("ADMIN", null, pastHour))
    }
}
