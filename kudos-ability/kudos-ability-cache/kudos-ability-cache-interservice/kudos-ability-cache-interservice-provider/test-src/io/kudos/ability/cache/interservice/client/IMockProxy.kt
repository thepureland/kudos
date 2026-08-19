package io.kudos.ability.cache.interservice.client

import io.kudos.ability.cache.interservice.common.RequestResult
import org.springframework.web.service.annotation.GetExchange

/**
 * Mock interface-client proxy; bypasses the service registry and connects directly (the group's
 * base-url is a plain `http://localhost:13578`, set in the `client` test profile).
 *
 * @author K
 * @since  1.0.0
 */
interface IMockProxy {

    @GetExchange("/same")
    fun same(): RequestResult

    @GetExchange("/different1")
    fun different1(): RequestResult

    @GetExchange("/different2")
    fun different2(): RequestResult

}
