package io.kudos.ability.distributed.notify.mq.ms.common

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
open class MsApplicationListener : ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private lateinit var mainClinet: IMainClinet

    @Autowired
    private lateinit var msConfig: MsConfig

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        mainClinet.registry(msConfig.appKey, msConfig.port)
    }

}
