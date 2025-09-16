package io.kudos.ability.cache.common.notify

import io.kudos.ability.distributed.notify.common.api.INotifyListener

class CacheNotifyListener : INotifyListener<CacheOperatorVo?> {
    private val log: Log = LogFactory.getLog(CacheNotifyListener::class.java)
    override fun notifyType(): String {
        return CACHE_OPERATOR
    }

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<CacheOperatorVo>) {
        val messageBody = notifyMessageVo.getMessageBody()
        if (!CacheTool.isCacheActive(messageBody.getCacheName())) {
            return
        }
        log.info(
            "收到清理缓存Notify：cacheName={0},type={1},key={2}",
            messageBody.getCacheName(),
            messageBody.getType(),
            messageBody.getKey()
        )
        //通过mq过来的清理缓存，只需一个应用处理就好
        val strategy = CacheTool.getCacheConfig(messageBody.getCacheName()).getStrategy()
        if (CacheStrategy.SINGLE_LOCAL.name == strategy) {
            doClear(messageBody)
        }
    }

    private fun doClear(messageBody: CacheOperatorVo) {
        if (CacheOperatorVo.TYPE_CLEAR == messageBody.getType()) {
            CacheTool.doClear(messageBody.getCacheName())
        }
        if (CacheOperatorVo.TYPE_EVICT == messageBody.getType()) {
            CacheTool.doEvict(messageBody.getCacheName(), messageBody.getKey())
        }
    }

    companion object {
        const val CACHE_OPERATOR: String = "_CACHE_OPERATOR"
    }
}
