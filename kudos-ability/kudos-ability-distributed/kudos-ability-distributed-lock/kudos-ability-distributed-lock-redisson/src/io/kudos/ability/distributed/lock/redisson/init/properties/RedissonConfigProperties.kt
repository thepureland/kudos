package io.kudos.ability.distributed.lock.redisson.init.properties

/**
 * Redisson config properties.
 * Encapsulates the core Redisson client configuration including thread pools, codec and transport mode.
 */
class RedissonConfigProperties {
    /**
     * Thread pool size. Default: current core count * 2.
     */
    var threads: Int = 0

    /**
     * Netty thread pool size. Default: current core count * 2.
     */
    var nettyThreads: Int = 0

    /**
     * Codec.
     */
    var codec: String = "!<org.redisson.codec.JsonJacksonCodec> {}"

    /**
     * Transport mode.
     */
    var transportMode: String = "NIO"
}
