package io.kudos.ability.distributed.discovery.nacos.init.properties

import org.springframework.boot.web.servlet.FilterRegistrationBean

/**
 * kudos Nacos discovery 扩展配置。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class NacosDiscoveryProperties {
    var feignContextFilter: FeignContextFilter = FeignContextFilter()

    /**
     * Feign 上下文 filter 配置。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    class FeignContextFilter {
        /**
         * 是否允许未携带 FEIGN_REQUEST / NOTIFY_REQUEST 标记的请求也按 header 写入 KudosContext。
         * 默认 false，避免外部请求伪造上下文；开发调试可显式打开。
         */
        var allowUnmarkedContextHeaders: Boolean = false
    }

    companion object {
        const val FILTER_ORDER: Int = FilterRegistrationBean.HIGHEST_PRECEDENCE + 1
    }
}
