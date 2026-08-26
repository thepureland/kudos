package io.kudos.ms.auth.notification.msg

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.security-event.notification.msg")
open class AuthSecurityEventMsgNotificationProperties {
    var enabled: Boolean = false
    var eventTypeDictCode: String = "auth_security_event_sla_escalated"
    var msgTypeDictCode: String = "security_alert"
    var localeDictCode: String? = null
}
