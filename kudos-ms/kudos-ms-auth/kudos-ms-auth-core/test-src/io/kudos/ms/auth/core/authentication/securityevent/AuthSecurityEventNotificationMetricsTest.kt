package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.metrics.AuthSecurityEventNotificationMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecurityEventNotificationMetricsTest {

    @Test
    fun countersArePrecreatedAndUseOnlyFixedTypeAndOutcomeTags() {
        val registry = SimpleMeterRegistry()
        val metrics = AuthSecurityEventNotificationMetrics(registry)
        metrics.onDelivery(
            AuthSecurityEventNotificationDeliveryEvent(
                AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
                AuthSecurityEventNotificationDeliveryOutcomeEnum.DEAD,
            )
        )

        assertEquals(
            AuthSecurityEventNotificationTypeEnum.entries.size *
                AuthSecurityEventNotificationDeliveryOutcomeEnum.entries.size,
            registry.find(AuthSecurityEventNotificationMetrics.METRIC_NAME).counters().size,
        )
        val counter = registry.get(AuthSecurityEventNotificationMetrics.METRIC_NAME)
            .tags("type", "SLA_ESCALATED", "outcome", "DEAD")
            .counter()
        assertEquals(1.0, counter.count())
        assertEquals(setOf("outcome", "type"), counter.id.tags.map { it.key }.toSet())
    }
}
