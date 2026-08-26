package io.kudos.ms.auth.notification.msg.init

import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import io.kudos.ms.auth.notification.msg.AuthSecurityEventMsgNotificationProperties
import io.kudos.ms.auth.notification.msg.AuthSecurityEventMsgNotificationPublisher
import io.kudos.ms.msg.common.send.api.IMsgSendApi
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(AuthSecurityEventMsgNotificationProperties::class)
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.security-event.notification.msg",
    name = ["enabled"],
    havingValue = "true",
)
open class AuthSecurityEventMsgNotificationAutoConfiguration {

    /**
     * Guarded on its own type rather than on the SPI.
     *
     * Guarding on the interface made sense while the dispatcher accepted exactly one publisher; now that they
     * are selected per channel, that condition would let whichever adapter happened to be configured first
     * suppress the others — which is precisely the multi-channel arrangement this is meant to support.
     */
    @Bean
    @ConditionalOnBean(IMsgSendApi::class)
    @ConditionalOnMissingBean(AuthSecurityEventMsgNotificationPublisher::class)
    open fun authSecurityEventMsgNotificationPublisher(
        msgSendApi: IMsgSendApi,
        properties: AuthSecurityEventMsgNotificationProperties,
    ): IAuthSecurityEventNotificationPublisher = AuthSecurityEventMsgNotificationPublisher(msgSendApi, properties)
}
