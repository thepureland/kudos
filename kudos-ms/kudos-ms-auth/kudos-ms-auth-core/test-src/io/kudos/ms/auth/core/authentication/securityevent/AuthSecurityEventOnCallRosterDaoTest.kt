package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.IAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.cache.AuthSecurityEventNotificationRouteCache
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.event.AuthSecurityEventNotificationRouteChanged
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallRosterSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.AuthSecurityEventOnCallShiftCommand
import io.kudos.ms.auth.core.authentication.securityevent.oncall.cache.AuthSecurityEventOnCallRosterCache
import io.kudos.ms.auth.core.authentication.securityevent.oncall.dao.AuthSecurityEventOnCallRosterDao
import io.kudos.ms.auth.core.authentication.securityevent.oncall.event.AuthSecurityEventOnCallRosterChanged
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.spi.IAuthSecurityEventResponderResolver
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Real Flyway/H2 regression for the V54 on-call rotation: whole-set replacement under optimistic concurrency,
 * tenant isolation, cache invalidation, and a route rule that reaches whoever the rotation puts on call.
 */
@EnabledIfDockerInstalled
internal class AuthSecurityEventOnCallRosterDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var rosterService: IAuthSecurityEventOnCallRosterService

    @Resource
    private lateinit var rosterDao: AuthSecurityEventOnCallRosterDao

    @Resource
    private lateinit var rosterCache: AuthSecurityEventOnCallRosterCache

    @Resource
    private lateinit var responderResolver: IAuthSecurityEventResponderResolver

    @Resource
    private lateinit var routeService: IAuthSecurityEventNotificationRouteConfigService

    @Resource
    private lateinit var routeCache: AuthSecurityEventNotificationRouteCache

    @Resource
    private lateinit var routePolicy: IAuthSecurityEventNotificationRoutePolicy

    @Test
    fun v54ReplacesTheWholeRotationUnderOptimisticConcurrencyAndKeepsTenantsApart() {
        val tenantId = UUID.randomUUID().toString()
        val otherTenantId = UUID.randomUUID().toString()

        val created = rosterService.save(rosterCommand(tenantId))
        assertEquals(1L, created.configVersion)
        assertEquals(listOf("primary-1", "secondary-1"), created.shifts.map { it.responderUserId })

        val stale = assertFailsWith<AuthSecurityEventException> {
            rosterService.save(rosterCommand(tenantId, expectedVersion = 0L))
        }
        assertEquals("AUTH_SECURITY_EVENT_ONCALL_VERSION_CONFLICT", stale.errorCode)

        val replaced = rosterService.save(
            rosterCommand(
                tenantId,
                expectedVersion = 1L,
                shifts = listOf(shift("weekend-1", tier = 1)),
            )
        )
        assertEquals(2L, replaced.configVersion)
        assertEquals(listOf("weekend-1"), replaced.shifts.map { it.responderUserId })
        assertEquals(listOf("weekend-1"), rosterService.listByTenant(tenantId).single().shifts.map { it.responderUserId })
        assertEquals(emptyList(), rosterService.listByTenant(otherTenantId))

        val rosterId = rosterDao.findByCode(tenantId, ROSTER_CODE)!!.id
        val audits = rosterDao.findChangeAudits(tenantId, rosterId, 10)
        assertEquals(listOf(2L, 1L), audits.map { it.configVersion })
        assertNull(audits.last().beforeSnapshot)
        assertTrue(audits.first().beforeSnapshot!!.contains("primary-1@1:"))
        assertTrue(audits.first().afterSnapshot.contains("weekend-1@1:"))
    }

    @Test
    fun v54ResolvesRespondersFromTheRotationAndReRoutesAfterItChanges() {
        val tenantId = UUID.randomUUID().toString()
        rosterService.save(rosterCommand(tenantId))
        rosterCache.on(AuthSecurityEventOnCallRosterChanged(tenantId))
        routeService.save(routeCommand(tenantId))
        routeCache.on(AuthSecurityEventNotificationRouteChanged(tenantId))

        // Tier 1 only at escalation level 1; the second tier joins when the incident escalates again.
        assertEquals(setOf("primary-1"), responderResolver.resolve(tenantId, ROSTER_CODE, 1, NOW))
        assertEquals(
            setOf("primary-1", "secondary-1"),
            responderResolver.resolve(tenantId, ROSTER_CODE, 2, NOW),
        )
        val routed = routePolicy.resolve(delivery(tenantId, escalationLevel = 2))
        assertEquals("DUTY", routed.routeCode)
        assertTrue(routed.recipientUserIds.containsAll(setOf("primary-1", "secondary-1")))

        // A rotation edit takes effect on the next attempt without touching the outbox row.
        rosterService.save(
            rosterCommand(tenantId, expectedVersion = 1L, shifts = listOf(shift("handover-1", tier = 1)))
        )
        rosterCache.on(AuthSecurityEventOnCallRosterChanged(tenantId))

        assertEquals(setOf("handover-1"), responderResolver.resolve(tenantId, ROSTER_CODE, 2, NOW))
        assertEquals(setOf("handover-1"), routePolicy.resolve(delivery(tenantId, escalationLevel = 2)).recipientUserIds)

        // Disabling the rotation leaves the route rule addressing nobody, so its fallback decides.
        rosterService.save(rosterCommand(tenantId, expectedVersion = 2L, enabled = false))
        rosterCache.on(AuthSecurityEventOnCallRosterChanged(tenantId))

        assertNull(rosterService.findEnabled(tenantId, ROSTER_CODE))
        assertEquals("TENANT_SECURITY_QUEUE", routePolicy.resolve(delivery(tenantId, escalationLevel = 2)).routeCode)
    }

    @Test
    fun v54RefusesRouteRulesNamingARotationTheTenantDoesNotHave() {
        val tenantId = UUID.randomUUID().toString()

        val missing = assertFailsWith<AuthSecurityEventException> { routeService.save(routeCommand(tenantId)) }

        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_ROSTER_NOT_FOUND", missing.errorCode)
    }

    private fun rosterCommand(
        tenantId: String,
        enabled: Boolean = true,
        shifts: List<AuthSecurityEventOnCallShiftCommand> = listOf(
            shift("primary-1", tier = 1),
            shift("secondary-1", tier = 2),
        ),
        expectedVersion: Long = 0L,
    ) = AuthSecurityEventOnCallRosterSaveCommand(
        tenantId = tenantId,
        rosterCode = ROSTER_CODE,
        displayName = "Security duty",
        enabled = enabled,
        shifts = shifts,
        expectedVersion = expectedVersion,
        actorUserId = "operator-1",
        reason = "weekly rotation",
    )

    private fun shift(responderUserId: String, tier: Int) = AuthSecurityEventOnCallShiftCommand(
        responderUserId = responderUserId,
        tier = tier,
        startAt = NOW.minusYears(1),
        endAt = NOW.plusYears(1),
    )

    private fun routeCommand(tenantId: String) = AuthSecurityEventNotificationRouteSaveCommand(
        tenantId = tenantId,
        notificationType = "SLA_ESCALATED",
        appliesTo = "UNASSIGNED",
        routeCode = "DUTY",
        destination = "USER",
        channels = setOf("SMS"),
        responderUserIds = emptySet(),
        responderRosterCode = ROSTER_CODE,
        includeAssignee = false,
        enabled = true,
        fallbackBehavior = "TENANT_SECURITY_QUEUE",
        expectedVersion = 0L,
        actorUserId = "operator-1",
        reason = "route to duty rotation",
    )

    private fun delivery(tenantId: String, escalationLevel: Int) = AuthSecurityEventNotificationDelivery(
        id = UUID.randomUUID().toString(),
        tenantId = tenantId,
        eventId = UUID.randomUUID().toString(),
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        escalationLevel = escalationLevel,
        recipientUserId = null,
        dueAt = NOW.minusHours(1),
        escalatedAt = NOW,
        attemptCount = 1,
        leaseUntil = NOW.plusMinutes(1),
    )

    private companion object {
        const val ROSTER_CODE = "SECURITY"

        /**
         * The rotation windows deliberately span a year either side of this instant, so the test asserts the
         * tier and cache behaviour rather than racing the wall clock the policy reads.
         */
        val NOW: LocalDateTime = LocalDateTime.now()
    }
}
