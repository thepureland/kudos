package io.kudos.ability.distributed.client.http

import io.kudos.ability.distributed.client.http.client.IUnreachableClient
import io.kudos.ability.distributed.client.http.client.UnreachableClientFallback
import org.springframework.cloud.client.circuitbreaker.httpservice.HttpServiceFallback
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Registers the `deadservice` group and its fallback.
 *
 * Kept in its own class rather than stacked on the test class because `@ImportHttpServices` is
 * per-group: one group, one declaration.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(group = "deadservice", types = [IUnreachableClient::class])
@HttpServiceFallback(
    value = UnreachableClientFallback::class,
    service = [IUnreachableClient::class],
    group = "deadservice"
)
open class UnreachableClientConfiguration
