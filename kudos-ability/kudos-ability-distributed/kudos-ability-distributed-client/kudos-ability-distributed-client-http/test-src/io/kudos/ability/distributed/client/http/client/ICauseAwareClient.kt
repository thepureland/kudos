package io.kudos.ability.distributed.client.http.client

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

/**
 * Client whose fallback wants to know *why* it was invoked.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@HttpExchange("/causeAware")
interface ICauseAwareClient {

    @GetExchange("/read")
    fun read(@RequestParam key: String): String?

}
