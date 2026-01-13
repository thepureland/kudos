package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.io.Serial
import java.io.Serializable

/**
 * 缓存操作值对象
 * 封装缓存操作信息（清除或失效），支持通过通知机制进行分布式缓存同步
 */
class CacheOperatorVo(
    var type: String, //操作类型
    var cacheName: String, //缓存名
    var key: Any? //缓存key
) : Serializable {

    fun doNotify() {
        val messageVo: NotifyMessageVo<*> = NotifyMessageVo<CacheOperatorVo>(CacheNotifyListener.CACHE_OPERATOR, this)
        var notify = false
        try {
            val notifyTool = SpringKit.getBean(NotifyTool::class)
            notify = notifyTool.notify(messageVo)
        } catch (e: NoSuchBeanDefinitionException) {
            LOG.warn(e.message)
        }
        if (!notify) {
            if (TYPE_CLEAR == this.type) {
                CacheKit.doClear(this.cacheName)
            }
            if (TYPE_EVICT == this.type) {
                CacheKit.doEvict(this.cacheName, this.key!!)
            }
        }
    }

    companion object {
        private val LOG = LogFactory.getLog(this)

        @Serial
        private val serialVersionUID = -1233873328202104930L

        const val TYPE_CLEAR: String = "clear"
        const val TYPE_EVICT: String = "evict"
    }
}
