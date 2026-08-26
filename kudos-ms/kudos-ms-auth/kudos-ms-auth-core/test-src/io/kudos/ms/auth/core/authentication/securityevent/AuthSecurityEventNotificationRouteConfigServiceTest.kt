package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAppliesToEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAuditRecord
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteFallbackEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.cache.AuthSecurityEventNotificationRouteCache
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.dao.AuthSecurityEventNotificationRouteDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.event.AuthSecurityEventNotificationRouteChanged
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.model.po.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.impl.AuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
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

internal class AuthSecurityEventNotificationRouteConfigServiceTest {
    private val instant = Instant.parse("2026-08-25T10:00:00Z")
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val dao = mock(AuthSecurityEventNotificationRouteDao::class.java)
    private val cache = mock(AuthSecurityEventNotificationRouteCache::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)
    private val rosterService = mock(IAuthSecurityEventOnCallRosterService::class.java)
    private val service =
        AuthSecurityEventNotificationRouteConfigService(dao, cache, rosterService, eventPublisher, clock)

    @Test
    fun firstSaveStoresTheRuleAtVersionOneAndAppendsChangeEvidence() {
        `when`(dao.findByScope("tenant-1", "SLA_ESCALATED", "ASSIGNED")).thenReturn(null)

        val saved = service.save(command())

        assertEquals(1L, saved.configVersion)
        assertEquals("ASSIGNEE_EMAIL", saved.routeCode)
        assertEquals(AuthSecurityEventNotificationDestinationEnum.USER, saved.destination)
        assertEquals(
            setOf(
                AuthSecurityEventNotificationChannelEnum.EMAIL,
                AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE,
            ),
            saved.channels,
        )
        assertEquals(setOf("responder-1"), saved.responderUserIds)
        assertEquals("operator-1", saved.createUserId)
        assertEquals(now, saved.updateTime)
        val stored = ArgumentCaptor.forClass(AuthSecurityEventNotificationRoute::class.java)
        verify(dao).insert(stored.capture() ?: route())
        assertEquals("EMAIL,SITE_MESSAGE", stored.value.channels)
        assertEquals("responder-1", stored.value.responderUserIds)
        assertEquals(1L, stored.value.configVersion)
        val audit = ArgumentCaptor.forClass(AuthSecurityEventNotificationRouteAuditRecord::class.java)
        verify(dao).insertChangeAudit(audit.capture() ?: auditRecord())
        assertEquals("tenant-1", audit.value.tenantId)
        assertEquals("operator-1", audit.value.actorUserId)
        assertEquals("duty roster change", audit.value.reason)
        assertEquals(1L, audit.value.configVersion)
        assertNull(audit.value.beforeSnapshot)
        assertEquals(EXPECTED_SNAPSHOT, audit.value.afterSnapshot)
        assertEquals(now, audit.value.changedAt)
        verify(eventPublisher).publishEvent(AuthSecurityEventNotificationRouteChanged("tenant-1"))
    }

    @Test
    fun updateRequiresTheDisplayedVersionAndCompareAndSetToWin() {
        val existing = route()
        `when`(dao.findByScope("tenant-1", "SLA_ESCALATED", "ASSIGNED")).thenReturn(existing)
        `when`(dao.updateWithVersion(existing, 3L)).thenReturn(true)

        val saved = service.save(command(expectedVersion = 3L))

        assertEquals(4L, saved.configVersion)
        verify(dao, never()).insert(any(AuthSecurityEventNotificationRoute::class.java) ?: route())
        val audit = ArgumentCaptor.forClass(AuthSecurityEventNotificationRouteAuditRecord::class.java)
        verify(dao).insertChangeAudit(audit.capture() ?: auditRecord())
        assertEquals("route-1", audit.value.routeId)
        assertEquals(4L, audit.value.configVersion)
        assertEquals(PREVIOUS_SNAPSHOT, audit.value.beforeSnapshot)
        assertEquals(EXPECTED_SNAPSHOT, audit.value.afterSnapshot)
    }

    @Test
    fun staleVersionsAndLostCompareAndSetRacesAreRefusedWithoutWriting() {
        val existing = route()
        `when`(dao.findByScope("tenant-1", "SLA_ESCALATED", "ASSIGNED")).thenReturn(existing)
        `when`(dao.updateWithVersion(existing, 3L)).thenReturn(false)

        val stale = assertFailsWith<AuthSecurityEventException> { service.save(command(expectedVersion = 2L)) }
        val lostRace = assertFailsWith<AuthSecurityEventException> { service.save(command(expectedVersion = 3L)) }
        val createOverExisting = assertFailsWith<AuthSecurityEventException> { service.save(command()) }

        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT", stale.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT", lostRace.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT", createOverExisting.errorCode)
        verify(dao, never()).insertChangeAudit(
            any(AuthSecurityEventNotificationRouteAuditRecord::class.java) ?: auditRecord()
        )
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun aConcurrentCreateOfTheSameScopeLosesInsteadOfDuplicating() {
        `when`(dao.findByScope("tenant-1", "SLA_ESCALATED", "ASSIGNED")).thenReturn(null)
        `when`(dao.insert(any(AuthSecurityEventNotificationRoute::class.java) ?: route()))
            .thenThrow(DataIntegrityViolationException("uk_auth_security_event_notification_route_scope"))

        val conflict = assertFailsWith<AuthSecurityEventException> { service.save(command()) }

        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT", conflict.errorCode)
        verifyNoInteractions(eventPublisher)
    }

    @Test
    fun malformedRulesAreRefusedBeforeTheyCanReachTheDeliveryPath() {
        val cases = mapOf(
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_TENANT_INVALID" to command(tenantId = " "),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_ACTOR_INVALID" to command(actorUserId = ""),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_REASON_INVALID" to command(reason = " "),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_INVALID" to command(expectedVersion = -1L),
            "AUTH_SECURITY_EVENT_NOTIFICATION_TYPE_INVALID" to command(notificationType = "UNKNOWN"),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_APPLIES_TO_INVALID" to command(appliesTo = "ANY"),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_DESTINATION_INVALID" to command(destination = "PAGER"),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_FALLBACK_INVALID" to command(fallbackBehavior = "IGNORE"),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_CODE_INVALID" to command(routeCode = "lower case"),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_CHANNEL_INVALID" to command(channels = emptySet()),
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_RESPONDERS_INVALID" to
                command(responderUserIds = setOf("x".repeat(37))),
        )

        cases.forEach { (errorCode, command) ->
            val failure = assertFailsWith<AuthSecurityEventException> { service.save(command) }
            assertEquals(errorCode, failure.errorCode)
        }
        verify(dao, never()).insert(any(AuthSecurityEventNotificationRoute::class.java) ?: route())
    }

    @Test
    fun rulesThatCouldNeverAddressAnybodyAreRefusedAtConfigurationTime() {
        val unassignedWithAssignee = assertFailsWith<AuthSecurityEventException> {
            service.save(command(appliesTo = "UNASSIGNED", includeAssignee = true))
        }
        val unassignedWithoutResponders = assertFailsWith<AuthSecurityEventException> {
            service.save(command(appliesTo = "UNASSIGNED", includeAssignee = false, responderUserIds = emptySet()))
        }
        val assignedAddressingNobody = assertFailsWith<AuthSecurityEventException> {
            service.save(command(includeAssignee = false, responderUserIds = emptySet()))
        }

        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INCLUDE_ASSIGNEE_INVALID",
            unassignedWithAssignee.errorCode,
        )
        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_RESPONDERS_REQUIRED",
            unassignedWithoutResponders.errorCode,
        )
        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_RESPONDERS_REQUIRED",
            assignedAddressingNobody.errorCode,
        )
    }

    @Test
    fun effectiveLookupUsesTheCachedRuleSetAndIgnoresDisabledRules() {
        `when`(cache.getRoutes("tenant-1")).thenReturn(
            listOf(
                config(AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED, enabled = true),
                config(AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED, enabled = false),
            )
        )

        val assigned = service.findEffective(
            "tenant-1",
            AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
            AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED,
        )
        val unassigned = service.findEffective(
            "tenant-1",
            AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED,
        )

        assertEquals("ASSIGNEE_EMAIL", assigned?.routeCode)
        assertNull(unassigned)
        verify(dao, never()).findByTenant(anyString())
    }

    private fun command(
        tenantId: String = "tenant-1",
        notificationType: String = "SLA_ESCALATED",
        appliesTo: String = "ASSIGNED",
        routeCode: String = "ASSIGNEE_EMAIL",
        destination: String = "USER",
        channels: Set<String> = setOf("EMAIL", "SITE_MESSAGE"),
        responderUserIds: Set<String> = setOf("responder-1"),
        responderRosterCode: String? = null,
        includeAssignee: Boolean = true,
        enabled: Boolean = true,
        fallbackBehavior: String = "DEFAULT_ROUTE",
        expectedVersion: Long = 0L,
        actorUserId: String = "operator-1",
        reason: String = "duty roster change",
    ) = AuthSecurityEventNotificationRouteSaveCommand(
        tenantId = tenantId,
        notificationType = notificationType,
        appliesTo = appliesTo,
        routeCode = routeCode,
        destination = destination,
        channels = channels,
        responderUserIds = responderUserIds,
        responderRosterCode = responderRosterCode,
        includeAssignee = includeAssignee,
        enabled = enabled,
        fallbackBehavior = fallbackBehavior,
        expectedVersion = expectedVersion,
        actorUserId = actorUserId,
        reason = reason,
    )

    private fun config(
        appliesTo: AuthSecurityEventNotificationRouteAppliesToEnum,
        enabled: Boolean,
    ) = AuthSecurityEventNotificationRouteConfig(
        tenantId = "tenant-1",
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        appliesTo = appliesTo,
        routeCode = "ASSIGNEE_EMAIL",
        destination = AuthSecurityEventNotificationDestinationEnum.USER,
        channels = setOf(AuthSecurityEventNotificationChannelEnum.EMAIL),
        responderUserIds = setOf("responder-1"),
        responderRosterCode = null,
        includeAssignee = true,
        enabled = enabled,
        fallbackBehavior = AuthSecurityEventNotificationRouteFallbackEnum.DEFAULT_ROUTE,
        configVersion = 1L,
        createUserId = "operator-1",
        createReason = "initial",
        createTime = now,
        updateUserId = "operator-1",
        updateReason = "initial",
        updateTime = now,
    )

    private fun route() = AuthSecurityEventNotificationRoute {
        id = "route-1"
        tenantId = "tenant-1"
        notificationType = "SLA_ESCALATED"
        appliesTo = "ASSIGNED"
        routeCode = "OLD_ROUTE"
        destination = "TENANT_SECURITY_QUEUE"
        channels = "EVENT_BUS"
        responderUserIds = null
        responderRosterCode = null
        includeAssignee = false
        enabled = true
        fallbackBehavior = "DEFAULT_ROUTE"
        configVersion = 3L
        createUserId = "operator-0"
        createReason = "initial"
        createTime = now.minusDays(1)
        updateUserId = "operator-0"
        updateReason = "initial"
        updateTime = now.minusDays(1)
    }

    private fun auditRecord() = AuthSecurityEventNotificationRouteAuditRecord(
        id = "audit-1",
        tenantId = "tenant-1",
        routeId = "route-1",
        actorUserId = "operator-1",
        reason = "duty roster change",
        configVersion = 1L,
        beforeSnapshot = null,
        afterSnapshot = EXPECTED_SNAPSHOT,
        changedAt = now,
    )

    private companion object {
        const val EXPECTED_SNAPSHOT = "routeCode=ASSIGNEE_EMAIL;destination=USER;channels=EMAIL,SITE_MESSAGE;" +
            "responderUserIds=responder-1;responderRosterCode=;includeAssignee=true;enabled=true;" +
            "fallbackBehavior=DEFAULT_ROUTE"
        const val PREVIOUS_SNAPSHOT = "routeCode=OLD_ROUTE;destination=TENANT_SECURITY_QUEUE;channels=EVENT_BUS;" +
            "responderUserIds=;responderRosterCode=;includeAssignee=false;enabled=true;" +
            "fallbackBehavior=DEFAULT_ROUTE"
    }
}
