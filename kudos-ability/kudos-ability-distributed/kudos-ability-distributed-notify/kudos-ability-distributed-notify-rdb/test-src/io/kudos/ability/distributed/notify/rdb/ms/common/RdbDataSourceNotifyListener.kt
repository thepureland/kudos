package io.kudos.ability.distributed.notify.rdb.ms.common

import io.kudos.ability.distributed.notify.rdb.common.NotifyTypeEnum
import org.soul.ability.distributed.notify.common.api.INotifyListener
import org.soul.ability.distributed.notify.common.model.NotifyMessageVo
import org.soul.base.log.Log
import org.soul.base.log.LogFactory

class RdbDataSourceNotifyListener : INotifyListener<Any?> {
    var key: String? = null
        private set

    private var port: Int? = null

    private val log: Log = LogFactory.getLog(RdbDataSourceNotifyListener::class.java)

    override fun notifyType(): String {
        return NotifyTypeEnum.DS.getCode()
    }

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<*>) {
        key = notifyMessageVo.getMessageBody() as String?
        log.info("notifyProcess port:{0}, key:{1}", port, key)
    }

    fun setPort(port: Int?) {
        this.port = port
    }
}
