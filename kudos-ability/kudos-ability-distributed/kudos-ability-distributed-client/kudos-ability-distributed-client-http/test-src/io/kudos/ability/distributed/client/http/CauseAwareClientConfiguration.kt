package io.kudos.ability.distributed.client.http

import io.kudos.ability.distributed.client.http.client.CauseAwareFallback
import io.kudos.ability.distributed.client.http.client.ICauseAwareClient
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Registers a dead-address client whose fallback takes the triggering `Throwable`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(group = "deadservice", types = [ICauseAwareClient::class])
@HttpServiceFallback(
    value = CauseAwareFallback::class,
    service = [ICauseAwareClient::class],
    group = "deadservice"
)
open class CauseAwareClientConfiguration
