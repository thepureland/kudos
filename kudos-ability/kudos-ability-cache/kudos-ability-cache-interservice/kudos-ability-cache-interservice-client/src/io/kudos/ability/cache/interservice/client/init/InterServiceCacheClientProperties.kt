package io.kudos.ability.cache.interservice.client.init

/**
 * Cross-service cache client-side configuration.
 *
 * @property ttlSeconds Feign local cache TTL in seconds, default 10 minutes.
 * @property decoderEnabled Whether to register the global Feign Decoder decoration chain provided by this module.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheClientProperties {
    var ttlSeconds: Int = 600
    var decoderEnabled: Boolean = true
}
