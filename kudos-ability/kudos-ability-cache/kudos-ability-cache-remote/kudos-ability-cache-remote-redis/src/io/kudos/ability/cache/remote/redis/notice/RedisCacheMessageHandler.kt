package io.kudos.ability.cache.remote.redis.notice

import io.kudos.ability.cache.common.core.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notice.CacheMessage
import io.kudos.ability.cache.common.notice.ICacheMessageHandler
import io.kudos.ability.cache.common.support.CacheCleanRegister
import io.kudos.ability.data.memdb.redis.KudosRedisTemplate
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import java.util.*

/**
 * Redis缓存消息处理器
 * 
 * 监听Redis发布订阅消息，实现分布式缓存同步和失效通知。
 * 
 * 核心功能：
 * 1. 消息发送：将缓存操作消息发送到Redis发布订阅频道，通知其他节点
 * 2. 消息接收：监听Redis频道，接收其他节点发送的缓存操作消息
 * 3. 节点隔离：通过节点ID机制，避免当前节点处理自己发送的消息（避免重复清理）
 * 4. 本地缓存清理：接收到其他节点的清理消息后，清除本地缓存
 * 5. 监听器通知：触发注册的缓存清理监听器，支持自定义清理逻辑
 * 
 * 工作流程：
 * - 发送消息：将CacheMessage对象序列化后发送到Redis频道，并设置发送节点ID
 * - 接收消息：从Redis频道接收消息，反序列化为CacheMessage对象
 * - 节点判断：比较消息中的节点ID和当前节点ID，只有不同节点的消息才处理
 * - 本地清理：调用MixCacheManager清除本地缓存
 * - 监听器触发：通知注册的缓存清理监听器执行清理逻辑
 * 
 * 节点ID机制：
 * - 每个应用实例启动时生成唯一的节点ID（UUID）
 * - 发送消息时携带节点ID，接收消息时比较节点ID
 * - 只有其他节点的消息才会触发本地缓存清理，避免重复操作
 * 
 * 注意事项：
 * - 消息序列化失败时会记录警告日志，但不会中断处理流程
 * - 支持缓存版本隔离，消息中的缓存名称会经过版本转换
 * - 需要确保Redis连接正常，否则消息无法正常发送和接收
 */
class RedisCacheMessageHandler : ICacheMessageHandler, MessageListener {
    @Value("\${kudos.ability.cache.remoteStore}")
    private lateinit var remoteStore: String

    @Autowired
    private lateinit var kudosRedisTemplate: KudosRedisTemplate

    @Autowired(required = false)
    @Qualifier("mixCacheManager")
    private lateinit var mixCacheManager: MixCacheManager

    @Autowired
    protected lateinit var versionConfig: CacheVersionConfig

     override fun sendMessage(message: CacheMessage) {
        //设置消息发送的节点id
        message.nodeId = nodeId
        kudosRedisTemplate.getRedisTemplate(remoteStore)!!.convertAndSend(versionConfig.realMsgChannel, message)
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        logger.debug("收到redis消息通知：清除本地缓存")
        var cacheMessage: CacheMessage? = null
        try {
            cacheMessage = kudosRedisTemplate.getRedisTemplate(remoteStore)!!.getValueSerializer()
                .deserialize(message.getBody()) as CacheMessage?
        } catch (e: Exception) {
            logger.warn(
                "清理缓存序列化失败！缓存key没指定，用了方法参数的类型而找不到。请调整为key，如：key='ALL'！ERROR = {0}",
                e.message
            )
        }
        if (cacheMessage != null) {
            receiveMessage(cacheMessage)
        }
    }

    public override fun receiveMessage(message: CacheMessage) {
        //只有非当前节点的清理才需要删除本地缓存，本节点自己已经删除过了
        if (message.nodeId != nodeId) {
            mixCacheManager.clearLocal(message.cacheName!!, message.key)
        }
        val realCacheKey = versionConfig.getRealCacheName(message.cacheName!!)
        val cleanListeners = CacheCleanRegister.getCleanListener(realCacheKey)
        if (!cleanListeners.isNullOrEmpty()) {
            for (cleanListener in cleanListeners) {
                cleanListener.cleanCache(realCacheKey, message.key)
            }
        }
    }

    val redisTemplate: RedisTemplate<*, *>
        get() = kudosRedisTemplate.getRedisTemplate(remoteStore) as RedisTemplate<*, *>

    companion object {
        private val logger = LogFactory.getLog(this)
        private val nodeId: String?

        init {
            nodeId = UUID.randomUUID().toString()
        }
    }
}
