package io.kudos.ability.distributed.notify.mq.ms.common

import org.soul.base.lang.string.RandomStringTool
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MsConfig {

    @Value("\${server.port}")
    val port: Int? = null

    val appKey: String? = RandomStringTool.random(8, true, true)

}
