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
