package io.kudos.ms.auth.notification.msg.init

import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import io.kudos.ms.auth.notification.msg.AuthSecurityEventMsgNotificationPublisher
import io.kudos.ms.msg.common.send.api.IMsgSendApi
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecurityEventMsgNotificationAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withUserConfiguration(AuthSecurityEventMsgNotificationAutoConfiguration::class.java)

    @Test
    fun publisherRequiresExplicitEnablementAndMsgApiAvailability() {
        runner.withBean(IMsgSendApi::class.java, { StubMsgSendApi() }).run { context ->
            assertEquals(0, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
        }
        runner.withPropertyValues("kudos.ms.auth.security-event.notification.msg.enabled=true").run { context ->
            assertEquals(0, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
        }
        runner
            .withPropertyValues("kudos.ms.auth.security-event.notification.msg.enabled=true")
            .withBean(IMsgSendApi::class.java, { StubMsgSendApi() })
            .run { context ->
                assertEquals(1, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
            }
    }

    @Test
    fun theInAppAdapterStandsNextToAnotherChannelAdapterInsteadOfSuppressingIt() {
        runner
            .withPropertyValues("kudos.ms.auth.security-event.notification.msg.enabled=true")
            .withBean(IMsgSendApi::class.java, { StubMsgSendApi() })
            .withBean("busPublisher", IAuthSecurityEventNotificationPublisher::class.java, { BusPublisher() })
            .run { context ->
                // Per-channel selection is the whole point: an existing publisher must not take this one's place.
                assertEquals(2, context.getBeanNamesForType(IAuthSecurityEventNotificationPublisher::class.java).size)
                assertEquals(
                    1,
                    context.getBeanNamesForType(AuthSecurityEventMsgNotificationPublisher::class.java).size,
                )
            }
    }

    private class StubMsgSendApi : IMsgSendApi {
        override fun publish(request: MsgPublishRequest): String = "send-1"
    }

    /** Stands in for an adapter that carries the bus channel, such as kudos-ms-auth-notification-eventbus. */
    private class BusPublisher : IAuthSecurityEventNotificationPublisher {
        override fun supports(
            destination: AuthSecurityEventNotificationDestinationEnum,
            channel: AuthSecurityEventNotificationChannelEnum,
        ): Boolean = channel == AuthSecurityEventNotificationChannelEnum.EVENT_BUS

        override fun publish(publication: AuthSecurityEventNotificationPublication) = Unit
    }
}
