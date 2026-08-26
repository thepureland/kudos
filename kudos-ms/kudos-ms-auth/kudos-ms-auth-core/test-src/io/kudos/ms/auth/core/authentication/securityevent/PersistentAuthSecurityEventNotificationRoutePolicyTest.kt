package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteAppliesToEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteConfig
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRouteFallbackEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.PersistentAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.spi.IAuthSecurityEventResponderResolver
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class PersistentAuthSecurityEventNotificationRoutePolicyTest {
    private val configService = mock(IAuthSecurityEventNotificationRouteConfigService::class.java)
    private val policy = PersistentAuthSecurityEventNotificationRoutePolicy(configService)
    private val responderResolver = mock(IAuthSecurityEventResponderResolver::class.java)
    private val rosterAwarePolicy = PersistentAuthSecurityEventNotificationRoutePolicy(
        configService,
        responderResolver,
        Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
    )

    @Test
    fun tenantsWithoutAnEnabledRuleKeepTheBuiltInBehaviour() {
        stub(AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED, null)
        stub(AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED, null)

        val assigned = policy.resolve(delivery("admin-2"))
        val unassigned = policy.resolve(delivery(null))

        assertEquals("ASSIGNEE", assigned.routeCode)
        assertEquals(AuthSecurityEventNotificationDestinationEnum.USER, assigned.destination)
        assertEquals(setOf("admin-2"), assigned.recipientUserIds)
        assertEquals("TENANT_SECURITY_QUEUE", unassigned.routeCode)
        assertEquals(setOf(AuthSecurityEventNotificationChannelEnum.EVENT_BUS), unassigned.channels)
    }

    @Test
    fun aStoredAssignedRuleAddsTheConfiguredRespondersToTheAssignee() {
        stub(
            AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED,
            config(
                routeCode = "ASSIGNEE_AND_DUTY",
                channels = setOf(
                    AuthSecurityEventNotificationChannelEnum.EMAIL,
                    AuthSecurityEventNotificationChannelEnum.SMS,
                ),
                responderUserIds = setOf("duty-1", "duty-2"),
                includeAssignee = true,
            ),
        )

        val route = policy.resolve(delivery("admin-2"))

        assertEquals("ASSIGNEE_AND_DUTY", route.routeCode)
        assertEquals(setOf("admin-2", "duty-1", "duty-2"), route.recipientUserIds)
        assertEquals(
            setOf(
                AuthSecurityEventNotificationChannelEnum.EMAIL,
                AuthSecurityEventNotificationChannelEnum.SMS,
            ),
            route.channels,
        )
    }

    @Test
    fun anUnassignedRuleAddressesTheConfiguredRespondersWithoutInventingAnAssignee() {
        stub(
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED,
            config(
                routeCode = "DUTY_QUEUE",
                channels = setOf(AuthSecurityEventNotificationChannelEnum.WORK_ORDER),
                responderUserIds = setOf("duty-1"),
                includeAssignee = false,
                destination = AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE,
            ),
        )

        val route = policy.resolve(delivery(null))

        assertEquals("DUTY_QUEUE", route.routeCode)
        assertEquals(AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE, route.destination)
        assertEquals(setOf("duty-1"), route.recipientUserIds)
    }

    @Test
    fun aRuleNamingARotationPagesWhoeverIsOnCallAtTheMomentOfTheAttempt() {
        stub(
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED,
            config(
                routeCode = "DUTY",
                channels = setOf(AuthSecurityEventNotificationChannelEnum.SMS),
                responderUserIds = setOf("fixed-1"),
                includeAssignee = false,
                responderRosterCode = "SECURITY",
            ),
        )
        `when`(responderResolver.resolve("tenant-1", "SECURITY", 1, NOW)).thenReturn(setOf("oncall-1"))

        val route = rosterAwarePolicy.resolve(delivery(null))

        assertEquals(setOf("fixed-1", "oncall-1"), route.recipientUserIds)
    }

    @Test
    fun aRotationWithNobodyOnCallFallsBackInsteadOfSubstitutingSomebody() {
        stub(
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED,
            config(
                routeCode = "DUTY",
                channels = setOf(AuthSecurityEventNotificationChannelEnum.SMS),
                responderUserIds = emptySet(),
                includeAssignee = false,
                responderRosterCode = "SECURITY",
            ),
        )
        `when`(responderResolver.resolve("tenant-1", "SECURITY", 1, NOW)).thenReturn(emptySet())

        val route = rosterAwarePolicy.resolve(delivery(null))

        assertEquals("TENANT_SECURITY_QUEUE", route.routeCode)
        assertEquals(emptySet(), route.recipientUserIds)
    }

    @Test
    fun aRotationThatCannotBeReadIsARetryRatherThanARoutingDecision() {
        stub(
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED,
            config(
                routeCode = "DUTY",
                channels = setOf(AuthSecurityEventNotificationChannelEnum.SMS),
                responderUserIds = setOf("fixed-1"),
                includeAssignee = false,
                responderRosterCode = "SECURITY",
            ),
        )
        `when`(responderResolver.resolve("tenant-1", "SECURITY", 1, NOW))
            .thenThrow(IllegalStateException("duty directory unavailable"))

        // Not an AuthSecurityEventException: the dispatcher backs off instead of marking the row DEAD.
        assertFailsWith<IllegalStateException> { rosterAwarePolicy.resolve(delivery(null)) }
    }

    @Test
    fun eachConfiguredFallbackIsHonouredWhenTheRuleCanNoLongerAddressAnybody() {
        val unaddressable = config(
            routeCode = "ASSIGNEE_ONLY",
            channels = setOf(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE),
            responderUserIds = emptySet(),
            includeAssignee = true,
        )
        stub(
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED,
            unaddressable,
            unaddressable.copy(
                fallbackBehavior = AuthSecurityEventNotificationRouteFallbackEnum.TENANT_SECURITY_QUEUE,
            ),
            unaddressable.copy(fallbackBehavior = AuthSecurityEventNotificationRouteFallbackEnum.FAIL),
        )

        val builtIn = policy.resolve(delivery(null))
        val degraded = policy.resolve(delivery(null))
        val refused = assertFailsWith<AuthSecurityEventException> { policy.resolve(delivery(null)) }

        assertEquals("TENANT_SECURITY_QUEUE", builtIn.routeCode)
        assertEquals(setOf(AuthSecurityEventNotificationChannelEnum.EVENT_BUS), builtIn.channels)
        assertEquals(AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE, degraded.destination)
        assertEquals(emptySet(), degraded.recipientUserIds)
        // The dispatcher turns this into a DEAD row rather than substituting a recipient nobody configured.
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INVALID", refused.errorCode)
    }

    private fun stub(
        appliesTo: AuthSecurityEventNotificationRouteAppliesToEnum,
        first: AuthSecurityEventNotificationRouteConfig?,
        vararg rest: AuthSecurityEventNotificationRouteConfig,
    ) {
        `when`(
            configService.findEffective(
                "tenant-1",
                AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
                appliesTo,
            )
        ).thenReturn(first, *rest)
    }

    private fun config(
        routeCode: String,
        channels: Set<AuthSecurityEventNotificationChannelEnum>,
        responderUserIds: Set<String>,
        includeAssignee: Boolean,
        responderRosterCode: String? = null,
        destination: AuthSecurityEventNotificationDestinationEnum =
            AuthSecurityEventNotificationDestinationEnum.USER,
    ) = AuthSecurityEventNotificationRouteConfig(
        tenantId = "tenant-1",
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        appliesTo = AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED,
        routeCode = routeCode,
        destination = destination,
        channels = channels,
        responderUserIds = responderUserIds,
        responderRosterCode = responderRosterCode,
        includeAssignee = includeAssignee,
        enabled = true,
        fallbackBehavior = AuthSecurityEventNotificationRouteFallbackEnum.DEFAULT_ROUTE,
        configVersion = 1L,
        createUserId = "operator-1",
        createReason = "initial",
        createTime = NOW,
        updateUserId = "operator-1",
        updateReason = "initial",
        updateTime = NOW,
    )

    private fun delivery(recipientUserId: String?) = AuthSecurityEventNotificationDelivery(
        id = "notification-1",
        tenantId = "tenant-1",
        eventId = "event-1",
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
