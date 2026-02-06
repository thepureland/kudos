package io.kudos.ability.distributed.notify.mq.ms.common

import io.kudos.base.lang.string.RandomStringKit
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MsConfig {

    @Value($$"${server.port}")
    val port: Int? = null

    val appKey: String = RandomStringKit.random(8, true, true)

}
