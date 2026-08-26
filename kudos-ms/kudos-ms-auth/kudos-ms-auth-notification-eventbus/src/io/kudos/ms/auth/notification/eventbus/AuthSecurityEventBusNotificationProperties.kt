package io.kudos.ms.auth.notification.eventbus

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.security-event.notification.event-bus")
open class AuthSecurityEventBusNotificationProperties {
    var enabled: Boolean = false

    /** The Spring Cloud Stream producer binding this adapter sends on; the broker is the deployment's choice. */
    var bindingName: String = "authSecurityEventNotification-out-0"
}
