package io.kudos.ability.distributed.notify.mq.ms.common

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.common.NotifyTypeEnum
import io.kudos.base.logger.LogFactory
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
open class DataSourceNotifyListener : INotifyListener {

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var mainClinet: IMainClinet

    @Autowired
    private lateinit var msConfig: MsConfig

    override fun notifyType() = NotifyTypeEnum.DS.code

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
        // 模擬收到通知，後修改數據源。
        log.info("@@@@ notifyProcess, port: ${msConfig.port}, appKey: ${msConfig.appKey}, key: ${notifyMessageVo.messageBody}")
        mainClinet.collection(msConfig.port, msConfig.appKey, notifyMessageVo.messageBody as String?)
    }

    @PostConstruct
    fun init() {
        log.info("[DataSourceNotifyListener]初始化完成...port:${msConfig.port}, appKey:${msConfig.appKey}")
    }
}
