package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAppliesToEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteSaveCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.IAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.cache.AuthSecurityEventNotificationRouteCache
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.dao.AuthSecurityEventNotificationRouteDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.event.AuthSecurityEventNotificationRouteChanged
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
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
 * Real Flyway/H2 regression for the V53 tenant routing table, its optimistic concurrency, its cache
 * invalidation and the delivery-path policy that reads it.
 */
@EnabledIfDockerInstalled
internal class AuthSecurityEventNotificationRouteDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthSecurityEventNotificationRouteConfigService

    @Resource
    private lateinit var dao: AuthSecurityEventNotificationRouteDao

    @Resource
    private lateinit var cache: AuthSecurityEventNotificationRouteCache

    @Resource
    private lateinit var routePolicy: IAuthSecurityEventNotificationRoutePolicy

    @Test
    fun v53PersistsTenantRoutesUnderOptimisticConcurrencyAndKeepsTenantsApart() {
        val tenantId = UUID.randomUUID().toString()
        val otherTenantId = UUID.randomUUID().toString()

        val created = service.save(command(tenantId))
        assertEquals(1L, created.configVersion)
        assertEquals(setOf("duty-1"), created.responderUserIds)

        val stale = assertFailsWith<AuthSecurityEventException> {
            service.save(command(tenantId, routeCode = "OTHER", expectedVersion = 0L))
        }
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_VERSION_CONFLICT", stale.errorCode)

        val updated = service.save(
            command(tenantId, routeCode = "ASSIGNEE_SMS", channels = setOf("SMS"), expectedVersion = 1L)
        )
        assertEquals(2L, updated.configVersion)
        assertEquals("ASSIGNEE_SMS", service.listByTenant(tenantId).single().routeCode)
        assertEquals(emptyList(), service.listByTenant(otherTenantId))

        val routeId = dao.findByScope(tenantId, "SLA_ESCALATED", "ASSIGNED")!!.id
        val audits = dao.findChangeAudits(tenantId, routeId, 10)
        assertEquals(listOf(2L, 1L), audits.map { it.configVersion })
        assertNull(audits.last().beforeSnapshot)
        assertTrue(audits.first().beforeSnapshot!!.contains("routeCode=ASSIGNEE_EMAIL"))
        assertTrue(audits.first().afterSnapshot.contains("routeCode=ASSIGNEE_SMS"))
        assertEquals("operator-1", audits.first().actorUserId)
    }

    @Test
    fun v53RoutesEachAttemptThroughTheStoredRuleAndInvalidatesTheCacheOnChange() {
        val tenantId = UUID.randomUUID().toString()

        // No configuration: the pre-V53 default still decides.
        assertEquals("ASSIGNEE", routePolicy.resolve(delivery(tenantId, "assignee-1")).routeCode)

        service.save(command(tenantId))
        cache.on(AuthSecurityEventNotificationRouteChanged(tenantId))

        val configured = routePolicy.resolve(delivery(tenantId, "assignee-1"))
        assertEquals("ASSIGNEE_EMAIL", configured.routeCode)
        assertEquals(setOf(AuthSecurityEventNotificationChannelEnum.EMAIL), configured.channels)
        assertEquals(setOf("assignee-1", "duty-1"), configured.recipientUserIds)
        assertEquals(AuthSecurityEventNotificationDestinationEnum.USER, configured.destination)

        // Disabling the rule is how a tenant goes back to the built-in behaviour; nothing is deleted.
        service.save(command(tenantId, enabled = false, expectedVersion = 1L))
        cache.on(AuthSecurityEventNotificationRouteChanged(tenantId))

        assertNull(
            service.findEffective(
                tenantId,
                AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
                AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED,
            )
        )
        assertEquals("ASSIGNEE", routePolicy.resolve(delivery(tenantId, "assignee-1")).routeCode)
        assertEquals(1, service.listByTenant(tenantId).size)
    }

    private fun command(
        tenantId: String,
        routeCode: String = "ASSIGNEE_EMAIL",
        channels: Set<String> = setOf("EMAIL"),
        enabled: Boolean = true,
        expectedVersion: Long = 0L,
        responderRosterCode: String? = null,
    ) = AuthSecurityEventNotificationRouteSaveCommand(
        tenantId = tenantId,
        notificationType = "SLA_ESCALATED",
        appliesTo = "ASSIGNED",
        routeCode = routeCode,
        destination = "USER",
        channels = channels,
        responderUserIds = setOf("duty-1"),
        responderRosterCode = responderRosterCode,
        includeAssignee = true,
        enabled = enabled,
        fallbackBehavior = "DEFAULT_ROUTE",
        expectedVersion = expectedVersion,
        actorUserId = "operator-1",
        reason = "duty roster change",
    )

    private fun delivery(tenantId: String, recipientUserId: String?) = AuthSecurityEventNotificationDelivery(
        id = UUID.randomUUID().toString(),
        tenantId = tenantId,
        eventId = UUID.randomUUID().toString(),
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        escalationLevel = 1,
        recipientUserId = recipientUserId,
        dueAt = NOW.minusHours(1),
        escalatedAt = NOW,
        attemptCount = 1,
        leaseUntil = NOW.plusMinutes(1),
    )

    private companion object {
        val NOW: LocalDateTime = LocalDateTime.parse("2026-08-25T10:00:00")
    }
}
