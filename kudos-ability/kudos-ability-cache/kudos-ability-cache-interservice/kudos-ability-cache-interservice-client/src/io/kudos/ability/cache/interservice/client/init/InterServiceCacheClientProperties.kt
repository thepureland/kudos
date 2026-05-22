package io.kudos.ability.cache.interservice.client.init

/**
 * 跨服务缓存 client 端配置。
 *
 * @property ttlSeconds Feign 本地缓存 TTL，单位秒，默认 10 分钟
 * @property decoderEnabled 是否注册本模块提供的全局 Feign Decoder 装饰链
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheClientProperties {
    var ttlSeconds: Int = 600
    var decoderEnabled: Boolean = true
}
