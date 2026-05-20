package io.kudos.ability.cache.remote.redis.notice

import io.kudos.ability.cache.common.core.keyvalue.MixCacheManager
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.ability.cache.common.support.CacheCleanRegister
import io.kudos.ability.cache.common.support.IHashCacheSync
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component

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
@Component
open class RedisCacheMessageHandler(
    private val nodeId: String
) : ICacheMessageHandler, MessageListener {
    @Value($$"${kudos.ability.cache.remoteStore}")
    private lateinit var remoteStore: String

    @Autowired
    private lateinit var redisTemplates: RedisTemplates

    @Autowired(required = false)
    @Qualifier("mixCacheManager")
    private lateinit var mixCacheManager: MixCacheManager

    @Autowired
    protected lateinit var versionConfig: CacheVersionConfig

    /**
     * 发送缓存操作消息
     * 
     * 将缓存操作消息发送到Redis发布订阅频道，通知其他节点进行缓存清理。
     * 
     * 工作流程：
     * 1. 设置消息的节点ID为当前节点ID
     * 2. 将消息序列化后发送到Redis频道（使用版本化的频道名称）
     * 
     * 节点ID作用：
     * - 接收方通过比较节点ID判断是否需要处理消息
     * - 避免当前节点处理自己发送的消息，防止重复清理
     * 
     * @param message 缓存操作消息对象
     */
     override fun sendMessage(message: CacheMessage) {
        //设置消息发送的节点id
        message.nodeId = nodeId
        redisTemplates.getRedisTemplate(remoteStore)!!.convertAndSend(versionConfig.realMsgChannel, message)
    }

    /**
     * 接收Redis发布订阅消息
     * 
     * 从Redis频道接收缓存操作消息，反序列化后调用receiveMessage处理。
     * 
     * 工作流程：
     * 1. 从消息体中读取字节数组
     * 2. 使用RedisTemplate的ValueSerializer反序列化为CacheMessage对象
     * 3. 如果反序列化成功，调用receiveMessage处理消息
     * 4. 如果反序列化失败，记录警告日志但不中断处理
     * 
     * 异常处理：
     * - 反序列化失败通常是因为缓存key使用了方法参数类型，而序列化时找不到对应类型
     * - 建议在缓存注解中明确指定key，例如key='ALL'，避免类型解析问题
     * - 反序列化失败不会抛出异常，只是记录警告日志
     * 
     * @param message Redis消息对象，包含消息体和频道信息
     * @param pattern 频道模式（可选）
     */
    override fun onMessage(message: Message, pattern: ByteArray?) {
        logger.debug("收到redis消息通知：清除本地缓存")
        val deserialized: CacheMessage? = try {
            redisTemplates.getRedisTemplate(remoteStore)!!.valueSerializer
                .deserialize(message.body) as CacheMessage?
        } catch (e: Exception) {
            // 反序列化失败是分布式缓存一致性事故：本节点不会收到这条失效通知 → 本地缓存可能长期持有脏数据。
            // 这里抬到 error 级别并带堆栈，便于运维捕获；同时不向上抛，避免破坏 listener 线程。
            // 常见原因：发送方与接收方的 CacheMessage 类签名不一致（serialVersionUID）、key 类型 classpath 缺失。
            logger.error(e, "Redis 缓存失效通知反序列化失败，本节点本地缓存可能不一致；请检查发送/接收方的 CacheMessage 与 key 类是否兼容")
            null
        }
        val cacheMessage = deserialized ?: return
        try {
            receiveMessage(cacheMessage)
        } catch (e: Exception) {
            // listener 由 Spring 单线程串行投递，处理异常若抛出会被容器吞掉但中断对该条的处理。显式 catch + log，避免后续消息被误以为"全部失败"。
            logger.error(e, "处理 Redis 缓存失效通知时异常：cacheName={0}, key={1}", cacheMessage.cacheName, cacheMessage.key)
        }
    }

    /**
     * 处理接收到的缓存操作消息
     * 
     * 根据消息内容执行本地缓存清理和触发清理监听器。
     * 
     * 工作流程：
     * 1. 节点判断：比较消息中的节点ID和当前节点ID
     *    - 如果不同：说明是其他节点发送的消息，需要清理本地缓存
     *    - 如果相同：说明是当前节点发送的消息，本地已经清理过，跳过
     * 2. 本地缓存清理：调用MixCacheManager清除本地缓存
     * 3. 缓存名称转换：将消息中的缓存名称转换为实际缓存名称（去除版本前缀）
     * 4. 监听器触发：获取注册的清理监听器，逐个触发清理逻辑
     * 
     * 节点隔离机制：
     * - 只有其他节点的消息才会触发本地缓存清理
     * - 当前节点发送的消息不会触发本地清理，避免重复操作
     * - 确保分布式环境下缓存清理的正确性
     * 
     * 清理监听器：
     * - 支持注册自定义的缓存清理监听器
     * - 监听器可以执行额外的清理逻辑，例如清理相关缓存、通知其他系统等
     * 
     * @param message 缓存操作消息对象，包含缓存名称、key、节点ID等信息
     */
    override fun receiveMessage(message: CacheMessage) {
        val cacheName = message.cacheName!!
        // 只有非当前节点的清理才需要删除本地缓存，本节点自己已经删除过了
        if (message.nodeId != nodeId) {
            if (message.cacheType == "hash") {
                SpringKit.getBeansOfType<IHashCacheSync>().values.firstOrNull()?.let { sync ->
                    when (val k = message.key) {
                        null -> sync.clearLocal(cacheName)
                        // 批量操作打包成一条消息携带 id 列表（saveBatch 等场景），避免 N+1 publish 风暴。
                        is Collection<*> -> k.filterNotNull().forEach { sync.evictLocal(cacheName, it) }
                        else -> sync.evictLocal(cacheName, k)
                    }
                }
            } else {
                mixCacheManager.clearLocal(cacheName, message.key)
            }
        }
        val realCacheKey = versionConfig.getRealCacheName(cacheName)
        CacheCleanRegister.getCleanListener(realCacheKey)
            ?.forEach { it.cleanCache(realCacheKey, message.key) }
    }

    val redisTemplate: RedisTemplate<*, *>
        get() = redisTemplates.getRedisTemplate(remoteStore) as RedisTemplate<*, *>

    companion object {
        private val logger = LogFactory.getLog(this::class)
    }
}
