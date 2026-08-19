package io.kudos.ability.distributed.client.http

import io.kudos.ability.distributed.client.http.client.ISecondaryTestClient
import org.springframework.context.annotation.Configuration
import org.springframework.web.service.registry.ImportHttpServices

/**
 * Declares `testservice` a **second** time, from a different class, contributing one more interface.
 *
 * If group merging did not work, this group would either be rejected as a duplicate or would lose the
 * `base-url` bound to the other declaration, and [ISecondaryTestClient] calls would fail.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ImportHttpServices(group = "testservice", types = [ISecondaryTestClient::class])
open class SecondaryClientConfiguration
