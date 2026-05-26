package io.kudos.ability.distributed.discovery.nacos.init.properties

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
    }

    companion object {
        const val FILTER_ORDER: Int = FilterRegistrationBean.HIGHEST_PRECEDENCE + 1
    }
}
