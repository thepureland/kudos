package io.kudos.ability.distributed.notify.mq.ms.common

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

@Component
class MsApplicationListener : ApplicationListener<ApplicationReadyEvent?> {
    @Autowired
    private val mainClinet: IMainClinet? = null

    @Autowired
    private val msConfig: MsConfig? = null

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        mainClinet!!.registry(msConfig!!.getAppKey(), msConfig.getPort())
    }
}
