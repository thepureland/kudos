package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.kit.CacheKit
import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import java.io.Serializable

/**
 * 缓存通知监听器
 * 
 * 监听缓存操作通知消息，实现分布式缓存的同步失效和清除。
 * 
 * 核心功能：
 * 1. 消息监听：监听类型为CACHE_OPERATOR的通知消息
 * 2. 缓存策略判断：根据缓存策略决定是否需要处理消息
 *    - SINGLE_LOCAL：单节点本地缓存策略，需要处理通知消息
 *    - 其他策略：由其他机制处理，不在此处处理
 * 3. 缓存操作：根据操作类型执行不同的缓存操作
 *    - TYPE_CLEAR：清除整个缓存
 *    - TYPE_EVICT：清除指定key的缓存
 * 
 * 工作流程：
 * - 接收NotifyMessageVo消息，提取CacheOperatorVo操作信息
 * - 检查缓存是否激活，未激活则直接返回
 * - 获取缓存配置，判断缓存策略
 * - 如果是SINGLE_LOCAL策略，执行缓存清理操作
 * 
 * 缓存策略说明：
 * - SINGLE_LOCAL：单节点本地缓存，需要通过通知机制实现多节点同步
 * - 其他策略（如分布式缓存）：由Redis发布订阅或其他机制处理
 * 
 * 注意事项：
 * - 只有SINGLE_LOCAL策略的缓存才会通过此监听器处理
 * - 通过MQ发送的通知消息，只需一个应用实例处理即可（避免重复清理）
 * - 如果缓存未激活，不会执行任何操作
 */
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
