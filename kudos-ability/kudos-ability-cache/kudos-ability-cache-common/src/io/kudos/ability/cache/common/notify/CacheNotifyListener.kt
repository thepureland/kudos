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

    /**
     * 处理缓存操作通知消息
     * 
     * 接收缓存操作通知，根据缓存策略决定是否处理，并执行相应的缓存清理操作。
     * 
     * 工作流程：
     * 1. 提取消息体：从NotifyMessageVo中提取CacheOperatorVo操作信息
     * 2. 缓存激活检查：检查缓存是否激活，未激活则直接返回
     * 3. 记录日志：记录收到的清理缓存通知信息
     * 4. 策略判断：获取缓存配置，判断缓存策略
     * 5. 执行清理：如果是SINGLE_LOCAL策略，调用doClear执行清理操作
     * 
     * 缓存策略说明：
     * - SINGLE_LOCAL：单节点本地缓存策略，需要通过通知机制实现多节点同步
     *   - 当某个节点清理缓存时，通过MQ发送通知消息
     *   - 其他节点收到消息后，清理本地缓存
     *   - 只需一个应用实例处理即可（避免重复清理）
     * - 其他策略（如分布式缓存）：由Redis发布订阅或其他机制处理，不在此处处理
     * 
     * 注意事项：
     * - 只有SINGLE_LOCAL策略的缓存才会通过此监听器处理
     * - 如果缓存未激活，不会执行任何操作
     * - 通过MQ发送的通知消息，只需一个应用实例处理即可
     * 
     * @param notifyMessageVo 通知消息对象，包含缓存操作信息
     */
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

    /**
     * 执行缓存清理操作
     * 
     * 根据操作类型执行不同的缓存清理操作。
     * 
     * 操作类型：
     * - TYPE_CLEAR：清除整个缓存，调用CacheKit.doClear清除所有缓存数据
     * - TYPE_EVICT：清除指定key的缓存，调用CacheKit.doEvict清除特定key的数据
     * 
     * 注意事项：
     * - 两种操作类型可以同时存在，都会被执行
     * - TYPE_EVICT需要确保key不为null，否则会抛出异常
     * - 清理操作会触发缓存失效通知，通知其他节点同步清理
     * 
     * @param messageBody 缓存操作信息对象，包含操作类型、缓存名称、key等信息
     */
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
