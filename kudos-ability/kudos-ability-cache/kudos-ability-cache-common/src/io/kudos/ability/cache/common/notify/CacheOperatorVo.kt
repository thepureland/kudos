package io.kudos.ability.cache.common.notify

import org.soul.ability.cache.common.notify.CacheNotifyListener
import org.soul.ability.cache.common.tools.CacheTool
import org.soul.ability.distributed.notify.common.model.NotifyMessageVo
import org.soul.ability.distributed.notify.common.support.NotifyTool
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.context.tool.SpringTool
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.io.Serial
import java.io.Serializable

class CacheOperatorVo : Serializable {
    var type: String? = null //操作类型
    var cacheName: String? = null //缓存名
    var key: Any? = null //缓存key

    constructor()

    constructor(type: String?, cacheName: String?, key: Any?) {
        this.type = type
        this.cacheName = cacheName
        this.key = key
    }

    fun doNotify() {
        val messageVo: NotifyMessageVo<*> = NotifyMessageVo<CacheOperatorVo?>(CacheNotifyListener.CACHE_OPERATOR, this)
        var notify = false
        try {
            val notifyTool = SpringTool.getBean<NotifyTool>(NotifyTool::class.java)
            notify = notifyTool.notify(messageVo)
        } catch (e: NoSuchBeanDefinitionException) {
            LOG.warn(e.message)
        }
        if (!notify) {
            if (TYPE_CLEAR == this.type) {
                CacheTool.doClear(this.cacheName)
            }
            if (TYPE_EVICT == this.type) {
                CacheTool.doEvict(this.cacheName, this.key)
            }
        }
    }

    companion object {
        private val LOG: Log = LogFactory.getLog(CacheOperatorVo::class.java)

        @Serial
        private val serialVersionUID = -1233873328202104930L

        const val TYPE_CLEAR: String = "clear"
        const val TYPE_EVICT: String = "evict"
    }
}
