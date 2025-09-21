package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import java.io.Serializable

class CacheNotifyListener : INotifyListener {

    private val log = LogFactory.getLog(this)

    override fun notifyType(): String {
        return CACHE_OPERATOR
    }

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
        val messageBody = notifyMessageVo.messageBody as CacheOperatorVo
        if (!CacheKit.isCacheActive(messageBody.cacheName)) {
            return
        }
        log.info(
            "收到清理缓存Notify：cacheName={0},type={1},key={2}",
            messageBody.cacheName,
            messageBody.type,
            messageBody.key
        )
        //通过mq过来的清理缓存，只需一个应用处理就好
        val strategy = CacheKit.getCacheConfig(messageBody.cacheName)!!.strategy
        if (CacheStrategy.SINGLE_LOCAL.name == strategy) {
            doClear(messageBody)
        }
    }

    private fun doClear(messageBody: CacheOperatorVo) {
        if (CacheOperatorVo.TYPE_CLEAR == messageBody.type) {
            CacheKit.doClear(messageBody.cacheName)
        }
        if (CacheOperatorVo.TYPE_EVICT == messageBody.type) {
            CacheKit.doEvict(messageBody.cacheName, messageBody.key!!)
        }
    }

    companion object {
        const val CACHE_OPERATOR: String = "_CACHE_OPERATOR"
    }
}
