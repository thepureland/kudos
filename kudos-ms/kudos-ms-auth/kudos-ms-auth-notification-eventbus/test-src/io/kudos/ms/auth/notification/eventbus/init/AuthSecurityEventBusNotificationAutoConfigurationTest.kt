package io.kudos.ms.auth.notification.eventbus.init

import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import io.kudos.ms.auth.notification.eventbus.AuthSecurityEventBusNotificationPublisher
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.MessageChannel
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecurityEventBusNotificationAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthSecurityEventBusNotificationAutoConfiguration::class.java)

    @Test
    fun publisherRequiresExplicitEnablementAndAConfiguredStreamProducer() {
        withStreamProducer(runner).run { context ->
            assertEquals(0, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
        }
        runner.withPropertyValues(ENABLED).run { context ->
            assertEquals(0, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
        }
        withStreamProducer(runner.withPropertyValues(ENABLED)).run { context ->
            assertEquals(1, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
        }
    }

    @Test
    fun theBusAdapterStandsNextToAnotherChannelAdapterInsteadOfSuppressingIt() {
        withStreamProducer(runner.withPropertyValues(ENABLED))
            .withBean("inAppPublisher", IAuthSecurityEventNotificationPublisher::class.java, { InAppPublisher() })
            .run { context ->
                // Per-channel selection is the whole point: an existing publisher must not take this one's place.
                assertEquals(2, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
                assertEquals(
                    1,
                    context.getBeanNamesForType(AuthSecurityEventBusNotificationPublisher::class.java).size,
                )
            }
    }

    /**
     * The helper is a real Spring bean with `@Resource` collaborators, so the container populates it even when
     * the instance itself is a mock; its dependencies have to be present for the context to start.
     */
    private fun withStreamProducer(runner: ApplicationContextRunner): ApplicationContextRunner = runner
        .withBean(StreamBridge::class.java, { mock(StreamBridge::class.java) })
        .withBean(BindingServiceProperties::class.java, { mock(BindingServiceProperties::class.java) })
        .withBean("streamAsyncSendExecutor", ThreadPoolTaskExecutor::class.java, { mock(ThreadPoolTaskExecutor::class.java) })
        .withBean("mqProducerChannel", MessageChannel::class.java, { mock(MessageChannel::class.java) })
        .withBean(StreamProducerHelper::class.java, { StreamProducerHelper() })

    /** Stands in for an adapter that carries the in-app channels, such as kudos-ms-auth-notification-msg. */
    private class InAppPublisher : IAuthSecurityEventNotificationPublisher {
        override fun supports(
            destination: AuthSecurityEventNotificationDestinationEnum,
            channel: AuthSecurityEventNotificationChannelEnum,
        ): Boolean = channel == AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE

        override fun publish(publication: AuthSecurityEventNotificationPublication) = Unit
    }

    private companion object {
        const val ENABLED = "kudos.ms.auth.security-event.notification.event-bus.enabled=true"
    }
}
