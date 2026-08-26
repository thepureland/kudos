package io.kudos.ms.auth.notification.eventbus.init

import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import io.kudos.ms.auth.notification.eventbus.AuthSecurityEventBusNotificationProperties
import io.kudos.ms.auth.notification.eventbus.AuthSecurityEventBusNotificationPublisher
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityEventBusNotificationProperties::class)
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.security-event.notification.event-bus",
    name = ["enabled"],
    havingValue = "true",
)
open class AuthSecurityEventBusNotificationAutoConfiguration {

    /**
     * Guarded on its own type rather than on the SPI: publishers are per channel now, so this adapter has to be
     * able to stand next to the in-app one instead of suppressing it.
     */
    @Bean
    @ConditionalOnBean(StreamProducerHelper::class)
    @ConditionalOnMissingBean(AuthSecurityEventBusNotificationPublisher::class)
    open fun authSecurityEventBusNotificationPublisher(
        producerHelper: StreamProducerHelper,
        properties: AuthSecurityEventBusNotificationProperties,
    ): IAuthSecurityEventNotificationPublisher =
        AuthSecurityEventBusNotificationPublisher(producerHelper, properties)
}
