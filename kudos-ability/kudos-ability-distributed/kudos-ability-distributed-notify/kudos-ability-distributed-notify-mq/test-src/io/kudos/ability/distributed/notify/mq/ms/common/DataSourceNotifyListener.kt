package io.kudos.ability.distributed.notify.mq.ms.common

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.common.NotifyTypeEnum
import jakarta.annotation.PostConstruct
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DataSourceNotifyListener : INotifyListener<Any?> {
    private val log: Log = LogFactory.getLog(DataSourceNotifyListener::class.java)

    @Autowired
    private val mainClinet: IMainClinet? = null

    @Autowired
    private val msConfig: MsConfig? = null

    override fun notifyType(): String? {
        return NotifyTypeEnum.DS.getCode()
    }

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<*>) {
        // 模擬收到通知，後修改數據源。
        log.info(
            "@@@@ notifyProcess, port: {0}, appKey: {1}, key: {2}",
            msConfig!!.getPort(),
            msConfig.getAppKey(),
            notifyMessageVo.messageBody
        )
        mainClinet!!.collection(msConfig.getPort(), msConfig.getAppKey(), notifyMessageVo.messageBody as String?)
    }

    @PostConstruct
    fun init() {
        log.info(
            "[DataSourceNotifyListener]初始化完成...port:{0}, appKey:{1}",
            msConfig!!.getPort(),
            msConfig.getAppKey()
        )
    }
}
