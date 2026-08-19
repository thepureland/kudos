package io.kudos.ability.distributed.client.http.client

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

/**
 * Client pointed at a dead address, used to prove the fallback path.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@HttpExchange("/unreachable")
interface IUnreachableClient {

    @GetExchange("/read")
    fun read(): String?

}
