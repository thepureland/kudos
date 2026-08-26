package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterAuditRecord
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShiftCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.cache.AuthSecurityEventOnCallRosterCache
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallRosterDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallShiftDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.event.AuthSecurityEventOnCallRosterChanged
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallRosterPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.model.po.AuthSecurityEventOnCallShiftPo
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.impl.AuthSecurityEventOnCallRosterService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthSecurityEventOnCallRosterServiceTest {
    private val instant = Instant.parse("2026-08-25T10:00:00Z")
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val rosterDao = mock(AuthSecurityEventOnCallRosterDao::class.java)
    private val shiftDao = mock(AuthSecurityEventOnCallShiftDao::class.java)
    private val cache = mock(AuthSecurityEventOnCallRosterCache::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val service =
        AuthSecurityEventOnCallRosterService(rosterDao, shiftDao, cache, eventPublisher, clock)

    @Test
    fun firstSaveStoresTheRotationAtVersionOneAndRecordsIt() {
        `when`(rosterDao.findByCode("tenant-1", "SECURITY")).thenReturn(null)

        val saved = service.save(command())

        assertEquals(1L, saved.configVersion)
        assertEquals("SECURITY", saved.rosterCode)
        assertEquals(listOf("primary-1", "secondary-1"), saved.shifts.map { it.responderUserId })
        val audit = ArgumentCaptor.forClass(AuthSecurityEventOnCallRosterAuditRecord::class.java)
        verify(rosterDao).insertChangeAudit(audit.capture() ?: auditRecord())
        assertEquals("tenant-1", audit.value.tenantId)
        assertEquals("operator-1", audit.value.actorUserId)
        assertNull(audit.value.beforeSnapshot)
        assertTrue(audit.value.afterSnapshot.contains("primary-1@1:"))
        verify(eventPublisher).publishEvent(AuthSecurityEventOnCallRosterChanged("tenant-1"))
    }

    @Test
    fun savingReplacesTheWholeShiftSetRatherThanMergingIntoIt() {
        val existing = rosterPo()
        `when`(rosterDao.findByCode("tenant-1", "SECURITY")).thenReturn(existing)
        `when`(shiftDao.findByTenant("tenant-1")).thenReturn(listOf(shiftPo("old-1")))
        `when`(rosterDao.updateWithVersion(existing, 2L)).thenReturn(true)

        val saved = service.save(command(expectedVersion = 2L))

        assertEquals(3L, saved.configVersion)
        verify(shiftDao).deleteByRoster("tenant-1", "roster-1")
        assertEquals(listOf("primary-1", "secondary-1"), saved.shifts.map { it.responderUserId })
        val audit = ArgumentCaptor.forClass(AuthSecurityEventOnCallRosterAuditRecord::class.java)
        verify(rosterDao).insertChangeAudit(audit.capture() ?: auditRecord())
        assertTrue(audit.value.beforeSnapshot!!.contains("old-1@1:"))
        assertTrue(!audit.value.afterSnapshot.contains("old-1@1:"))
    }

    @Test
    fun staleVersionsAndConcurrentCreatesAreRefusedWithoutWriting() {
        val existing = rosterPo()
        `when`(rosterDao.findByCode("tenant-1", "SECURITY")).thenReturn(existing, existing, null)
        `when`(rosterDao.updateWithVersion(existing, 2L)).thenReturn(false)
        `when`(rosterDao.insert(any(AuthSecurityEventOnCallRosterPo::class.java) ?: rosterPo()))
            .thenThrow(DataIntegrityViolationException("uk_auth_security_event_oncall_roster_code"))

        val stale = assertFailsWith<AuthSecurityEventException> { service.save(command(expectedVersion = 1L)) }
        val lostRace = assertFailsWith<AuthSecurityEventException> { service.save(command(expectedVersion = 2L)) }
        val duplicate = assertFailsWith<AuthSecurityEventException> { service.save(command()) }

        assertEquals("AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT", stale.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT", lostRace.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT", duplicate.errorCode)
        verify(shiftDao, never()).deleteByRoster("tenant-1", "roster-1")
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun malformedRotationsAreRefusedBeforeAnybodyCanBePagedByThem() {
        val cases = mapOf(
            "AUTH_SECURITY_EVENT_ONCALL_TENANT_INVALID" to command(tenantId = " "),
            "AUTH_SECURITY_EVENT_ONCALL_ACTOR_INVALID" to command(actorUserId = ""),
            "AUTH_SECURITY_EVENT_ONCALL_REASON_INVALID" to command(reason = " "),
            "AUTH_SECURITY_EVENT_ONCALL_VERSION_INVALID" to command(expectedVersion = -1L),
            "AUTH_SECURITY_EVENT_ONCALL_ROSTER_CODE_INVALID" to command(rosterCode = "lower case"),
            "AUTH_SECURITY_EVENT_ONCALL_ROSTER_NAME_INVALID" to command(displayName = " "),
            "AUTH_SECURITY_EVENT_ONCALL_SHIFT_RESPONDER_INVALID" to
                command(shifts = listOf(shiftCommand("x".repeat(37), 1))),
            "AUTH_SECURITY_EVENT_ONCALL_SHIFT_TIER_INVALID" to
                command(shifts = listOf(shiftCommand("primary-1", 6))),
            "AUTH_SECURITY_EVENT_ONCALL_SHIFT_WINDOW_INVALID" to
                command(shifts = listOf(shiftCommand("primary-1", 1, endAt = now.minusHours(2)))),
            "AUTH_SECURITY_EVENT_ONCALL_SHIFT_DUPLICATED" to
                command(shifts = listOf(shiftCommand("primary-1", 1), shiftCommand("primary-1", 1))),
            "AUTH_SECURITY_EVENT_ONCALL_SHIFTS_TOO_MANY" to
                command(shifts = (1..101).map { shiftCommand("responder-$it", 1) }),
        )

        cases.forEach { (errorCode, command) ->
            val failure = assertFailsWith<AuthSecurityEventException> { service.save(command) }
            assertEquals(errorCode, failure.errorCode)
        }
        verify(rosterDao, never()).insert(any(AuthSecurityEventOnCallRosterPo::class.java) ?: rosterPo())
    }

    @Test
    fun deliveryPathLookupUsesTheCacheAndIgnoresDisabledRotations() {
        `when`(cache.getRosters("tenant-1")).thenReturn(emptyList())

        assertNull(service.findEnabled("tenant-1", "SECURITY"))
        val invalidCode = assertFailsWith<AuthSecurityEventException> { service.findEnabled("tenant-1", "bad code") }

        assertEquals("AUTH_SECURITY_EVENT_ONCALL_ROSTER_CODE_INVALID", invalidCode.errorCode)
        verify(rosterDao, never()).findByTenant("tenant-1")
    }

    private fun command(
        tenantId: String = "tenant-1",
        rosterCode: String = "SECURITY",
        displayName: String = "Security duty",
        enabled: Boolean = true,
        shifts: List<AuthSecurityEventOnCallShiftCommand> = listOf(
            shiftCommand("primary-1", 1),
            shiftCommand("secondary-1", 2),
        ),
        expectedVersion: Long = 0L,
        actorUserId: String = "operator-1",
        reason: String = "weekly rotation",
    ) = AuthSecurityEventOnCallRosterSaveCommand(
        tenantId = tenantId,
        rosterCode = rosterCode,
        displayName = displayName,
        enabled = enabled,
        shifts = shifts,
        expectedVersion = expectedVersion,
        actorUserId = actorUserId,
        reason = reason,
    )

    private fun shiftCommand(
        responderUserId: String,
        tier: Int,
        startAt: LocalDateTime = now.minusHours(1),
        endAt: LocalDateTime = now.plusHours(1),
    ) = AuthSecurityEventOnCallShiftCommand(responderUserId, tier, startAt, endAt)

    private fun rosterPo() = AuthSecurityEventOnCallRosterPo {
        id = "roster-1"
        tenantId = "tenant-1"
        rosterCode = "SECURITY"
        displayName = "Security duty"
        enabled = true
        configVersion = 2L
        createUserId = "operator-0"
        createReason = "initial"
        createTime = now.minusDays(1)
        updateUserId = "operator-0"
        updateReason = "initial"
        updateTime = now.minusDays(1)
    }

    private fun shiftPo(responder: String) = AuthSecurityEventOnCallShiftPo {
        id = "shift-$responder"
        tenantId = "tenant-1"
        rosterId = "roster-1"
        responderUserId = responder
        tier = 1
        startAt = now.minusHours(1)
        endAt = now.plusHours(1)
    }

    private fun auditRecord() = AuthSecurityEventOnCallRosterAuditRecord(
        id = "audit-1",
        tenantId = "tenant-1",
        rosterId = "roster-1",
        actorUserId = "operator-1",
        reason = "weekly rotation",
        configVersion = 1L,
        beforeSnapshot = null,
        afterSnapshot = "displayName=Security duty;enabled=true;shifts=",
        changedAt = now,
    )
}
