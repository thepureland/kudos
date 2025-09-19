package io.kudos.ability.comm.sms.aws.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "soul.ability.comm.sms.aws.proxy")
class SmsAwsProxyProperties {
    var isEnable: Boolean = false
    var url: String? = null
    var username: String? = null
    var password: String? = null
}
