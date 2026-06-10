package io.kudos.ability.distributed.discovery.nacos.init.properties

import io.kudos.ability.distributed.discovery.nacos.filter.FeignContextSignatureVerifier
import org.springframework.boot.web.servlet.FilterRegistrationBean

/**
 * Kudos Nacos discovery extension properties.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class NacosDiscoveryProperties {
    var feignContextFilter: FeignContextFilter = FeignContextFilter()

    /**
     * Feign context filter configuration.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    class FeignContextFilter {
        /**
         * Whether to allow requests without the FEIGN_REQUEST / NOTIFY_REQUEST marker to also
         * populate KudosContext from headers. Defaults to false to prevent external requests from
         * forging context; may be enabled explicitly for development/debugging.
         */
        var allowUnmarkedContextHeaders: Boolean = false

        /**
         * HMAC secret used to verify the context-propagation signature headers
         * (X-Kudos-Context-Timestamp / Nonce / Signature). Must equal the client-side
         * `kudos.ability.distributed.client.feign.contextSignatureSecret`. Empty (default)
         * disables verification and preserves the legacy trust-the-marker behavior — a one-time
         * WARN is then logged on the first context-propagation request.
         */
        var contextSignatureSecret: String? = null

        /**
         * Acceptance window (in milliseconds, applied as plus or minus) for the signed timestamp.
         * Also used as the TTL of the nonce replay cache. Default: 5 minutes.
         */
        var contextSignatureTimestampWindowMillis: Long =
            FeignContextSignatureVerifier.DEFAULT_TIMESTAMP_WINDOW_MILLIS

        /**
         * Upper bound of the in-process seen-nonce cache, protecting against memory growth under
         * high request rates. Oldest entries are evicted first when the cap is reached.
         */
        var contextSignatureNonceCacheMaxSize: Int =
            FeignContextSignatureVerifier.DEFAULT_NONCE_CACHE_MAX_SIZE
    }

    companion object {
        const val FILTER_ORDER: Int = FilterRegistrationBean.HIGHEST_PRECEDENCE + 1
    }
}
