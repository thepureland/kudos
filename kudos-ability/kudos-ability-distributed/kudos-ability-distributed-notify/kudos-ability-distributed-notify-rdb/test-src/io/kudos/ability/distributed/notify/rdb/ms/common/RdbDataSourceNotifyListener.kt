package io.kudos.ability.distributed.notify.rdb.ms.common

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.rdb.common.NotifyTypeEnum
import io.kudos.base.logger.LogFactory
import org.springframework.stereotype.Component


@Component
open class RdbDataSourceNotifyListener : INotifyListener {

    var key: String? = null
        private set

    private var port: Int? = null

    private val log = LogFactory.getLog(this)

    override fun notifyType(): String {
        return NotifyTypeEnum.DS.code
    }

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<*>) {
        key = notifyMessageVo.messageBody as String?
        log.info("notifyProcess port:{0}, key:{1}", port, key)
    }

    fun setPort(port: Int?) {
        this.port = port
    }
}
