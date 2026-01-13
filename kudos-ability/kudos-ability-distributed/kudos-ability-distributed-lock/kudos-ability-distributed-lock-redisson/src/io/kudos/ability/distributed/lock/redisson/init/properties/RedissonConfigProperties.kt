package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Redisson配置属性类
 * 封装Redisson客户端的核心配置，包括线程池、编码器、传输模式等
 */
class RedissonConfigProperties {
    /**
     * 线程池数量,默认值: 当前处理核数量 * 2
     */
    var threads: Int = 0

    /**
     * Netty线程池数量,默认值: 当前处理核数量 * 2
     */
    var nettyThreads: Int = 0

    /**
     * 编码
     */
    var codec: String = "!<org.redisson.codec.JsonJacksonCodec> {}"

    /**
     * 传输模式
     */
    var transportMode: String = "NIO"
}
