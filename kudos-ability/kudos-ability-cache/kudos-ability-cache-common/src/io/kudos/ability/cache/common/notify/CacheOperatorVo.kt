package io.kudos.ability.cache.common.notify

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.ability.distributed.notify.common.support.NotifyTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import java.io.Serial
import java.io.Serializable
import java.util.UUID

/**
 * 缓存操作值对象
 * 
 * 用于封装缓存操作信息（清除或失效），支持通过通知机制实现分布式缓存的同步。
 * 
 * 核心属性：
 * - type：操作类型（TYPE_CLEAR清除整个缓存，TYPE_EVICT清除指定key）
 * - cacheName：缓存名称，标识要操作的缓存
 * - key：缓存key，TYPE_EVICT时使用，TYPE_CLEAR时可以为null
 * 
 * 操作类型：
 * - TYPE_CLEAR：清除整个缓存，所有key都会被清除
 * - TYPE_EVICT：清除指定key的缓存，只清除单个key
 * 
 * 通知机制：
 * - 通过doNotify方法发送通知消息到MQ
 * - 其他节点通过CacheNotifyListener监听并处理通知
 * - 实现分布式环境下的缓存同步失效
 * 
 * 降级处理：
 * - 如果通知发送失败，会执行本地清理作为降级方案
 * - 确保即使通知机制不可用，本地缓存也能被清理
 * - 避免缓存数据不一致的问题
 * 
 * 使用场景：
 * - 分布式缓存的一致性维护
 * - 缓存失效的主动通知
 * - 多节点环境下的缓存同步
 * 
 * 注意事项：
 * - 通知发送成功时，不会执行本地清理（由其他节点处理）
 * - 通知发送失败时，会执行本地清理作为降级方案
 * - TYPE_EVICT操作时，key不能为null
 * 
 * @since 1.0.0
 */
class CacheOperatorVo(
    var type: String, //操作类型
    var cacheName: String, //缓存名
    var key: Any? //缓存key
) : Serializable {

    /**
     * 消息唯一 id，用于跨节点日志追踪与（必要时）去重。
     * 默认在对象构造时生成；旧节点反序列化时为 null（Java 序列化对新增字段的兼容行为），消费端需对 null 做兼容判断。
     */
    var messageId: String? = UUID.randomUUID().toString()

    /**
     * 发送节点 id；在 [doNotify] 中填充。便于消费端识别 loopback / 排查跨节点问题。
     * 当前实现不基于 nodeId 做 loopback 过滤（SINGLE_LOCAL 流程依赖回环来清理 sender 的本地缓存），仅作日志维度。
     */
    var nodeId: String? = null

    /** 发送时间戳（毫秒）。便于消费端在日志里排查"消息延迟到达"或乱序问题。 */
    var timestamp: Long = System.currentTimeMillis()

    /**
     * 发送缓存操作通知
     * 
     * 通过通知机制发送缓存操作消息，实现分布式缓存的同步失效和清除。
     * 
     * 工作流程：
     * 1. 创建通知消息：将当前CacheOperatorVo封装为NotifyMessageVo
     * 2. 尝试发送通知：通过NotifyTool发送通知消息到MQ
     * 3. 发送失败处理：如果通知发送失败（NotifyTool不存在或发送失败），执行本地清理
     * 
     * 通知机制：
     * - 优先使用通知机制（MQ）进行分布式缓存同步
     * - 通知消息会被CacheNotifyListener监听并处理
     * - 支持多节点环境下的缓存同步失效
     * 
     * 降级处理：
     * - 如果NotifyTool Bean不存在，会记录警告日志并执行本地清理
     * - 如果通知发送失败，也会执行本地清理
     * - 确保即使通知机制不可用，本地缓存也能被清理
     * 
     * 清理操作：
     * - TYPE_CLEAR：清除整个缓存
     * - TYPE_EVICT：清除指定key的缓存
     * 
     * 注意事项：
     * - 通知发送成功时，不会执行本地清理（由其他节点处理）
     * - 通知发送失败时，会执行本地清理作为降级方案
     * - 需要确保NotifyTool Bean存在，否则会降级为本地清理
     */
    fun doNotify() {
        // 在发送时点把节点 id 填进消息，便于消费端排查；找不到 cacheNodeId bean 时降级为 null，不影响主流程。
        if (nodeId == null) {
            nodeId = runCatching { SpringKit.getBeanOrNull("cacheNodeId") as? String }.getOrNull()
        }
        val messageVo: NotifyMessageVo<*> = NotifyMessageVo<CacheOperatorVo>(CacheNotifyListener.CACHE_OPERATOR, this)
        var notify = false
        try {
            val notifyTool = SpringKit.getBean<NotifyTool>()
            notify = notifyTool.notify(messageVo)
        } catch (e: NoSuchBeanDefinitionException) {
            LOG.warn(e.message)
        }
        if (!notify) {
            if (TYPE_CLEAR == this.type) {
                KeyValueCacheKit.doClear(this.cacheName)
            }
            if (TYPE_EVICT == this.type) {
                KeyValueCacheKit.doEvict(this.cacheName, this.key!!)
            }
        }
    }

    companion object {
        private val LOG = LogFactory.getLog(this::class)

        @Serial
        private val serialVersionUID = -1233873328202104930L

        const val TYPE_CLEAR: String = "clear"
        const val TYPE_EVICT: String = "evict"
    }
}
