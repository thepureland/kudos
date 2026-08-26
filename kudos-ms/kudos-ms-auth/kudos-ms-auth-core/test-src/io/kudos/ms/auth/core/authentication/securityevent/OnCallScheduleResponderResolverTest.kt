package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRoster
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShift
import io.kudos.ms.auth.core.authentication.securityevent.oncall.OnCallScheduleResponderResolver
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OnCallScheduleResponderResolverTest {
    private val rosterService = mock(IAuthSecurityEventOnCallRosterService::class.java)
    private val resolver = OnCallScheduleResponderResolver(rosterService)

    @Test
    fun escalatingAddsHigherTiersInsteadOfHandingOverToThem() {
        `when`(rosterService.findEnabled("tenant-1", "SECURITY")).thenReturn(
            roster(
                shift("primary-1", tier = 1),
                shift("secondary-1", tier = 2),
                shift("manager-1", tier = 3),
            )
        )

        assertEquals(setOf("primary-1"), resolve(escalationLevel = 1))
        assertEquals(setOf("primary-1", "secondary-1"), resolve(escalationLevel = 2))
        assertEquals(setOf("primary-1", "secondary-1", "manager-1"), resolve(escalationLevel = 3))
    }

    @Test
    fun onlyShiftsCoveringTheInstantCount() {
        `when`(rosterService.findEnabled("tenant-1", "SECURITY")).thenReturn(
            roster(
                shift("yesterday-1", tier = 1, startAt = NOW.minusDays(1), endAt = NOW.minusHours(1)),
                shift("current-1", tier = 1),
                // Half-open windows: the shift starting exactly at NOW is the successor, not a second holder.
                shift("next-1", tier = 1, startAt = NOW.plusHours(1), endAt = NOW.plusHours(2)),
            )
        )

        assertEquals(setOf("current-1"), resolve(escalationLevel = 1))
        assertEquals(setOf("next-1"), resolver.resolve("tenant-1", "SECURITY", 1, NOW.plusHours(1)))
    }

    @Test
    fun anAbsentOrDisabledRotationContributesNobodyRatherThanGuessing() {
        `when`(rosterService.findEnabled("tenant-1", "SECURITY")).thenReturn(null)

        assertEquals(emptySet(), resolve(escalationLevel = 3))
    }

    @Test
    fun aRotationWithNobodyOnCallRightNowIsAnEmptyAnswerNotAnError() {
        `when`(rosterService.findEnabled("tenant-1", "SECURITY")).thenReturn(
            roster(shift("gap-1", tier = 1, startAt = NOW.plusDays(1), endAt = NOW.plusDays(2)))
        )

        assertEquals(emptySet(), resolve(escalationLevel = 5))
    }

    private fun resolve(escalationLevel: Int) =
        resolver.resolve("tenant-1", "SECURITY", escalationLevel, NOW)

    private fun shift(
        responderUserId: String,
        tier: Int,
        startAt: LocalDateTime = NOW.minusHours(1),
        endAt: LocalDateTime = NOW.plusHours(1),
    ) = AuthSecurityEventOnCallShift(
        id = "shift-$responderUserId",
        responderUserId = responderUserId,
        tier = tier,
        startAt = startAt,
        endAt = endAt,
    )

    private fun roster(vararg shifts: AuthSecurityEventOnCallShift) = AuthSecurityEventOnCallRoster(
        tenantId = "tenant-1",
        rosterCode = "SECURITY",
        displayName = "Security duty",
        enabled = true,
        shifts = shifts.toList(),
        configVersion = 1L,
        createUserId = "operator-1",
        createReason = "initial",
        createTime = NOW,
        updateUserId = "operator-1",
        updateReason = "initial",
        updateTime = NOW,
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.parse("2026-08-25T10:00:00")
    }
}
